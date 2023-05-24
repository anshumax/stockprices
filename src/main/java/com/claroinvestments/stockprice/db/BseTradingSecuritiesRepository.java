package com.claroinvestments.stockprice.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BseTradingSecuritiesRepository extends JpaRepository<BseTradingSecurities, Integer>, JpaSpecificationExecutor<BseTradingSecurities> {

}