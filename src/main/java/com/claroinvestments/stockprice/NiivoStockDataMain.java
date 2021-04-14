package com.claroinvestments.stockprice;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

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
		LocalDate startLimitDate = LocalDate.of(2000, 1, 1);
		LocalDate today = LocalDate.now();
		
		ExecutionType executionType = ExecutionType.valueOf(args[0]);
		switch(executionType) {
		case DAILY: 
			LocalDate lastSundayOfTheMonth = today.with(TemporalAdjusters.lastInMonth(DayOfWeek.SUNDAY));
			LocalDate dailyRunStartDate = today.isEqual(lastSundayOfTheMonth) ? startLimitDate : today.minusDays(noOfDaysBack);
			LocalDate dailyRunEndDate = today.minusDays(1);
			niivoStockDataApp.init();
			niivoStockDataApp.execute(dailyRunStartDate, dailyRunEndDate, true);
			niivoStockDataApp.shutdown();
			break;
		case HISTORICAL:
			LocalDate historicalStartDate = startLimitDate;
			LocalDate historicalEndDate = today.minusDays(1);
			niivoStockDataApp.init();
			niivoStockDataApp.execute(historicalStartDate, historicalEndDate, false);
			niivoStockDataApp.shutdown();
			break;
		}
	}
	
}
