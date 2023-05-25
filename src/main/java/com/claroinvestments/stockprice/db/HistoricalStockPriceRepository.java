package com.claroinvestments.stockprice.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;

public interface HistoricalStockPriceRepository extends JpaRepository<HistoricalStockPrice, Integer>, JpaSpecificationExecutor<HistoricalStockPrice> {

	List<HistoricalStockPrice> findByExchangeAndTickerAndDateBetween(String exchange, String ticker, LocalDate fromDate, LocalDate endDate);
	
	@Query("select max(h.id) from HistoricalStockPrice h")
	Long getMaxId();

	@Transactional
	default void saveInBatches(JdbcTemplate jdbcTemplate, List<HistoricalStockPrice> historicalStockPrices) {
		jdbcTemplate.batchUpdate("INSERT INTO historical_stock_prices (ID, TICKER, EXCHANGE, DATE, OPEN_PRICE, HIGH_PRICE, LOW_PRICE, CLOSE_PRICE, ADJUSTED_CLOSE_PRICE, VOLUME) " +
						"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
				historicalStockPrices,
				100,
				(PreparedStatement ps, HistoricalStockPrice h) -> {
					ps.setLong(1, h.getId());
					ps.setString(2, h.getTicker());
					ps.setString(3, h.getExchange());
					ps.setDate(4, java.sql.Date.valueOf(h.getDate()));
					ps.setBigDecimal(5,h.getOpen());
					ps.setBigDecimal(6,h.getHighPrice());
					ps.setBigDecimal(7,h.getLowPrice());
					ps.setBigDecimal(8,h.getClosePrice());
					ps.setBigDecimal(9,h.getAdjustedClosePrice());
					ps.setLong(10,h.getVolume());
				});
	}
}
