package com.claroinvestments.stockprice.db;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface HistoricalStockPriceRepository extends JpaRepository<HistoricalStockPrice, Integer> {
	
	List<HistoricalStockPrice> findByExchangeAndTickerAndDateBetween(String exchange, String ticker, LocalDate fromDate, LocalDate endDate);
	
	@Query("select max(h.id) from HistoricalStockPrice h")
	Long getMaxId();
}
