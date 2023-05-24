package com.claroinvestments.stockprice.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface NseTradingSecuritiesRepository extends JpaRepository<NseTradingSecurities, String>, JpaSpecificationExecutor<NseTradingSecurities> {

}