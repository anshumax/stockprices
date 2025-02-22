package com.claroinvestments.stockprice;

import com.claroinvestments.stockprice.db.HistoricalStockPrice;
import com.claroinvestments.stockprice.db.HistoricalStockPriceRepository;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
public class StockFileDataMapperService{
	
	@Autowired
	HistoricalStockPriceRepository historicalStockPriceRepository;
	
	Log log = LogFactory.getLog(StockFileDataMapperService.class);
	
	final ZoneId ZONE_ID_IST = ZoneId.of(ZoneId.SHORT_IDS.get("IST"));
	
	public Collection<HistoricalStockPrice> getHistoricalStockPrices(String jsonString, String ticker, String exchange, LocalDate startDate, LocalDate endDate, boolean overwrite){
		try {
			Map<Integer, HistoricalStockPrice> historicalStockPricesMap = new HashMap<>();
			JsonReader jsonReader = new JsonReader(new StringReader(jsonString));
			jsonReader.setLenient(true);
			JsonObject chartObject = JsonParser.parseReader(jsonReader).getAsJsonObject().get("chart").getAsJsonObject();
			JsonElement error = chartObject.get("error");
			if (!error.isJsonNull()) {

				String codeString = Optional.ofNullable(error.getAsJsonObject().get("code")).map(c -> c.getAsString())
						.orElseThrow(() -> new IOException("No 'code' found in 'error'"));

				String descriptionString = Optional.ofNullable(error.getAsJsonObject().get("description")).map(c -> c.getAsString())
						.orElseThrow(() -> new IOException("No 'description' found in 'error'"));

				throw new IOException("Unable to retrieve data: " + codeString + "|" + descriptionString);
			}
			JsonArray results = chartObject.getAsJsonArray("result");

			if (results == null || results.size() != 1) {
			    throw new IOException("result missing from Json body");
			}
			JsonObject result = results.get(0).getAsJsonObject();
			JsonArray timeStamps = Optional.ofNullable(result.get("timestamp")).map(r -> r.getAsJsonArray())
					.orElseThrow(() -> new IOException("No timestamps found"));

			if(Objects.isNull(timeStamps) || timeStamps.isEmpty()) {
				throw new IOException("Invalid timestamp: " + (Objects.isNull(timeStamps) ? "null" : "Empty list"));
			}

			int noOfTimeStamps = timeStamps.size();
			for(int idx = 0; idx < timeStamps.size() ; idx++) {
				long timeStamp = timeStamps.get(idx).getAsLong();
				LocalDate quoteDate = LocalDateTime.from(Instant.ofEpochSecond(timeStamp).atZone(ZONE_ID_IST)).toLocalDate();
				historicalStockPricesMap.put(idx, new HistoricalStockPrice(ticker, exchange, quoteDate));
			}
			JsonObject indicators = Optional.ofNullable(result.get("indicators")).map(r -> r.getAsJsonObject())
					.orElseThrow(() -> new IOException("'indicators' is null"));

			JsonObject quote = Optional.ofNullable(indicators.get("quote")).map(r -> r.getAsJsonArray().get(0).getAsJsonObject())
					.orElseThrow(() -> new IOException("'quote' is null"));

			JsonArray highs = quote.get("high").getAsJsonArray();
			if(Objects.isNull(highs) || highs.isEmpty()) {
				throw new IOException("Invalid highs: " + (Objects.isNull(highs) ? "null" : "Empty list"));
			}
			if(highs.size() != noOfTimeStamps) {
				throw new IOException("Count of highs (" + highs.size() + ") is not same as timestamps (" + noOfTimeStamps + ")");
			}

			BigDecimal previousHighPrice = BigDecimal.ZERO;
			for(int idx = 0; idx < highs.size() ; idx++) {
				HistoricalStockPrice historicalStockPrice = historicalStockPricesMap.get(idx);
				JsonElement highPrice = highs.get(idx);
				historicalStockPrice.setHighPrice(highPrice.isJsonNull() ? previousHighPrice : highPrice.getAsBigDecimal().setScale(2, RoundingMode.HALF_UP));
				previousHighPrice = historicalStockPrice.getHighPrice();
			}
		
			JsonArray lows = quote.get("low").getAsJsonArray();
			if(Objects.isNull(lows) || lows.isEmpty()) {
				throw new IOException("Invalid lows: " + (Objects.isNull(lows) ? "null" : "Size " + highs.size()));
			}
			if(lows.size() != noOfTimeStamps) {
				throw new IOException("Count of lows (" + lows.size() + ") is not same as timestamps (" + noOfTimeStamps + ")");
			}
			BigDecimal previousLowPrice = BigDecimal.ZERO;
			for(int idx = 0; idx < lows.size() ; idx++) {
				HistoricalStockPrice historicalStockPrice = historicalStockPricesMap.get(idx);
				JsonElement lowPrice = lows.get(idx);
				historicalStockPrice.setLowPrice(lowPrice.isJsonNull() ? previousLowPrice : lowPrice.getAsBigDecimal().setScale(2, RoundingMode.HALF_UP));
				previousLowPrice = historicalStockPrice.getLowPrice();
			}
			
			JsonArray opens = quote.get("open").getAsJsonArray();
			if(Objects.isNull(opens) || opens.isEmpty()) {
				throw new IOException("Invalid opens: " + (Objects.isNull(opens) ? "null" : "Size " + opens.size()));
			}
			if(opens.size() != noOfTimeStamps) {
				throw new IOException("Count of opens (" + opens.size() + ") is not same as timestamps (" + noOfTimeStamps + ")");
			}
			BigDecimal previousOpen = BigDecimal.ZERO;
			for(int idx = 0; idx < opens.size() ; idx++) {
				HistoricalStockPrice historicalStockPrice = historicalStockPricesMap.get(idx);
				JsonElement openPrice = opens.get(idx);
				historicalStockPrice.setOpen(openPrice.isJsonNull() ? previousOpen : openPrice.getAsBigDecimal().setScale(2, RoundingMode.HALF_UP));
				previousOpen = historicalStockPrice.getOpen();
			}
			
			JsonArray volumes = quote.get("volume").getAsJsonArray();
			if(Objects.isNull(volumes) || volumes.isEmpty()) {
				throw new IOException("Invalid volumes: " + (Objects.isNull(volumes) ? "null" : "Size " + volumes.size()));
			}
			if(volumes.size() != noOfTimeStamps) {
				throw new IOException("Count of volumes (" + volumes.size() + ") is not same as timestamps (" + noOfTimeStamps + ")");
			}
			Long previousVolume = 0L;
			for(int idx = 0; idx < volumes.size() ; idx++) {
				HistoricalStockPrice historicalStockPrice = historicalStockPricesMap.get(idx);
				JsonElement volume = volumes.get(idx);
				historicalStockPrice.setVolume(volume.isJsonNull() ? previousVolume : volume.getAsLong());
				previousVolume = historicalStockPrice.getVolume();
			}
			
			JsonArray closes = quote.get("close").getAsJsonArray();
			if(Objects.isNull(closes) || closes.isEmpty()) {
				throw new IOException("Invalid closes: " + (Objects.isNull(closes) ? "null" : "Size " + closes.size()));
			}
			if(closes.size() != noOfTimeStamps) {
				throw new IOException("Count of closes (" + closes.size() + ") is not same as timestamps (" + noOfTimeStamps + ")");
			}
			BigDecimal previousClose = BigDecimal.ZERO;
			for(int idx = 0; idx < closes.size() ; idx++) {
				HistoricalStockPrice historicalStockPrice = historicalStockPricesMap.get(idx);
				JsonElement closePrice = closes.get(idx);
				historicalStockPrice.setClosePrice(closePrice.isJsonNull() ? previousClose : closePrice.getAsBigDecimal().setScale(2, RoundingMode.HALF_UP));
				previousClose = historicalStockPrice.getClosePrice();
			}
			
			JsonArray adjCloses = indicators.get("adjclose").getAsJsonArray().get(0).getAsJsonObject().get("adjclose").getAsJsonArray();
			if(Objects.isNull(adjCloses) || adjCloses.size() == 0) {
				throw new IOException("Invalid adjAdjCloses: " + (Objects.isNull(adjCloses) ? "null" : "Size " + adjCloses.size()));
			}
			if(adjCloses.size() != noOfTimeStamps) {
				throw new IOException("Count of adjAdjCloses (" + adjCloses.size() + ") is not same as timestamps (" + noOfTimeStamps + ")");
			}
			BigDecimal previousAdjClose = BigDecimal.ZERO;
			for(int idx = 0; idx < adjCloses.size() ; idx++) {
				HistoricalStockPrice historicalStockPrice = historicalStockPricesMap.get(idx);
				JsonElement adjClosePrice = closes.get(idx);
				historicalStockPrice.setAdjustedClosePrice(adjClosePrice.isJsonNull() ? previousAdjClose : adjClosePrice.getAsBigDecimal().setScale(2, RoundingMode.HALF_UP));
				previousAdjClose = historicalStockPrice.getAdjustedClosePrice();
			}

			List<HistoricalStockPrice> historicalStockPrices = addForAbsentDates(historicalStockPricesMap.values(), endDate);
			if(overwrite) {
				historicalStockPrices = overwriteExistingBenchmarkHistoricalDatas(historicalStockPrices, exchange, ticker, startDate, endDate);
			}
			log.info("Retrieved " + historicalStockPrices.size() + " historical prices for " + ticker + "|" + exchange);
			return historicalStockPrices;
		}catch(Exception e) {
			log.error("Unable to retrieve historical price information for " + ticker + "|" + exchange, e);
			return Collections.emptyList();
		}
	}
	
	private List<HistoricalStockPrice> addForAbsentDates(Collection<HistoricalStockPrice> originalHistoricalStockPrices,LocalDate endDate) throws IOException{
		List<HistoricalStockPrice> addedHistoricalStockPrices = new ArrayList<>();
		
		LinkedHashMap<LocalDate, HistoricalStockPrice> originalHistoricalStockPriceMap = originalHistoricalStockPrices
				.stream()
				.sorted(Comparator.comparing(HistoricalStockPrice::getDate))
				.collect(Collectors.toMap(HistoricalStockPrice::getDate, b -> b, (b1, b2) -> b1, LinkedHashMap::new));
		
		LocalDate firstDate = originalHistoricalStockPrices.stream().map(HistoricalStockPrice::getDate).min(LocalDate::compareTo)
				.orElseThrow(() -> new IOException("Unable to get first date"));
		HistoricalStockPrice lastFetchedHistoricalStockPrice = originalHistoricalStockPriceMap.get(firstDate);
		
		List<LocalDate> dates = firstDate.datesUntil(endDate.plusDays(1)).toList();
		
		for(LocalDate date:dates) {
			if(originalHistoricalStockPriceMap.containsKey(date)) {
				HistoricalStockPrice originalHistoricalStockPrice = originalHistoricalStockPriceMap.get(date);
				addedHistoricalStockPrices.add(originalHistoricalStockPrice);
				lastFetchedHistoricalStockPrice = originalHistoricalStockPrice;
			}else {
				HistoricalStockPrice addedBenchmarkHistoricalData = new HistoricalStockPrice(
						null,
						lastFetchedHistoricalStockPrice.getTicker(),
						lastFetchedHistoricalStockPrice.getExchange(),
						date,
						lastFetchedHistoricalStockPrice.getOpen(),
						lastFetchedHistoricalStockPrice.getHighPrice(),
						lastFetchedHistoricalStockPrice.getLowPrice(),
						lastFetchedHistoricalStockPrice.getClosePrice(),
						lastFetchedHistoricalStockPrice.getAdjustedClosePrice(),
						lastFetchedHistoricalStockPrice.getVolume()
						);
				addedHistoricalStockPrices.add(addedBenchmarkHistoricalData);
			}
		}
		
		return addedHistoricalStockPrices;
	}
	
	protected List<HistoricalStockPrice> overwriteExistingBenchmarkHistoricalDatas(List<HistoricalStockPrice> fetchedHistoricalStockPrices, String exchange, String ticker, LocalDate startDate, LocalDate endDate) {
		List<HistoricalStockPrice> overwrittenHistoricalStockPrices = new ArrayList<>();
		LocalDate fromDate = startDate.minusDays(10);
		List<HistoricalStockPrice> dbDatas = historicalStockPriceRepository.findByExchangeAndTickerAndDateBetween(exchange, ticker, fromDate, endDate);
		log.info("Found " + dbDatas.size() + " for " + ticker + "|" + exchange + " from " + fromDate + " to " + endDate);
		Map<LocalDate, HistoricalStockPrice> storedHistoricalStockPriceMap = dbDatas.stream().collect(Collectors.toMap(HistoricalStockPrice::getDate, h -> h));
		for(HistoricalStockPrice fetchedHistoricalStockPrice:fetchedHistoricalStockPrices) {
			LocalDate fetchedHistoricalStockPriceDate = fetchedHistoricalStockPrice.getDate();
			Long id = Optional.ofNullable(storedHistoricalStockPriceMap.get(fetchedHistoricalStockPriceDate)).map(HistoricalStockPrice::getId).orElse(null);
			HistoricalStockPrice overwrittenHistoricalStockPrice = new HistoricalStockPrice(
					id,
					fetchedHistoricalStockPrice.getTicker(),
					fetchedHistoricalStockPrice.getExchange(),
					fetchedHistoricalStockPriceDate,
					fetchedHistoricalStockPrice.getOpen(),
					fetchedHistoricalStockPrice.getHighPrice(),
					fetchedHistoricalStockPrice.getLowPrice(),
					fetchedHistoricalStockPrice.getClosePrice(),
					fetchedHistoricalStockPrice.getAdjustedClosePrice(),
					fetchedHistoricalStockPrice.getVolume()
					);
			overwrittenHistoricalStockPrices.add(overwrittenHistoricalStockPrice);
		}
		return overwrittenHistoricalStockPrices;
	}
}
