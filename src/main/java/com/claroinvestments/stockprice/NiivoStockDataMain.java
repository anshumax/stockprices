package com.claroinvestments.stockprice;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NiivoStockDataMain implements CommandLineRunner{

	@Autowired
	NiivoStockDataApp niivoStockDataApp;
	
	@Value("${NO_DAYS_BACK}")
	Integer noOfDaysBack;
	
	public static void main(String[] args) {
		SpringApplication.run(NiivoStockDataMain.class, args);
	}
	
	@Override
	public void run(String... args) throws Exception {
		ExecutionType executionType = ExecutionType.valueOf(args[0]);
		switch(executionType) {
		case DAILY: 
			LocalDate dailyRunStartDate = LocalDate.now().minusDays(noOfDaysBack);
			LocalDate dailyRunEndDate = LocalDate.now().minusDays(1);
			niivoStockDataApp.init();
			niivoStockDataApp.execute(dailyRunStartDate, dailyRunEndDate, true);
			niivoStockDataApp.shutdown();
			break;
		case HISTORICAL:
			LocalDate historicalStartDate = LocalDate.of(2000, 1, 1);
			LocalDate historicalEndDate = LocalDate.now().minusDays(1);
			niivoStockDataApp.init();
			niivoStockDataApp.execute(historicalStartDate, historicalEndDate, false);
			niivoStockDataApp.shutdown();
			break;
		}
	}
	
}
