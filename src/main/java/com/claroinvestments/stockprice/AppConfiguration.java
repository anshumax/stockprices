package com.claroinvestments.stockprice;

import java.io.File;
import java.util.concurrent.Executor;

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
    
    @Bean (name = "taskExecutor")
    public Executor taskExecutor() {
        log.debug("Creating Async Task Executor");
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(100);
        executor.setMaxPoolSize(120);
        executor.setThreadNamePrefix("Thread-");
        executor.initialize();
        return executor;
    }
    
    @Bean
    public File downloadDir() {
    	return new File(downloadDirLoc);
    }
    
}