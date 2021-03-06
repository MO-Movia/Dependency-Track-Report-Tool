/*
 * [FS] IRAD-1027 2020-08-05
 * 
 */
package com.modusoperandi.utility;

import java.util.Properties;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import com.modusoperandi.jenkins.plugins.ConsoleLogger;

/**
 * Third Party Audit & License Validation Utility
 * using Dependency-Track
 */
public class ThirdPartyAuditNLicenseValidation 
{
	/**
	 * Dependency-Track API URL
	 */
	private String restAPILoc;
	
	/**
	 * Dependency-Track API KEY
	 */
	private String restAPIKey;
	
	private String restAPIPID;

	private String licXlate;

	private String appovedLic;

	private String whiteList;

	private String auditRpt;

	private String licList;

	private String licText;	
	
	/**
	 * OutputStream Logger
	 */
	private ConsoleLogger logger;
	
	public ThirdPartyAuditNLicenseValidation(final ConsoleLogger logger) {
		this.logger = logger;
	}
	
	public void initialize(final String restAPILoc, final String restAPIKey, final String restAPIPID, 
						final String appovedLic, final String licXlate, final String whiteList, 
						final String auditRpt, final String licList, final String licText) {
		setRestAPILoc(restAPILoc);
		setRestAPIKey(restAPIKey);
		setRestAPIPID(restAPIPID);
		setAppovedLic(appovedLic);
		setLicXlate(licXlate);
		setWhiteList(whiteList);
		setAuditRpt(auditRpt);
		setLicList(licList);
		setLicText(licText);
		doJOB();
	}
	
	private void setRestAPILoc(final String restAPILoc) {
		this.restAPILoc = (null == this.restAPILoc) ? restAPILoc : this.restAPILoc;
	}
	
	private void setRestAPIKey(final String restAPIKey) {
		this.restAPIKey = (null == this.restAPIKey) ? restAPIKey : this.restAPIKey;
	}
	
	private void setRestAPIPID(final String restAPIPID) {
		this.restAPIPID = (null == this.restAPIPID) ? restAPIPID : this.restAPIPID;
	}

	private void setAppovedLic(final String appovedLic) {
		this.appovedLic = (null == this.appovedLic) ? appovedLic : this.appovedLic;
	}	

	private void setLicXlate(final String licXlate) {
		this.licXlate = (null == this.licXlate) ? licXlate : this.licXlate;
	}
	
	private void setWhiteList(final String whiteList) {
		this.whiteList = (null == this.whiteList) ? whiteList : this.whiteList;
	}

	private void setAuditRpt(final String auditRpt) {
		this.auditRpt = (null == this.auditRpt) ? auditRpt : this.auditRpt;
	}

	private void setLicList(final String licList) {
		this.licList = (null == this.licList) ? licList : this.licList;
	}

	private void setLicText(final String licText) {
		this.licText = (null == this.licText) ? licText : this.licText;
	}
	
	private void doJOB() {
		try {		
			final ApiClient apiClient = new ApiClient(this.restAPILoc, this.restAPIKey, this.logger);
			final String result = apiClient.getDependencies(this.restAPIPID);			
			Processor processor = new Processor(this.logger, this.appovedLic, this.licXlate, this.whiteList, apiClient);
			processor.validateLibs(result);
			processor.generateOutputFiles(this.auditRpt, this.licList, this.licText);
			
		} catch (Exception ex) {
			this.logger.log( "doJOB error " + ex.getMessage());
		}
	}
	
	
}