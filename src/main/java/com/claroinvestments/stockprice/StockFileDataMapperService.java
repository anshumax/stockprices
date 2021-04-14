package com.claroinvestments.stockprice;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import com.claroinvestments.stockprice.db.HistoricalStockPrice;
import com.claroinvestments.stockprice.db.HistoricalStockPriceRepository;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

@Service
public class StockFileDataMapperService{
	
	@Autowired
	HistoricalStockPriceRepository historicalStockPriceRepository;
	
	Log log = LogFactory.getLog(StockFileDataMapperService.class);
	
	final ZoneId ZONE_ID_IST = ZoneId.of(ZoneId.SHORT_IDS.get("IST"));
	
	JsonParser jsonParser;
	AtomicLong id;
	
	@PostConstruct
	public void init(){
		 id = new AtomicLong(Optional.ofNullable(historicalStockPriceRepository.getMaxId()).orElse(1L));
		 jsonParser = new JsonParser();
	}
	
	@Async
	public Future<Collection<HistoricalStockPrice>> getHistoricalStockPrices(String jsonString, String ticker, String exchange, LocalDate startDate, LocalDate endDate, boolean overwrite){
		try {
			Map<Integer, HistoricalStockPrice> historicalStockPricesMap = new HashMap<>();
			JsonReader jsonReader = new JsonReader(new StringReader(jsonString));
			jsonReader.setLenient(true);
			JsonObject chartObject = jsonParser.parse(jsonReader).getAsJsonObject().get("chart").getAsJsonObject();
			JsonElement error = chartObject.get("error");
			if (!error.isJsonNull()) {
				String codeString, descriptionString = null;
				
				JsonElement code = error.getAsJsonObject().get("code");
				if(Objects.isNull(code)) {
					throw new IOException("No 'code' found in 'error'");
				}else {
					codeString = code.getAsString();
				}
				JsonElement description = error.getAsJsonObject().get("code");
				if(Objects.isNull(description)) {
					throw new IOException("No 'description' found in 'error'");
				}else {
					descriptionString = description.getAsString();
				}
				throw new IOException("Unable to retrieve data: " + codeString + "|" + descriptionString);
			}
			JsonArray results = chartObject.getAsJsonArray("result");
			if (results == null || results.size() != 1) {
			    throw new IOException("result missing from Json body");
			}
			JsonObject result = results.get(0).getAsJsonObject();
			if(!result.has("timestamp")) {
				throw new IOException("No timestamps found");
			}
			
			JsonArray timeStamps = result.get("timestamp").getAsJsonArray();
			if(Objects.isNull(timeStamps) || timeStamps.size() == 0) {
				throw new IOException("Invalid timestamp: " + (Objects.isNull(timeStamps) ? "null" : "Size " + timeStamps.size()));
			}
			Integer noOfTimeStamps = timeStamps.size();
			for(int idx = 0; idx < timeStamps.size() ; idx++) {
				Long timeStamp = timeStamps.get(idx).getAsLong();
				LocalDate quoteDate = LocalDateTime.from(Instant.ofEpochSecond(timeStamp).atZone(ZONE_ID_IST)).toLocalDate();
				historicalStockPricesMap.put(idx, new HistoricalStockPrice(ticker, exchange, quoteDate));
			}
			JsonObject indicators = result.get("indicators").getAsJsonObject();
			if(Objects.isNull(indicators)) {
				throw new IOException("indicators is null");
			}
			JsonElement quotes = indicators.get("quote");
			if(Objects.isNull(quotes)) {
				throw new IOException("quote is null");
			}
			JsonArray highs = quotes.getAsJsonArray().get(0).getAsJsonObject().get("high").getAsJsonArray();
			if(Objects.isNull(highs) || highs.size() == 0) {
				throw new IOException("Invalid highs: " + (Objects.isNull(highs) ? "null" : "Size " + highs.size()));
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
		
			JsonArray lows = quotes.getAsJsonArray().get(0).getAsJsonObject().get("low").getAsJsonArray();
			if(Objects.isNull(lows) || lows.size() == 0) {
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
			
			JsonArray opens = quotes.getAsJsonArray().get(0).getAsJsonObject().get("open").getAsJsonArray();
			if(Objects.isNull(opens) || opens.size() == 0) {
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
			
			JsonArray volumes = quotes.getAsJsonArray().get(0).getAsJsonObject().get("volume").getAsJsonArray();
			if(Objects.isNull(volumes) || volumes.size() == 0) {
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
			
			JsonArray closes = quotes.getAsJsonArray().get(0).getAsJsonObject().get("close").getAsJsonArray();
			if(Objects.isNull(closes) || closes.size() == 0) {
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
			List<HistoricalStockPrice> historicalStockPrices = addForAbsentDates(historicalStockPricesMap.values(), startDate, endDate);
			if(overwrite) {
				historicalStockPrices = overwriteExistingBenchmarkHistoricalDatas(historicalStockPrices, exchange, ticker, startDate, endDate);
			}
			log.info("Retrieved " + historicalStockPrices.size() + " historical prices for " + ticker + "|" + exchange);
			return new AsyncResult<Collection<HistoricalStockPrice>>(historicalStockPrices);
		}catch(Exception e) {
			log.error("Unable to retrieve historical price information for " + ticker + "|" + exchange + ": " + e.getMessage());
			return new AsyncResult<Collection<HistoricalStockPrice>>(Collections.emptyList());
		}
	}
	
	private List<HistoricalStockPrice> addForAbsentDates(Collection<HistoricalStockPrice> originalHistoricalStockPrices, LocalDate startDate, LocalDate endDate) throws IOException{
		List<HistoricalStockPrice> addedHistoricalStockPrices = new ArrayList<>();
		
		LinkedHashMap<LocalDate, HistoricalStockPrice> originalHistoricalStockPriceMap = originalHistoricalStockPrices
				.stream()
				.sorted((b1,b2) -> b1.getDate().compareTo(b2.getDate()))
				.collect(Collectors.toMap(b -> b.getDate(), b -> b, (b1,b2) -> b1, () -> new LinkedHashMap<>()));
		
		LocalDate firstDate = originalHistoricalStockPrices.stream().map(b -> b.getDate()).min((d1,d2) -> d1.compareTo(d2))
				.orElseThrow(() -> new IOException("Unable to get first date"));
		HistoricalStockPrice lastFetchedHistoricalStockPrice = originalHistoricalStockPriceMap.get(firstDate);
		
		List<LocalDate> dates = firstDate.datesUntil(endDate.plusDays(1)).collect(Collectors.toList());
		
		for(LocalDate date:dates) {
			if(originalHistoricalStockPriceMap.containsKey(date)) {
				HistoricalStockPrice originalHistoricalStockPrice = originalHistoricalStockPriceMap.get(date);
				originalHistoricalStockPrice.setId(id.getAndIncrement());
				addedHistoricalStockPrices.add(originalHistoricalStockPrice);
				lastFetchedHistoricalStockPrice = originalHistoricalStockPrice;
			}else {
				HistoricalStockPrice addedBenchmarkHistoricalData = new HistoricalStockPrice(
						id.getAndIncrement(),
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
		LocalDate toDate = endDate;
		Map<LocalDate, HistoricalStockPrice> storedHistoricalStockPriceMap = historicalStockPriceRepository.findByExchangeAndTickerAndDateBetween(exchange, ticker, fromDate, toDate).stream().collect(Collectors.toMap(h -> h.getDate(), h -> h));
		for(HistoricalStockPrice fetchedHistoricalStockPrice:fetchedHistoricalStockPrices) {
			LocalDate fetchedHistoricalStockPriceDate = fetchedHistoricalStockPrice.getDate();
			Long id = Optional.ofNullable(storedHistoricalStockPriceMap.get(fetchedHistoricalStockPriceDate)).map(s -> s.getId()).orElse(null);
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