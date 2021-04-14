package com.claroinvestments.stockprice;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.claroinvestments.stockprice.db.HistoricalStockPrice;
import com.claroinvestments.stockprice.db.HistoricalStockPriceRepository;

@Component
public class NiivoStockDataApp {
	Log log = LogFactory.getLog(NiivoStockDataApp.class);
	
	final Integer WRITE_FLUSH_LIMIT = 500000;
	
	@Autowired
	BseScripListDownloaderService bseScripListDownloaderService;
	
	@Autowired
	NseScripListDownloaderService nsseScripListDownloaderService;
	
	@Autowired
	StockPriceDataDownloaderService yahooFinanceStockInformationService;
	
	@Autowired
	StockFileDataMapperService stockFileDataMapperService;
	
	@Autowired
	HistoricalStockPriceRepository historicalStockPriceRepository;
	
	@Autowired
	File downloadDir;
	
    @Value("${HISTORICAL_STOCK_PRICES_OUTPUT_LOC}")
	String historicalStockPricesOutputLoc;
    
	final private StopWatch stopWatch = StopWatch.createStarted();
	
	AtomicLong id;
	
	public void init() throws IOException {
		log.info("Execution started");
		id = new AtomicLong(Optional.ofNullable(historicalStockPriceRepository.getMaxId()).orElse(1L));
	}
	
	@Transactional
	public void execute(LocalDate fromDate, LocalDate toDate, boolean overwrite) throws Exception {
		log.info("Getting From: " + fromDate + ", To: " + toDate);
		
		cleanDownloadDir();
		
		List<StockFileDownloadResult> stockFileDownloadResults = new ArrayList<StockFileDownloadResult>();
		List<StockFileDownloadResult> bseStockFileDownloadResults = downloadBse(fromDate, toDate);
		stockFileDownloadResults.addAll(bseStockFileDownloadResults);
		List<StockFileDownloadResult> nseStockFileDownloadResults = downloadNse(fromDate, toDate);
		stockFileDownloadResults.addAll(nseStockFileDownloadResults);
		
		if(overwrite) {
			mapAndSave(stockFileDownloadResults, fromDate, toDate, true);
		} else {
			File historicalStockPricesOutputFile = new File(historicalStockPricesOutputLoc);
			log.info("Deleting " + historicalStockPricesOutputFile.getAbsolutePath());
			Files.deleteIfExists(historicalStockPricesOutputFile.toPath());
			log.info("Done");
			log.info("Creating new file at " + historicalStockPricesOutputFile.getAbsolutePath());
			Files.createFile(historicalStockPricesOutputFile.toPath());
			mapAndWriteToFile(stockFileDownloadResults, historicalStockPricesOutputFile, fromDate, toDate, false);
		}
	}
	
	private void cleanDownloadDir() throws IOException{
		if(downloadDir.exists()) {
			log.info("Folder " + downloadDir.getAbsolutePath() + " exists, clearing it");
			FileUtils.cleanDirectory(downloadDir);
			log.info("Done");	
		}else {
			log.info("Folder " + downloadDir.getAbsolutePath() + "doesn't exist, creating it");
			Files.createDirectories(downloadDir.toPath());
			log.info("Done");
		}
	}
	
	private void mapAndSave(List<StockFileDownloadResult> stockFileDownloadResults, LocalDate fromDate, LocalDate toDate, boolean overwrite) throws Exception{
		List<HistoricalStockPrice> historicalStockPrices = new ArrayList<>();
		int total = stockFileDownloadResults.size();
		log.info("Processing " + total + " tickers");
		List<Future<Collection<HistoricalStockPrice>>> results = new ArrayList<>();
		for(StockFileDownloadResult stockFileDownloadResult:stockFileDownloadResults) {
			String stockPriceFileLoc = stockFileDownloadResult.getStockPriceFileLoc();
			String ticker = stockFileDownloadResult.getTicker();
			String exchange = stockFileDownloadResult.getExchangeName();
			Future<Collection<HistoricalStockPrice>> result = stockFileDataMapperService.getHistoricalStockPrices(Files.readString(new File(stockPriceFileLoc).toPath(), Charset.defaultCharset()), ticker, exchange, fromDate, toDate, overwrite);
			results.add(result);
		}
		
		int count = 1;
		
		for(Future<Collection<HistoricalStockPrice>> result:results) {
			Collection<HistoricalStockPrice> downloadedHistoricalStockPricesForTicker = result.get();
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
	
	private void mapAndWriteToFile(List<StockFileDownloadResult> stockFileDownloadResults, File historicalStockPricesOutputFile, LocalDate fromDate, LocalDate toDate, boolean overwrite) throws Exception {
		
		int total = stockFileDownloadResults.size();
		log.info("Processing " + total + " tickers");
		List<Future<Collection<HistoricalStockPrice>>> results = new ArrayList<>();
		for(StockFileDownloadResult stockFileDownloadResult:stockFileDownloadResults) {
			String stockPriceFileLoc = stockFileDownloadResult.getStockPriceFileLoc();
			String ticker = stockFileDownloadResult.getTicker();
			String exchange = stockFileDownloadResult.getExchangeName();
			Future<Collection<HistoricalStockPrice>> result = stockFileDataMapperService.getHistoricalStockPrices(Files.readString(new File(stockPriceFileLoc).toPath(), Charset.defaultCharset()), ticker, exchange, fromDate, toDate, overwrite);
			results.add(result);
		}
		
		List<HistoricalStockPrice> historicalStockPricesSaveList = new ArrayList<>();
		int count = 1;
		int totalCount = 0;
		
		int resultIdx = 1;
		int noOfResults = results.size();
		for(Future<Collection<HistoricalStockPrice>> result:results) {
			Collection<HistoricalStockPrice> downloadedHistoricalStockPricesForTicker = result.get();
			log.info("Mapping " + count + " of " + total);
			Integer noOfDownloadedHistoricalStockPricesForTicker = downloadedHistoricalStockPricesForTicker.size();
			downloadedHistoricalStockPricesForTicker.forEach(h -> {
				if(Objects.isNull(h.getId())) {
					h .setId(id.getAndIncrement());	
				}
			});
			
			totalCount += noOfDownloadedHistoricalStockPricesForTicker;
			historicalStockPricesSaveList.addAll(downloadedHistoricalStockPricesForTicker);
			log.info("Total processed " + totalCount);
			if((historicalStockPricesSaveList.size() >= WRITE_FLUSH_LIMIT) || (resultIdx == noOfResults)) {
				log.info("Writing " + historicalStockPricesSaveList.size());
				FileUtils.writeLines(historicalStockPricesOutputFile, historicalStockPricesSaveList, true);
				log.info("Written");
				historicalStockPricesSaveList.clear();
			}
			resultIdx++;
			count++;
		}
	}
	
	public void shutdown() throws IOException {
		this.stopWatch.stop();
		Long executionTime = this.stopWatch.getTime();
		log.info("Total execution time: " + DurationFormatUtils.formatDuration(executionTime, "mm:ss"));
		System.exit(0);
	}
	
	private List<StockFileDownloadResult> downloadNse(LocalDate fromDate, LocalDate toDate) throws Exception {
		List<StockFileDownloadResult> nseStockFileDownloadResults = new ArrayList<>();
		log.info("Downloading NSE Scrips List");
		byte[] nseScripsListFileContent = nsseScripListDownloaderService.getNseScripsListFileContent();
		log.info("Done");
		log.info("Iterating over NSE Data set");
		List<String> nseStockDataSet = IOUtils.readLines(new ByteArrayInputStream(nseScripsListFileContent), Charset.defaultCharset()).stream().skip(1).collect(Collectors.toList());
		
		int total = nseStockDataSet.size();
		List<Future<StockFileDownloadResult>> results = new ArrayList<>(total);
		yahooFinanceStockInformationService.reset(total);
		
		for(String nseStockString:nseStockDataSet) {
			String ticker = nseStockString.split(",")[0];
			results.add(yahooFinanceStockInformationService.download(fromDate, toDate,ticker,"NS","NSE"));
		}
		
		for(Future<StockFileDownloadResult> result:results) {
			StockFileDownloadResult stockFileDownloadResult = result.get();
			if(Objects.nonNull(nseStockFileDownloadResults)) {
				nseStockFileDownloadResults.add(stockFileDownloadResult);	
			}
		}
		log.info("NSE fetching done");
		return nseStockFileDownloadResults;
	}
	
	private List<StockFileDownloadResult> downloadBse(LocalDate fromDate, LocalDate toDate) throws Exception {
		List<StockFileDownloadResult> bseStockFileDownloadResults = new ArrayList<>();
		log.info("Downloading BSE Scrips List");
		byte[] bseScripsListFileContent = bseScripListDownloaderService.getBseScripsListFileContent();
		List<String> bseStockDataSet = IOUtils.readLines(new ByteArrayInputStream(bseScripsListFileContent), Charset.defaultCharset()).stream().skip(1).collect(Collectors.toList());
		
		int total = bseStockDataSet.size();
		List<Future<StockFileDownloadResult>> results = new ArrayList<>(total);
		
		yahooFinanceStockInformationService.reset(total);
		for(String bseStockString:bseStockDataSet) {
			String ticker = bseStockString.split(",")[2].replace("*", "").replaceAll(" ", "").replaceAll("'", "");
			results.add(yahooFinanceStockInformationService.download(fromDate, toDate,ticker,"BO","BSE"));
		}
		
		for(Future<StockFileDownloadResult> result:results) {
			StockFileDownloadResult stockFileDownloadResult = result.get();
			if(Objects.nonNull(bseStockFileDownloadResults)) {
				bseStockFileDownloadResults.add(stockFileDownloadResult);	
			}
		}
		log.info("BSE fetching done");
		return bseStockFileDownloadResults;
	}
	
}
