package com.claroinvestments.stockprice;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

@Service
public class NseScripListDownloaderService {
	
	Log log = LogFactory.getLog(NseScripListDownloaderService.class);
	
	public byte[] getNseScripsListFileContent() throws IOException {
		String downloadUrlNse = "https://www1.nseindia.com/content/equities/EQUITY_L.csv";
		log.info("Downloading NSE Scrips List");
		byte[] nseScripsListFileContent = IOUtils.toByteArray(new URL(downloadUrlNse).openStream());
		return nseScripsListFileContent;
	}
}
