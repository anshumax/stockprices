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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

@Service
public class YahooStockDataService {
	
	final Log log = LogFactory.getLog(YahooStockDataService.class);
	
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
	
	public StockFileDownloadResult download(LocalDate fromDate, LocalDate toDate, String ticker, String exchangeShort, String exchangeName) {
		String fileName = ticker + "." + exchangeName;
		log.info("Downloading " + count.getAndIncrement() + " of " + total + " for " + exchangeName);
		try {
			LocalDateTime fromDateTime = LocalDateTime.of(fromDate, LocalTime.MIDNIGHT);
			LocalDateTime toDateTime = LocalDateTime.of(toDate.minusDays(1), LocalTime.of(18, 30));
			long fromTimeStamp = ZonedDateTime.of(fromDateTime, istZoneId).toEpochSecond();
			long toTimeStamp = ZonedDateTime.of(toDateTime, istZoneId).toEpochSecond();
			
			String url = MessageFormat.format(YAHOO_FINANCE_URL_TEMPLATE, ticker, exchangeShort, Long.toString(fromTimeStamp), Long.toString(toTimeStamp));
			
			HttpRequest httpRequest = HttpRequest.newBuilder().uri(new URI(url)).GET().build();
			HttpResponse<String> httpResponse = client.send(httpRequest, BodyHandlers.ofString(Charset.defaultCharset()));

			int statusCode = httpResponse.statusCode();
			log.info("URL: " + url + " -> " + statusCode);
			while(statusCode == HttpResponseStatus.TOO_MANY_REQUESTS.code()){
				log.info("Sleeping 60 seconds because received: " + HttpResponseStatus.TOO_MANY_REQUESTS.toString());
				Thread.sleep(60000);
				httpResponse = client.send(httpRequest, BodyHandlers.ofString(Charset.defaultCharset()));
				statusCode = httpResponse.statusCode();
				log.info("URL: " + url + " -> " + statusCode);
			}

			File downloadFile = new File(downloadDir, fileName);
			Files.writeString(downloadFile.toPath(), httpResponse.body(), Charset.defaultCharset());
			return new StockFileDownloadResult(downloadFile.getAbsolutePath(), ticker, exchangeName);
		}catch(Exception e) {
			log.error("Unable to download for ticker " + ticker + "." + exchangeShort, e);
			return null;
		}
	}
}
