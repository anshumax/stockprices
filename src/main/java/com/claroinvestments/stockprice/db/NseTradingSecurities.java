package com.claroinvestments.stockprice.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.sql.Date;

@Entity
@Table(name = "nse_trading_securities")
public class NseTradingSecurities implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "SYMBOL", nullable = false)
    private String symbol;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "SERIES", nullable = false)
    private String series;

    @Column(name = "DATE_OF_LISTING")
    private Date dateOfListing;

    @Column(name = "PAID_UP_VALUE", nullable = false)
    private String paidUpValue;

    @Column(name = "MARKET_LOT", nullable = false)
    private String marketLot;

    @Column(name = "ISIN", nullable = false)
    private String isin;

    @Column(name = "FACE_VALUE", nullable = false)
    private String faceValue;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSeries() {
        return series;
    }

    public void setSeries(String series) {
        this.series = series;
    }

    public Date getDateOfListing() {
        return dateOfListing;
    }

    public void setDateOfListing(Date dateOfListing) {
        this.dateOfListing = dateOfListing;
    }

    public String getPaidUpValue() {
        return paidUpValue;
    }

    public void setPaidUpValue(String paidUpValue) {
        this.paidUpValue = paidUpValue;
    }

    public String getMarketLot() {
        return marketLot;
    }

    public void setMarketLot(String marketLot) {
        this.marketLot = marketLot;
    }

    public String getIsin() {
        return isin;
    }

    public void setIsin(String isin) {
        this.isin = isin;
    }

    public String getFaceValue() {
        return faceValue;
    }

    public void setFaceValue(String faceValue) {
        this.faceValue = faceValue;
    }
}
