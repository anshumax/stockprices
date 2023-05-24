package com.claroinvestments.stockprice;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Executor;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AppConfiguration {
    private static final Log log = LogFactory.getLog(AppConfiguration.class);
    
    @Value("${DOWNLOAD_DIR_LOC}")
	String downloadDirLoc;

    @Value("${HISTORICAL_STOCK_PRICES_OUTPUT_LOC}")
    String historicalStockPricesOutputLoc;

    @Value("${cleanDir}")
    boolean cleanDir;
    
    @Bean (name = "taskExecutor")
    public Executor taskExecutor() {
        log.debug("Creating Async Task Executor");
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(6);
        executor.setThreadNamePrefix("Thread-");
        executor.initialize();
        return executor;
    }
    
    @Bean
    public File downloadDir() throws IOException {
        File downloadDir = new File(downloadDirLoc);
        if(downloadDir.exists()) {
            if(cleanDir){
                log.info("Folder " + downloadDir.getAbsolutePath() + " exists, clearing it");
                FileUtils.cleanDirectory(downloadDir);
                log.info("Done");
            }else{
                log.info("Folder " + downloadDir.getAbsolutePath() + " exists - not clearing it");
            }
        }else {
            log.info("Folder " + downloadDir.getAbsolutePath() + "doesn't exist, creating it");
            Files.createDirectories(downloadDir.toPath());
            log.info("Done");
        }
    	return downloadDir;
    }

    @Bean File historicalStockPricesOutputFile() throws IOException{
        File historicalStockPricesOutputFile = new File(historicalStockPricesOutputLoc);
        log.info("Deleting " + historicalStockPricesOutputFile.getAbsolutePath());
        Files.deleteIfExists(historicalStockPricesOutputFile.toPath());
        log.info("Done");
        log.info("Creating new file at " + historicalStockPricesOutputFile.getAbsolutePath());
        Files.createFile(historicalStockPricesOutputFile.toPath());
        return historicalStockPricesOutputFile;
    }

    @Bean Boolean cleanDir(){
        return cleanDir;
    }
    
}