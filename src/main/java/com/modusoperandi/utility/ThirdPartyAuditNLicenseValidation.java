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

	private String noLicFix;
	
	private String licTextI;

	private String auditRpt;

	private String licList;

	private String licText;	
	
	/**
	 * OutputStream Logger
	 */
	private ConsoleLogger logger;
	
	private static final String DEF_RESTAPILOC = "http://localhost:8080/api";
	private static final String DEF_RESTAPIKEY = "";
	private static final String DEF_RESTAPIPID = "";
	private static final String DEF_APPOVEDLIC = "movia_approved_licenses.txt";
	private static final String DEF_LICXLATE = "movia_lic_xlate_list.txt";
	private static final String DEF_NOLICFIX = "movia_no_lic_fix.txt";
	private static final String DEF_LICTXTI = "License_text_input.txt";
	private static final String DEF_AUDITRPT = "movia_audit_out.csv";			
	private static final String DEF_LICLIST = "movia_license_list.csv";
	private static final String DEF_LICTEXT = "movia_license_text.txt";	
	
	public ThirdPartyAuditNLicenseValidation(String[] args) {
		initLogger();
		if(readArguments(args)) {
			loadProperties();
			doJOB();
		}
	}
	
	public ThirdPartyAuditNLicenseValidation(final String restAPILoc, final String restAPIKey, final String restAPIPID, 
											final String appovedLic, final String licXlate, final String noLicFix, 
											final String auditRpt, final String licList, final String licText,
											final String licTextI) {
		initLogger();
		setRestAPILoc(restAPILoc);
		setRestAPIKey(restAPIKey);
		setRestAPIPID(restAPIPID);
		setAppovedLic(appovedLic);
		setLicXlate(licXlate);
		setNoLicFix(noLicFix);
		setLicTextInput(licTextI);
		setAuditRpt(auditRpt);
		setLicList(licList);
		setLicText(licText);		
		loadProperties();
		doJOB();
	}
	
    public static void main(String[] args) {
        System.out.println( "Third Party Audit & License Validation Utility" );
		ThirdPartyAuditNLicenseValidation tpalvu = new ThirdPartyAuditNLicenseValidation(args);
    }
	
	private void initLogger() {
		try {		
			this.logger = new ConsoleLogger(null);
		} catch (Exception ex) {
			// error
			System.out.println("[TPALVU] " + ex.getMessage());
		}
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
	
	private void setNoLicFix(final String noLicFix) {
		this.noLicFix = (null == this.noLicFix) ? noLicFix : this.noLicFix;
	}
	
	private void setLicTextInput(final String licTextI) {
		this.licTextI = (null == this.licTextI) ? licTextI : this.licTextI;
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
	
	private boolean readArguments(String[] args) {
		boolean process = true;

		try {
			// each argument expected in the format -<option>=<value>
			for (String arg: args) {
				switch (arg.charAt(0)) {
					case '-':
						switch(arg.charAt(1)) {
							case 'U':
								this.setRestAPILoc(arg.substring(3));
								break;
							case 'K':
								this.setRestAPIKey(arg.substring(3));
								break;
							case 'I':
								this.setRestAPIPID(arg.substring(3));
								break;
							case 'A':
								this.setAppovedLic(arg.substring(3));
								break;
							case 'X':
								this.setLicXlate(arg.substring(3));
								break;
							case 'F':
								this.setNoLicFix(arg.substring(3));
								break;
							case 'S':
								this.setLicTextInput(arg.substring(3));
								break;								
							case 'R':
								this.setAuditRpt(arg.substring(3));
								break;
							case 'L':
								this.setLicList(arg.substring(3));
								break;
							case 'T':
								this.setLicText(arg.substring(3));
								break;
							case '?':
								process = false;
								this.displayHelp();
								break;								
							default:
								break;
						}
						break;
					default:
						break;
				}
			}
		} catch (Exception ex) {
			// error
			this.logger.log("[TPALVU] " + ex.getMessage());
		}
		
		return process;
	}
	
	private void displayHelp() {
		this.logger.log0("Usage: java -jar utility.tpalv-1.0.0-standalone.jar [-configurations]\r\nwhere each configuration is in the format -<configuration>=<value> and seperated by a space\r\n\r\nAvailable configurations are:\r\n\t-U\tDependency Track Rest API URL.\r\n\t-K\tDependency Track Rest API Key.\r\n\t-I\tDependency Track Project ID.\r\n\t-A\tFile path of the Approved Licenses.\r\n\t-X\tFile path of the License Translations.\r\n\t-F\tFile path of the No License Fix.\r\n\t-S\tFile path of the License Text Input.\r\n\t-R\tFile path of the Audit report csv.\r\n\t-L\tFile path of the unique License list.\r\n\t-T\tFile path of the License Text.\r\n\t-?\tprint help message.");
	}
	
	private void loadProperties() {
		this.logger.log( "\r\nloadProperties..." );
 
		try {
			File configFile = new File("config.properties");
			FileReader reader = new FileReader(configFile);
			Properties props = new Properties();
			props.load(reader);
		 
			this.setRestAPILoc(props.getProperty("REST_API_LOC", DEF_RESTAPILOC));
			
			this.setRestAPIKey(props.getProperty("REST_API_KEY", DEF_RESTAPIKEY));			
			
			this.setRestAPIPID(props.getProperty("REST_API_PID", DEF_RESTAPIPID));
			
			this.setAppovedLic(props.getProperty("APPROVED_LIC", DEF_APPOVEDLIC));
			
			this.setLicXlate(props.getProperty("LIC_XLATE", DEF_LICXLATE));
			
			this.setNoLicFix(props.getProperty("NOLICFIX", DEF_NOLICFIX));
			
			this.setLicTextInput(props.getProperty("LIC_TEXT", DEF_LICTXTI));
			
			this.setAuditRpt(props.getProperty("AUDIT_RPT", DEF_AUDITRPT));
			
			this.setLicList(props.getProperty("LICENSE_LIST", DEF_LICLIST));
			
			this.setLicText(props.getProperty("LICENSE_TEXT", DEF_LICTEXT));
			
			reader.close();
		} catch (FileNotFoundException ex) {
			// file does not exist
			this.logger.log("config file does not exist " + ex.getMessage());
			this.setDefaultValues();
			
		} catch (IOException ex) {
			// I/O error
			this.logger.log( "config file I/O error " + ex.getMessage());
		}
		
		this.logger.log("\r\nREST_API_LOC: " + this.restAPILoc);
		this.logger.log("\r\nREST_API_KEY: " + this.restAPIKey);			
		this.logger.log("\r\nREST_API_PID: " + this.restAPIPID);
		this.logger.log("\r\nAPPROVED_LIC: " + this.appovedLic);
		this.logger.log("\r\nLIC_XLATE: " + this.licXlate);			
		this.logger.log("\r\nNOLICFIX: " + this.noLicFix);
		this.logger.log("\r\nLIC_TEXT: " + this.licTextI);
		this.logger.log("\r\nAUDIT_RPT: " + this.auditRpt);
		this.logger.log("\r\nLICENSE_LIST: " + this.licList);			
		this.logger.log("\r\nLICENSE_TEXT: " + this.licText);	
	}
	
	private void setDefaultValues() {
		this.logger.log("Setting default values...");
		
		this.setRestAPILoc(DEF_RESTAPILOC);
		this.setRestAPIKey(DEF_RESTAPIKEY);
		this.setRestAPIPID(DEF_RESTAPIPID);
		this.setAppovedLic(DEF_APPOVEDLIC);
		this.setLicXlate(DEF_LICXLATE);
		this.setNoLicFix(DEF_NOLICFIX);
		this.setLicTextInput(DEF_LICTXTI);
		this.setAuditRpt(DEF_AUDITRPT);			
		this.setLicList(DEF_LICLIST);
		this.setLicText(DEF_LICTEXT);
	}
	
	private void doJOB() {
		try {		
			final ApiClient apiClient = new ApiClient(this.restAPILoc, this.restAPIKey, this.logger);
			final String result = apiClient.getDependencies(this.restAPIPID);			
			Processor processor = new Processor(this.logger, this.appovedLic, this.licXlate, this.noLicFix, this.licTextI, apiClient);
			processor.validateLibs(result);
			processor.generateOutputFiles(this.auditRpt, this.licList, this.licText);
			
		} catch (Exception ex) {
			this.logger.log( "doJOB error " + ex.getMessage());
		}
	}
	
	
}