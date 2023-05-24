package com.claroinvestments.stockprice.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "bse_trading_securities")
public class BseTradingSecurities implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "SECURITY_CODE", nullable = false)
    private Integer securityCode;

    @Column(name = "ISSUER_NAME")
    private String issuerName;

    @Column(name = "SYMBOL")
    private String symbol;

    @Column(name = "SECURITY_NAME")
    private String securityName;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "SECURITY_GROUP")
    private String securityGroup;

    @Column(name = "FACE_VAUE")
    private String faceVaue;

    @Column(name = "ISIN")
    private String isin;

    @Column(name = "INDUSTRY")
    private String industry;

    @Column(name = "INSTRUMENT")
    private String instrument;

    @Column(name = "SECTOR_NAME")
    private String sectorName;

    @Column(name = "INDUSTRY_NEW_NAME")
    private String industryNewName;

    @Column(name = "IGROUP_NAME")
    private String igroupName;

    @Column(name = "ISUBGROUP_NAME")
    private String isubgroupName;

    public Integer getSecurityCode() {
        return securityCode;
    }

    public void setSecurityCode(Integer securityCode) {
        this.securityCode = securityCode;
    }

    public String getIssuerName() {
        return issuerName;
    }

    public void setIssuerName(String issuerName) {
        this.issuerName = issuerName;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getSecurityName() {
        return securityName;
    }

    public void setSecurityName(String securityName) {
        this.securityName = securityName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSecurityGroup() {
        return securityGroup;
    }

    public void setSecurityGroup(String securityGroup) {
        this.securityGroup = securityGroup;
    }

    public String getFaceVaue() {
        return faceVaue;
    }

    public void setFaceVaue(String faceVaue) {
        this.faceVaue = faceVaue;
    }

    public String getIsin() {
        return isin;
    }

    public void setIsin(String isin) {
        this.isin = isin;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getInstrument() {
        return instrument;
    }

    public void setInstrument(String instrument) {
        this.instrument = instrument;
    }

    public String getSectorName() {
        return sectorName;
    }

    public void setSectorName(String sectorName) {
        this.sectorName = sectorName;
    }

    public String getIndustryNewName() {
        return industryNewName;
    }

    public void setIndustryNewName(String industryNewName) {
        this.industryNewName = industryNewName;
    }

    public String getIgroupName() {
        return igroupName;
    }

    public void setIgroupName(String igroupName) {
        this.igroupName = igroupName;
    }

    public String getIsubgroupName() {
        return isubgroupName;
    }

    public void setIsubgroupName(String isubgroupName) {
        this.isubgroupName = isubgroupName;
    }
}
