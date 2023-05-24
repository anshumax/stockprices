package com.claroinvestments.stockprice.db;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "historical_stock_prices")
public class HistoricalStockPrice implements Serializable{

	private static final long serialVersionUID = -7702016658803902313L;
	
	@Id
	@Column(name = "ID")
	private Long id;
	@Column(name = "TICKER")
	private String ticker;
	@Column(name = "EXCHANGE")
	private String exchange;
	@Column(name = "DATE")
	private LocalDate date;
	@Column(name = "OPEN_PRICE")
	private BigDecimal open = BigDecimal.ZERO;
	@Column(name = "HIGH_PRICE")
	private BigDecimal highPrice = BigDecimal.ZERO;
	@Column(name = "LOW_PRICE")
	private BigDecimal lowPrice = BigDecimal.ZERO;
	@Column(name = "CLOSE_PRICE")
	private BigDecimal closePrice = BigDecimal.ZERO;
	@Column(name = "ADJUSTED_CLOSE_PRICE")
	private BigDecimal adjustedClosePrice = BigDecimal.ZERO;
	@Column(name = "VOLUME")
	private Long volume = 0L;
	
	
	public HistoricalStockPrice(Long id, String ticker, String exchange, LocalDate date, BigDecimal open, BigDecimal highPrice,
			BigDecimal lowPrice, BigDecimal closePrice, BigDecimal adjustedClosePrice, Long volume) {
		this.id = id;
		this.ticker = ticker;
		this.exchange = exchange;
		this.date = date;
		this.open = open;
		this.highPrice = highPrice;
		this.lowPrice = lowPrice;
		this.closePrice = closePrice;
		this.adjustedClosePrice = adjustedClosePrice;
		this.volume = volume;
	}
	
	public HistoricalStockPrice(String ticker, String exchange, LocalDate date, BigDecimal open, BigDecimal highPrice,
			BigDecimal lowPrice, BigDecimal closePrice, BigDecimal adjustedClosePrice, Long volume) {
		this.ticker = ticker;
		this.exchange = exchange;
		this.date = date;
		this.open = open;
		this.highPrice = highPrice;
		this.lowPrice = lowPrice;
		this.closePrice = closePrice;
		this.adjustedClosePrice = adjustedClosePrice;
		this.volume = volume;
	}
	
	public HistoricalStockPrice(Long id, String ticker, String exchange, LocalDate date) {
		this.id = id;
		this.ticker = ticker;
		this.exchange = exchange;
		this.date = date;
	}
	
	public HistoricalStockPrice(String ticker, String exchange, LocalDate date) {
		this.ticker = ticker;
		this.exchange = exchange;
		this.date = date;
	}
	
	public HistoricalStockPrice() {}
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getTicker() {
		return ticker;
	}
	public void setTicker(String ticker) {
		this.ticker = ticker;
	}
	public String getExchange() {
		return exchange;
	}
	public void setExchange(String exchange) {
		this.exchange = exchange;
	}
	public LocalDate getDate() {
		return date;
	}
	public void setDate(LocalDate date) {
		this.date = date;
	}
	public BigDecimal getOpen() {
		return open;
	}
	public void setOpen(BigDecimal open) {
		this.open = open;
	}
	public BigDecimal getHighPrice() {
		return highPrice;
	}
	public void setHighPrice(BigDecimal highPrice) {
		this.highPrice = highPrice;
	}
	public BigDecimal getLowPrice() {
		return lowPrice;
	}
	public void setLowPrice(BigDecimal lowPrice) {
		this.lowPrice = lowPrice;
	}
	public BigDecimal getClosePrice() {
		return closePrice;
	}
	public void setClosePrice(BigDecimal closePrice) {
		this.closePrice = closePrice;
	}
	public BigDecimal getAdjustedClosePrice() {
		return adjustedClosePrice;
	}
	public void setAdjustedClosePrice(BigDecimal adjustedClosePrice) {
		this.adjustedClosePrice = adjustedClosePrice;
	}
	public Long getVolume() {
		return volume;
	}
	public void setVolume(Long volume) {
		this.volume = volume;
	}

	@Override
	public String toString() {
		return id + "|" + ticker + "|" + exchange + "|" + date.format(DateTimeFormatter.ISO_LOCAL_DATE) + "|" 
				+ open.toPlainString() + "|" + highPrice.toPlainString() + "|" + lowPrice.toPlainString() + "|"
				+ closePrice.toPlainString() + "|" + adjustedClosePrice.toPlainString() + "|" + volume;
	}
	
	
	
}
