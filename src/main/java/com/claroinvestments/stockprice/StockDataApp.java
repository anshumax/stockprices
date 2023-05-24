package com.claroinvestments.stockprice;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import com.claroinvestments.stockprice.db.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class StockDataApp {
	Log log = LogFactory.getLog(StockDataApp.class);
	
	final Integer FILE_WRITE_FLUSH_LIMIT = 500000;
	final Integer DB_SAVE_FLUSH_LIMIT = 100000;

	@Autowired
	BseTradingSecuritiesRepository bseTradingSecuritiesRepository;
	
	@Autowired
	NseTradingSecuritiesRepository nseTradingSecuritiesRepository;
	
	@Autowired
	YahooStockDataService yahooStockDataService;
	
	@Autowired
	StockFileDataMapperService stockFileDataMapperService;

	@Autowired
	HistoricalStockPriceRepository historicalStockPriceRepository;
	@Autowired
	File historicalStockPricesOutputFile;
	@Autowired
	Boolean cleanDir;
	@Autowired
	File downloadDir;
	final private StopWatch stopWatch = StopWatch.createStarted();

	@Autowired
	JdbcTemplate jdbcTemplate;
	
	AtomicLong id;
	
	public void init() throws IOException {
		log.info("Execution started");
		id = new AtomicLong(Optional.ofNullable(historicalStockPriceRepository.getMaxId()).orElse(1L));
	}
	
	@Transactional
	public void execute(LocalDate fromDate, LocalDate toDate, boolean overwrite) throws Exception {
		List<StockFileDownloadResult> stockFileDownloadResults = new ArrayList<>();
		if(cleanDir){
			log.info("Getting From: " + fromDate + ", To: " + toDate);


//		Supplier<List<String>> bseSymbolSupplier = () -> bseTradingSecuritiesRepository.findAll().stream().map(BseTradingSecurities::getSymbol).toList();
//		List<StockFileDownloadResult> bseStockFileDownloadResults = downloadForExchange(fromDate, toDate, bseSymbolSupplier,"BO","BSE");
//		stockFileDownloadResults.addAll(bseStockFileDownloadResults);

			Supplier<List<String>> nseSymbolSupplier = () -> nseTradingSecuritiesRepository.findAll().stream().map(NseTradingSecurities::getSymbol).toList();
			List<StockFileDownloadResult> nseStockFileDownloadResults = downloadForExchange(fromDate, toDate, nseSymbolSupplier,"NS","NSE");
			stockFileDownloadResults.addAll(nseStockFileDownloadResults);
		}else{
			stockFileDownloadResults = Arrays.stream(downloadDir.listFiles(f -> !f.isHidden())).map(f -> {
				String[] strComponents = f.getName().split("[.]");
				return new StockFileDownloadResult(f.getAbsolutePath(), strComponents[0], strComponents[1]);
			}).toList();
		}

		if(overwrite) {
			mapAndSave(stockFileDownloadResults, fromDate, toDate);
		} else {
			mapAndWriteToFile(stockFileDownloadResults, fromDate, toDate);
//			mapAndSaveInBatches(stockFileDownloadResults, fromDate, toDate);
		}
	}
	
	private void mapAndSave(List<StockFileDownloadResult> stockFileDownloadResults, LocalDate fromDate, LocalDate toDate) throws Exception{
		List<HistoricalStockPrice> historicalStockPrices = new ArrayList<>();
		int total = stockFileDownloadResults.size(), count = 1;;
		log.info("Processing " + total + " tickers");
		List<Future<Collection<HistoricalStockPrice>>> results = new ArrayList<>();
		for(StockFileDownloadResult stockFileDownloadResult:stockFileDownloadResults) {
			String stockPriceFileLoc = stockFileDownloadResult.getStockPriceFileLoc();
			String ticker = stockFileDownloadResult.getTicker();
			String exchange = stockFileDownloadResult.getExchangeName();
			Collection<HistoricalStockPrice> downloadedHistoricalStockPricesForTicker = stockFileDataMapperService.getHistoricalStockPrices(Files.readString(new File(stockPriceFileLoc).toPath(), Charset.defaultCharset()), ticker, exchange, fromDate, toDate, true);
			log.info("Mapped " + count + " of " + total);
			historicalStockPrices.addAll(downloadedHistoricalStockPricesForTicker);
			count++;
		}

		AtomicLong newData = new AtomicLong(0);
		AtomicLong existingData = new AtomicLong(0);
		historicalStockPrices.forEach(h -> {
			if(Objects.isNull(h.getId()) || (h.getId().compareTo(0L) == 0)) {
				newData.incrementAndGet();
			}else {
				existingData.incrementAndGet();
			}
		});
		
		
		log.info("Saving " + historicalStockPrices.size() + " Historical Stock Prices, out of which " + newData.get() + " are new and " + existingData.get() + " are existing");
		historicalStockPrices.forEach(h -> {
			if(Objects.isNull(h.getId())) {
				h .setId(id.getAndIncrement());	
			}
		});
		historicalStockPriceRepository.saveAll(historicalStockPrices);
		
		log.info("Done");
	}
	
	private void mapAndWriteToFile(List<StockFileDownloadResult> stockFileDownloadResults, LocalDate fromDate, LocalDate toDate) throws Exception {
		List<HistoricalStockPrice> historicalStockPricesSaveList = new ArrayList<>();
		int count = 1, totalCount = 0, resultIdx = 1, total = stockFileDownloadResults.size();
		log.info("Processing " + total + " tickers");
		for(StockFileDownloadResult stockFileDownloadResult:stockFileDownloadResults) {
			String stockPriceFileLoc = stockFileDownloadResult.getStockPriceFileLoc();
			String ticker = stockFileDownloadResult.getTicker();
			String exchange = stockFileDownloadResult.getExchangeName();
			Collection<HistoricalStockPrice> downloadedHistoricalStockPricesForTicker = stockFileDataMapperService.getHistoricalStockPrices(Files.readString(new File(stockPriceFileLoc).toPath(), Charset.defaultCharset()), ticker, exchange, fromDate, toDate, false);
			log.info("Mapping " + count + " of " + total);
			int noOfDownloadedHistoricalStockPricesForTicker = downloadedHistoricalStockPricesForTicker.size();
			downloadedHistoricalStockPricesForTicker.forEach(h -> {
				if(Objects.isNull(h.getId())) {
					h .setId(id.getAndIncrement());
				}
			});

			totalCount += noOfDownloadedHistoricalStockPricesForTicker;
			historicalStockPricesSaveList.addAll(downloadedHistoricalStockPricesForTicker);
			log.info("Total processed " + totalCount);
			if((historicalStockPricesSaveList.size() >= FILE_WRITE_FLUSH_LIMIT) || (resultIdx == total)) {
				log.info("Writing " + historicalStockPricesSaveList.size());
				FileUtils.writeLines(historicalStockPricesOutputFile, historicalStockPricesSaveList, true);
				log.info("Written");
				historicalStockPricesSaveList.clear();
			}
			resultIdx++;
			count++;
		}
	}

	private void mapAndSaveInBatches(List<StockFileDownloadResult> stockFileDownloadResults, LocalDate fromDate, LocalDate toDate) throws Exception {
		int count = 1;
		int totalCount = 0;
		int total = stockFileDownloadResults.size();
		List<HistoricalStockPrice> historicalStockPricesForTicker = new ArrayList<>();
		log.info("Processing " + total + " tickers");
		for (StockFileDownloadResult stockFileDownloadResult : stockFileDownloadResults) {
			String stockPriceFileLoc = stockFileDownloadResult.getStockPriceFileLoc();
			String ticker = stockFileDownloadResult.getTicker();
			String exchange = stockFileDownloadResult.getExchangeName();
			Collection<HistoricalStockPrice> downloadedHistoricalStockPricesForTicker = stockFileDataMapperService.getHistoricalStockPrices(Files.readString(new File(stockPriceFileLoc).toPath(), Charset.defaultCharset()), ticker, exchange, fromDate, toDate, false);
			log.info("Mapping " + count + " of " + total);
			historicalStockPricesForTicker.addAll(downloadedHistoricalStockPricesForTicker);
			downloadedHistoricalStockPricesForTicker.forEach(h -> {
				if (Objects.isNull(h.getId())) {
					h.setId(id.getAndIncrement());
				}
			});

			if (historicalStockPricesForTicker.size() > DB_SAVE_FLUSH_LIMIT) {
				log.info("Saving " + historicalStockPricesForTicker.size() + " records");
				historicalStockPriceRepository.saveInBatches(jdbcTemplate, historicalStockPricesForTicker);
				log.info("Done - Total Saved " + totalCount);
				historicalStockPricesForTicker.clear();
			}
			totalCount += downloadedHistoricalStockPricesForTicker.size();
			count++;
		}
	}

	public void shutdown() {
		this.stopWatch.stop();
		long executionTime = this.stopWatch.getTime();
		log.info("Total execution time: " + DurationFormatUtils.formatDuration(executionTime, "mm:ss"));
		System.exit(0);
	}
	
	private List<StockFileDownloadResult> downloadForExchange(LocalDate fromDate, LocalDate toDate, Supplier<List<String>> supplier, String exchangeShort, String exchangeName) throws Exception {
		List<StockFileDownloadResult> nseStockFileDownloadResults = new ArrayList<>();
		log.info("Getting " + exchangeName + " Scrips List");
		List<String> nseSymbolList = supplier.get();
		
		int total = nseSymbolList.size();
		yahooStockDataService.reset(total);
		
		for(String ticker:nseSymbolList) {
			StockFileDownloadResult stockFileDownloadResult = yahooStockDataService.download(fromDate, toDate,ticker,exchangeShort, exchangeName);
			if(Objects.nonNull(stockFileDownloadResult)){
				nseStockFileDownloadResults.add(stockFileDownloadResult);
			}
		}

		log.info(exchangeName + " fetching done");
		return nseStockFileDownloadResults;
	}


}
