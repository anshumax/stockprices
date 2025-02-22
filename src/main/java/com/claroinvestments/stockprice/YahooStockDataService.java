package com.claroinvestments.stockprice;

import io.netty.handler.codec.http.HttpResponseStatus;
import jakarta.annotation.PostConstruct;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.time.*;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class YahooStockDataService {
	
	final Log log = LogFactory.getLog(YahooStockDataService.class);
	
	@Autowired
	File downloadDir;
	
	final String YAHOO_FINANCE_URL_TEMPLATE = "https://query1.finance.yahoo.com/v7/finance/chart/{0}.{1}?period1={2}&period2={3}&interval=1d&indicators=quote&includeTimestamps=true";
	final ZoneId istZoneId = TimeZone.getTimeZone("IST").toZoneId();

	CloseableHttpClient httpClient;

	@PostConstruct
	public void init() throws IOException {
		refreshHttpClient();
	}

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

			HttpGet getRequest = new HttpGet(url);
			String responseBody = httpClient.execute(getRequest, response -> {
				log.info("URL: " + url + " -> " + response.getCode());
				return EntityUtils.toString(response.getEntity());
			});

			while(Objects.isNull(responseBody)){
				log.info("Sleeping 60 seconds because received: " + HttpResponseStatus.TOO_MANY_REQUESTS);
				Thread.sleep(60000);
				refreshHttpClient();
				responseBody = httpClient.execute(getRequest, response -> {
					log.info("URL: " + url + " -> " + response.getCode());
					return EntityUtils.toString(response.getEntity());
				});
			}

			File downloadFile = new File(downloadDir, fileName);
			Files.writeString(downloadFile.toPath(), responseBody, Charset.defaultCharset());
			return new StockFileDownloadResult(downloadFile.getAbsolutePath(), ticker, exchangeName);
		}catch(Exception e) {
			log.error("Unable to download for ticker " + ticker + "." + exchangeShort, e);
			return null;
		}
	}

	private void refreshHttpClient() throws IOException {
		httpClient = HttpClientBuilder.create().setDefaultCookieStore(new BasicCookieStore()).build();
		String hostSite = "https://finance.yahoo.com/";
		log.info("Navigating to " + hostSite + " to obtain cookies");
		httpClient.execute(new HttpGet(hostSite), classicHttpResponse -> {
			log.info("Received status: " + classicHttpResponse.getCode());
			return null;
		});
	}
}
