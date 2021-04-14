package com.claroinvestments.stockprice;

public class StockFileDownloadResult {
	private String stockPriceFileLoc;
	private String ticker;
	private String exchangeName;
	
	public StockFileDownloadResult(String stockPriceFileLoc, String ticker, String exchangeName) {
		this.stockPriceFileLoc = stockPriceFileLoc;
		this.ticker = ticker;
		this.exchangeName = exchangeName;
	}
	
	public StockFileDownloadResult() {}
			
	public String getStockPriceFileLoc() {
		return stockPriceFileLoc;
	}
	public void setStockPriceFileLoc(String stockPriceFileLoc) {
		this.stockPriceFileLoc = stockPriceFileLoc;
	}
	public String getTicker() {
		return ticker;
	}
	public void setTicker(String ticker) {
		this.ticker = ticker;
	}
	public String getExchangeName() {
		return exchangeName;
	}
	public void setExchangeName(String exchangeName) {
		this.exchangeName = exchangeName;
	}
	
}
