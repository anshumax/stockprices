package com.claroinvestments.stockprice;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimeZone;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

@Service
public class StockPriceDataDownloaderService {
	
	final Log log = LogFactory.getLog(StockPriceDataDownloaderService.class);
	
	@Autowired
	File downloadDir;
	
	final String YAHOO_FINANCE_URL_TEMPLATE = "https://query1.finance.yahoo.com/v7/finance/chart/{0}.{1}?period1={2}&period2={3}&interval=1d&indicators=quote&includeTimestamps=true";
	final HttpClient client = HttpClient.newHttpClient();
	final ZoneId istZoneId = TimeZone.getTimeZone("IST").toZoneId();
	
	AtomicInteger count;
	Integer total;
	
	public void reset(Integer total) {
		this.total = total;
		count = new AtomicInteger(1);
	}
	
	@Async
	public Future<StockFileDownloadResult> download(LocalDate fromDate, LocalDate toDate, String ticker, String exchangeShort, String exchangeName) {
		log.info("Downloading " + count.getAndIncrement() + " of " + total + " for " + exchangeName);
		try {
			LocalDateTime fromDateTime = LocalDateTime.of(fromDate, LocalTime.MIDNIGHT);
			LocalDateTime toDateTime = LocalDateTime.of(toDate.minusDays(1), LocalTime.of(18, 30));
			Long fromTimeStamp = ZonedDateTime.of(fromDateTime, istZoneId).toEpochSecond();
			Long toTimeStamp = ZonedDateTime.of(toDateTime, istZoneId).toEpochSecond();
			
			String url = MessageFormat.format(YAHOO_FINANCE_URL_TEMPLATE, ticker, exchangeShort, fromTimeStamp.toString(), toTimeStamp.toString());
			
			HttpRequest httpRequest = HttpRequest.newBuilder().uri(new URI(url)).GET().build();
			HttpResponse<String> httpResponse = client.send(httpRequest, BodyHandlers.ofString(Charset.defaultCharset()));
			String fileName = ticker + "." + exchangeName;
			Integer statusCode = httpResponse.statusCode();
			log.info("URL: " + url + " -> " + statusCode);
			File downloadFile = new File(downloadDir, fileName);
			Files.write(downloadFile.toPath(), httpResponse.body().getBytes(Charset.defaultCharset()));
			return new AsyncResult<StockFileDownloadResult>(new StockFileDownloadResult(downloadFile.getAbsolutePath(), ticker, exchangeName));
		}catch(Exception e) {
			log.error("Unable to download for ticker " + ticker + "." + exchangeShort, e);
			return null;
		}
	}
}
