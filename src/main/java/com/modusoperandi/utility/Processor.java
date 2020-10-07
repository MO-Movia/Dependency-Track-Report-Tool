/*
 *
 */
package com.modusoperandi.utility;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.json.JsonArray;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.io.StringReader;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Comparator;
import java.util.ArrayList;
import org.spdx.rdfparser.license.AnyLicenseInfo;
import org.spdx.rdfparser.license.LicenseInfoFactory;
import org.spdx.spdxspreadsheet.InvalidLicenseStringException;
import org.spdx.rdfparser.license.DisjunctiveLicenseSet;
import org.spdx.rdfparser.license.ConjunctiveLicenseSet;
import org.spdx.rdfparser.license.SpdxListedLicense;
import org.spdx.rdfparser.license.ExtractedLicenseInfo;
import org.spdx.rdfparser.license.WithExceptionOperator;

public class Processor {

	private class LibAudit {
		protected String name;
		protected String description;
		protected String usedVersion;
		protected String latestVersion;
		protected String license;
		protected boolean isLatestVersion;
		protected boolean isValidLicense;
		protected boolean isCompliant;
		protected boolean isCommercial;
		protected int vulnerabilities;
		
		public LibAudit() {
			isLatestVersion = false;
			isValidLicense = false;
			isCompliant = false;
			isCommercial = false;
		}
	}

	private class LibAuditSorter implements Comparator<LibAudit> 
	{ 
		// Would be good to have an ordering – 
		// libraries with vulnerabilities first, followed by libraries that are not at the latest released versions, followed by clean 
		// libraries.
		public int compare(LibAudit a, LibAudit b) 
		{
			int c = 0;

			c = (b.vulnerabilities - a.vulnerabilities);

			// The Valid License column contains all the information we need about the license – 
			// either it is valid (it is in either the white list, approved list, or translate list), or it is not
			// Valid == Compliant ie "license info (valid or not)" column alone is enough.  Thus, I don’t see any reason for the compliant column
			if (0 == c) {
				if (b.isCompliant != a.isCompliant) {
					c = b.isCompliant ? -1 : 1;
				}
			}
			
			if (0 == c) {
				if (b.isCommercial != a.isCommercial) {
					c = b.isCommercial ? -1 : 1;
				}
			}

			if (0 == c) {
				if (b.isLatestVersion != a.isLatestVersion) {
					c = b.isLatestVersion ? -1 : 1;
				}
			}

			return c;
		} 
	} 

	// since license names have commas in it! use TAB as delimeter
	private static final String SEP_DELIMITER = "\t";
	private static final String NEW_LINE = "\r\n";
	private static final String COMMENT_LINE = "## ";
	private static final String SEP_LINE = "------------------------------------------------------------------------\r\n";
	private static final String OK_TEXT = "Yes";
	private static final String SEP_INPUT = " -> ";
	private static final String XLS_HEADER = "\"sep=" + SEP_DELIMITER + "\"";

    private final ConsoleLogger logger;
	private final String appovedLic;
	private final String licXlate;
	private final String whiteList;
	private final ApiClient apiClient;

	private String auditReport = "";
	private String licenseList = "";
	private String licenseText = "";
	private String[] approvedLicenses = null;
	private HashMap<String,JsonObject> uniqueLicense = null;
	private HashMap<String,String> whiteListMap = null;
	private HashMap<String,String> licXlateMap = null;
	private ArrayList<LibAudit> auditList = null;
	private ArrayList<String> apiLicenses = null;

    public Processor(final ConsoleLogger logger, final String appovedLic, final String licXlate, final String whiteList, final ApiClient apiClient) {
        this.logger = logger;
		this.appovedLic = appovedLic;
		this.licXlate = licXlate;
		this.whiteList = whiteList;
		this.apiClient = apiClient;
    }

    protected void validateLibs(final String result) {
		try {
			final JsonArray jsonArray = this.getJsonArray(result);

			this.uniqueLicense = new HashMap<String,JsonObject>();
			this.auditList = new ArrayList<LibAudit>();
			
			this.prepareLicenseListHeader();
			
			for(Object o: jsonArray){
				if ( o instanceof JsonObject ) {
					this.prepareAuditReport((JsonObject)o);
				}
			}
			
			this.prepareSortedAuditReport();
		} catch (Exception ex) {
			this.logger.log( "validateLibs error " + ex.getMessage());
		}
	}
	
	private void prepareAuditReport(JsonObject jsonObj) {
		String libName = "";

		try {
			// For each library, list the library name, description, known vulnerabilities, version differences (latest versus what is 
			// being used), and license info (compliant or not). Output in csv (one library per line).  Would be good to have an ordering – 
			// libraries with vulnerabilities first, followed by libraries that are not at the latest released versions, followed by clean 
			// libraries.
			// Thinking we need something different for the versions. It is too easy to "miss" a version mismatch. So, thinking that I would like to re-order 
			// this and add a column:
			// Module, Description (hopefully will fill in at some point), Version used, Latest version, Not latest version Flag, Valid License, Vulnerabilities
			// Thinking that we keep the flag columns empty – unless there is an issue – so that they stand out.  Thus, everything is assumed positive, unless there is something there.
			// So, if latest version, the not latest version flag will be blank. If no vulnerabilities, leave blank. If valid license, leave blank.  So, if everything is good, there will be no flag entries.
			// We could fill in the empty spots with OK and None if it is better to have something in there, the text will still stand out:
			
			final LibAudit libAudit = new LibAudit();

			final JsonObject jsonLib = jsonObj.getJsonObject("component");
			
			// library name
			libName = jsonLib.getString("name");
			libAudit.name = libName;
			
			// description - (Right now dependency-track doesn't return description - https://github.com/DependencyTrack/dependency-track/issues/746)
			try {
				libAudit.description = jsonLib.getString("description");			
			} catch (NullPointerException ex) {
				this.logger.log( "auditReport description error for " + libName + " : " + ex.getMessage());
			}
			
			// used version
			final String libVersion = jsonLib.getString("version");
			libAudit.usedVersion = libVersion;
			
			// latest version
			String libLatestVersion = "";
			try {
				libLatestVersion = jsonLib.getJsonObject("repositoryMeta").getString("latestVersion");				
			} catch (NullPointerException ex) {
				this.logger.log( "auditReport latestVersion error for " + libName + " : " + ex.getMessage());
			}
			libAudit.latestVersion = libLatestVersion;
			
			// Not latest version Flag
			libAudit.isLatestVersion = libLatestVersion.equals(libVersion);

			// Valid License,
			JsonObject jsonLic = null;
			String licName = "";
			String licID = "";

			try {
				jsonLic = jsonLib.getJsonObject("resolvedLicense");		
			} catch (NullPointerException ex) {
				this.logger.log( "auditReport resolvedLicense error for " + libName + " : " + ex.getMessage());
			}
			
			boolean skipLicText = false;
			
			if(null != jsonLic) {
				try {
					licName = jsonLic.getString("name");				
				} catch (NullPointerException ex) {
					this.logger.log( "auditReport license name error for " + libName + " : " + ex.getMessage());
				}
				
				try {
					licID = jsonLic.getString("licenseId");		
				} catch (NullPointerException ex) {
					this.logger.log( "auditReport license ID error for " + libName + " : " + ex.getMessage());
				}
			} else {
				// It might be in one of these format:
				// (LicenseIDA OR LicenseIDB OR LicenseIDC)
				// (LicenseIDA AND LicenseIDB AND LicenseIDC)
				// (LicenseIDA WITH Exception)
				// Optionally be encapsulated by parentheses: "( )".
				// Order of Precedence and Parentheses -> +, WITH, AND, OR
				// https://spdx.github.io/spdx-spec/appendix-IV-SPDX-license-expressions/
				try {
					licName = jsonLib.getString("license");		
				} catch (NullPointerException ex) {
					this.logger.log( "auditReport license error for " + libName + " : " + ex.getMessage());
				}
				
				libAudit.isValidLicense = this.isValidLicenseEx(libAudit, libName, licName);
				licID = licName;
				skipLicText = true;
			}
			
			String apiLicName = "";
			if (!skipLicText) {
				if (!this.uniqueLicense.containsKey(licID)) {
					apiLicName = this.prepareLicenseText(licID);
				} else {
					apiLicName = this.uniqueLicense.get(licID).getString("name");
				}
				libAudit.isValidLicense = apiLicName.equals(licName);
			}
			libAudit.license = licName;

			// license info (compliant or not)
			final String[] license = {licName};// always use the compliant license name
			libAudit.isCompliant = this.isCompliant(libAudit, libName, licName, license);
			
			// known vulnerabilities
			libAudit.vulnerabilities = jsonObj.getJsonObject("metrics").getInt("vulnerabilities");

			this.auditList.add(libAudit);
			
			this.prepareLicenseList(libName, libVersion, license[0]);
		} catch (Exception ex) {
			this.logger.log( "auditReport error for " + libName + " : " + ex.getMessage());
		}
	}
	
	private boolean parseAnyLicenseInfo(final AnyLicenseInfo licenseInfo, final String libName) {
		String licID = null;
		boolean isValidLicense = true;
		
		try {
			if (licenseInfo instanceof DisjunctiveLicenseSet) {
				DisjunctiveLicenseSet dls = (DisjunctiveLicenseSet)licenseInfo;
				for (AnyLicenseInfo ali : dls.getMembers()) {
					isValidLicense &= parseAnyLicenseInfo(ali, libName);
				}
			} else if (licenseInfo instanceof ConjunctiveLicenseSet) {
				ConjunctiveLicenseSet cls = (ConjunctiveLicenseSet)licenseInfo;
				for (AnyLicenseInfo ali : cls.getMembers()) {
					isValidLicense &= parseAnyLicenseInfo(ali, libName);
				}
			} else if (licenseInfo instanceof SpdxListedLicense) {
				SpdxListedLicense sll = (SpdxListedLicense)licenseInfo;
				licID = sll.getLicenseId();
				isValidLicense &= isValidLicense(licID);
			} else if (licenseInfo instanceof ExtractedLicenseInfo) {
				ExtractedLicenseInfo eli = (ExtractedLicenseInfo)licenseInfo;
				licID = eli.getLicenseId();
				isValidLicense &= isValidLicense(licID);
			} else if (licenseInfo instanceof WithExceptionOperator) {
				WithExceptionOperator weo = (WithExceptionOperator)licenseInfo;
				licID = weo.getException().getLicenseExceptionId();
				isValidLicense &= isValidLicense(licID);
				licID = weo.getLicense().toString();
				isValidLicense &= isValidLicense(licID);
			}
		} catch (Exception ex) {
			this.logger.log( "parseAnyLicenseInfo error for " + libName + " : " + ex.getMessage());
		}
		
		return isValidLicense;
	}
	
	private boolean isValidLicense(String licID) {
		boolean isValid = true;
		
		if (!this.uniqueLicense.containsKey(licID)) {
			String apiLicName = this.prepareLicenseText(licID);
			isValid = !apiLicName.isEmpty();
		}
		
		return isValid;
	}
	
	private void prepareAuditReportHeader() {
		try {
			// Easy trick to open in Excel formatted.
			this.auditReport += XLS_HEADER;
			this.auditReport += NEW_LINE;
			
			// library name
			this.auditReport += "NAME";
			this.auditReport += SEP_DELIMITER;
			
			// description
			this.auditReport += "DESCRIPTION";
			this.auditReport += SEP_DELIMITER;
			
			// used version
			this.auditReport += "USED VERSION";
			this.auditReport += SEP_DELIMITER;
			
			// latest version
			this.auditReport += "LATEST VERSION";
			this.auditReport += SEP_DELIMITER;
			
			// Not latest version Flag
			this.auditReport += "HAS LATEST VERSION";
			this.auditReport += SEP_DELIMITER;

			// Valid License (compliant or not)
			this.auditReport += "VALID LICENSE";
			this.auditReport += SEP_DELIMITER;
			
			// known vulnerabilities
			this.auditReport += "VULNERABILITIES";
			this.auditReport += SEP_DELIMITER;

			this.auditReport += NEW_LINE;
		} catch (Exception ex) {
			this.logger.log( "prepareAuditReportHeader " + ex.getMessage());
		}	
	}
	
	private void prepareLicenseListHeader() {
		try {
			// Easy trick to open in Excel formatted.
			this.licenseList += XLS_HEADER;
			this.licenseList += NEW_LINE;
			
			this.licenseList += "LIBRARY";
			this.licenseList += SEP_DELIMITER;
			
			this.licenseList += "VERSION";
			this.licenseList += SEP_DELIMITER;
			
			this.licenseList += "LICENSE";
			this.licenseList += SEP_DELIMITER;

			this.licenseList += NEW_LINE;
		} catch (Exception ex) {
			this.logger.log( "prepareLicenseListHeader " + ex.getMessage());
		}	
	}
	
	private void prepareAuditReport(final LibAudit libAudit) {
		try {
			// library name
			this.auditReport += libAudit.name;
			this.auditReport += SEP_DELIMITER;
			
			// description
			this.auditReport += (null != libAudit.description) ? libAudit.description : "";
			this.auditReport += SEP_DELIMITER;
			
			// used version
			this.auditReport += libAudit.usedVersion;
			this.auditReport += SEP_DELIMITER;
			
			// latest version
			this.auditReport += libAudit.latestVersion;
			this.auditReport += SEP_DELIMITER;
			
			// Not latest version Flag
			this.auditReport += libAudit.isLatestVersion ? OK_TEXT : "Not Latest";
			this.auditReport += SEP_DELIMITER;

			// The Valid License column contains all the information we need about the license – 
			// either it is valid (it is in either the white list, approved list, or translate list), or it is not
			// Valid == Compliant ie "license info (valid or not)" column alone is enough.  Thus, I don’t see any reason for the compliant column
			// license info (compliant or not)
			// when found a "commercial" text in the license, it shall be listed as "Note [<commercial_license_name>]" in the audit report and if any input files have any 
			// "commercial" license specified, it shall be skipped for verifying the name with dependency track.
			this.auditReport += libAudit.isCompliant ? OK_TEXT : ((libAudit.isCommercial ? "Note" : "Invalid") + " [" + libAudit.license + "]");
			this.auditReport += SEP_DELIMITER;

			// known vulnerabilities
			this.auditReport += this.getVulnerabilitiesText(libAudit.vulnerabilities);
			this.auditReport += SEP_DELIMITER;

			this.auditReport += NEW_LINE;
		} catch (Exception ex) {
			this.logger.log( "prepareAuditReport " + ex.getMessage());
		}	
	}
	
	private void prepareSortedAuditReport() {
		try {
			this.prepareAuditReportHeader();
			this.auditList.sort(new LibAuditSorter());
			this.auditList.forEach((libAudit) -> this.prepareAuditReport(libAudit));
		} catch (Exception ex) {
			this.logger.log( "prepareSortedAuditReport " + ex.getMessage());
		}
	}
	
	private String getVulnerabilitiesText(final int vulnerabilities) {
		String text = "";
		switch(vulnerabilities) {
			case 0:
				text = "None";
				break;
			case 1:
				text = vulnerabilities + " Vulnerability";
				break;
			default:
				text = vulnerabilities + " Vulnerabilities";
				break;				
		}
		
		return text;
	}
	
	private void prepareLicenseList(final String libName, final String libVersion, final String licName) {
		try {
			// For each library, list the library, version being used, the license name for the library, new line, next library. You can 
			// make this csv since it is still readable so that it could be brought into excel..
			this.licenseList += libName;
			this.licenseList += SEP_DELIMITER;
			
			this.licenseList += libVersion;
			this.licenseList += SEP_DELIMITER;
			
			this.licenseList += licName;
			this.licenseList += SEP_DELIMITER;
			
			this.licenseList += NEW_LINE;
		} catch (Exception ex) {
			this.logger.log( "prepareLicenseList error for " + libName + " : " + ex.getMessage());
		}
	}
	
	private String prepareLicenseText(final String licenseID) {
		String licenseName = "";
		try {
			final JsonObject jsonObj = this.getJsonObject(this.apiClient.getLicense(licenseID));
			this.uniqueLicense.put(licenseID, jsonObj);
			licenseName = this.getLicenseText(jsonObj);
		} catch (Exception ex) {
			this.logger.log( "prepareLicenseText error for " + licenseID + " " + ex.getMessage());
		}
		
		return licenseName;
	}
	
	private String getLicenseText(final JsonObject jsonObj) {
		String licenseName = "";
		try {
			// List all unique license text for licenses listed in the first file – License name, new line, the license text, new line, new 
			// line, next license. Note that the text for the licenses that would be placed into the license file are also available via 
			// Dependency-Tracker REST calls.
			licenseName = jsonObj.getString("name");			
			this.licenseText += SEP_LINE;
			this.licenseText += licenseName;
			this.licenseText += NEW_LINE;
			this.licenseText += SEP_LINE;
			this.licenseText += jsonObj.getString("licenseText");
			this.licenseText += NEW_LINE;
			this.licenseText += NEW_LINE;
			this.licenseText += NEW_LINE;
		} catch (Exception ex) {
			this.logger.log( "prepareLicenseText error " + ex.getMessage());
		}
		
		return licenseName;
	}
	
	private JsonObject getJsonObject(String json) {
		JsonObject jsonObj = null;
		try {
			final JsonReader jsonReader = Json.createReader(new StringReader(json));
			jsonObj = jsonReader.readObject();
			jsonReader.close();
		} catch (Exception ex) {
			this.logger.log( "getJsonObject error " + ex.getMessage());
		}
		return jsonObj;
	}
	
	private JsonArray getJsonArray(String json) {
		JsonArray jsonArrr = null;
		try {
			final JsonReader jsonReader = Json.createReader(new StringReader(json));
			jsonArrr = jsonReader.readArray();
			jsonReader.close();
		} catch (Exception ex) {
			this.logger.log( "getJsonArray error " + ex.getMessage());
		}
		return jsonArrr;
	}
	
	private boolean isValidLicenseEx(final LibAudit libAudit, final String libName, final String licName) {
		boolean isValid = false;

		if (null != licName) {				
			try {
				// deal multiple license ID here.
				this.logger.log("BEGIN parsing " + licName);
				final AnyLicenseInfo licenseInfo = LicenseInfoFactory.parseSPDXLicenseString(licName);
				this.logger.log("END parsing " + licName);
				isValid = parseAnyLicenseInfo(licenseInfo, libName);
			} catch (InvalidLicenseStringException ex) {
				this.logger.log("auditReport license ID error for " + libName + " : " + ex.getMessage());
			}
		}

		return isValid;
	}
	
	protected void getLicenseList() {
		try {
			if(null == apiLicenses) {
				final JsonArray jsonArray = this.getJsonArray(this.apiClient.getLicenseList());
				
				this.apiLicenses = new ArrayList<String>();

				for(Object o: jsonArray){
					if ( o instanceof JsonObject ) {
						this.apiLicenses.add(((JsonObject)o).getString("name"));
					}
				}
			}
		} catch (Exception ex) {
			this.logger.log( "getLicenseList error " + ex.getMessage());
		}
	}
	
	protected boolean isLicenseNameValid(final String licName) {
		boolean isValid = false;
		
		try {
			getLicenseList();
			isValid = this.apiLicenses.contains(licName);			
		} catch (Exception ex) {
			this.logger.log( "isLicenseNameValid for " + licName + " error " + ex.getMessage());
		}
		
		if(!isValid) {
			this.logger.log(licName + " is NOT a valid license name");
		}
		
		return isValid;
	}

	private boolean isCompliant(final LibAudit libAudit, final String libName, final String licName, final String[] license) {
		boolean compliant = false;
		
		try {
			compliant = this.isApproved(licName);
		
			if(!compliant) {
				compliant = this.checkWL(libName, license);			
			} else {
				license[0] = licName;
			}
			
			if(!compliant) {
				compliant = this.checkXLate(licName, license);
			}
			
			if(compliant) {
				String name = license[0];
				libAudit.isCommercial = name.toLowerCase().contains("commercial");
				libAudit.license = license[0];
			}
			
			// I am curious. If I put in a white-list, and give you a license name for a module that is still not approved, what will happen?  
			// My desire would be that it still shows invalid license, and shows the invalid license from the white-list input file.
			if(compliant && !libAudit.isCommercial && !isLicenseNameValid(license[0])) {
				libAudit.license = license[0];
				compliant = false;
			}
		} catch (Exception ex) {
			this.logger.log( "isCompliant error " + ex.getMessage());
		}

		return compliant;
	}
	
	private boolean isApproved(String licName) {
		boolean approved = false;
		
		try {
			// Check approved license file, see if there is the license name to confirm compliance.
			if (null == this.approvedLicenses) {
				final Set<String> approvedLicenses = new HashSet<String>();
				final BufferedReader reader = new BufferedReader(new FileReader(this.appovedLic));
				String line = reader.readLine();

				while (line != null) {
					// skip comment lines
					if (!line.startsWith(COMMENT_LINE)) {
						approvedLicenses.add(line);
					}
					// read next line
					line = reader.readLine();
				}
				reader.close();
				
				this.approvedLicenses = approvedLicenses.toArray(new String[0]);
			}
			
			int iLen = this.approvedLicenses.length;
			
			for (int i = 0; i < iLen; i++) {
				if (licName.equals(this.approvedLicenses[i])) {
					approved = true;
					// break the loop.
					i = iLen;
				}
			}
		} catch (Exception ex) {
			this.logger.log("isApproved error for " + licName + " : " + ex.getMessage());
		}
		
		return approved;
	}
	
	private boolean checkWL(String libName, final String[] license) {
		boolean found = false;
		
		try {
			// Check license whitelist file, see if there is the library name. It will provide the appropriate license name if found, which 
			// will bring this back into compliance.
			if (null == this.whiteListMap) {
				this.whiteListMap = new HashMap<String,String>();
				final BufferedReader reader = new BufferedReader(new FileReader(this.whiteList));
				String line = reader.readLine();

				while (line != null) {
					// skip comment lines
					if (!line.startsWith(COMMENT_LINE)) {
						// since license names have commas in it!  use space-right chevron-space format as delimeter
						String[] vars = line.split(SEP_INPUT);
						int iLen = vars.length;
						final String libraryName = (0 < iLen) ? vars[0] : "";
						final String licenseName = (1 < iLen) ? vars[1] : "";
						this.whiteListMap.put(libraryName, licenseName);
					}
					// read next line
					line = reader.readLine();
				}
				reader.close();
			}
			
			String licName = this.whiteListMap.get(libName);
			if (null != licName) {			
				license[0] = licName; 
				// The Valid License column contains all the information we need about the license – 
				// either it is valid (it is in either the white list, approved list, or translate list), or it is not
				// Valid == Compliant ie "license info (valid or not)" column alone is enough.  Thus, I don’t see any reason for the compliant column
				found = true;
			}
		} catch (Exception ex) {
			this.logger.log( "checkWL error for " + libName + " : " + ex.getMessage());
		}
		
		return found;
	}
	
	private boolean checkXLate(String licName, final String[] license) {
		boolean found = false;
		
		try {
			// Check license translation file, see if the license text is in there, and then the appropriate license ID that will map to 
			// the appropriate license in Dependency-Track. If found, this will bring this back into compliance.
			if (null == this.licXlateMap) {
				this.licXlateMap = new HashMap<String,String>();
				final BufferedReader reader = new BufferedReader(new FileReader(this.licXlate));
				String line = reader.readLine();

				while (line != null) {
					// skip comment lines
					if (!line.startsWith(COMMENT_LINE)) {
						// since license names have commas in it!  use space-right chevron-space format as delimeter
						String[] vars = line.split(SEP_INPUT);
						int iLen = vars.length;
						final String licenseName = (0 < iLen) ? vars[0] : "";
						final String correctLicName = (1 < iLen) ? vars[1] : "";
						this.whiteListMap.put(licenseName, correctLicName);
					}
					// read next line
					line = reader.readLine();
				}
				reader.close();
			}

			String correctLicName = this.whiteListMap.get(licName);
			if (null != correctLicName) {
				license[0] = correctLicName; 
				// The Valid License column contains all the information we need about the license – 
				// either it is valid (it is in either the white list, approved list, or translate list), or it is not
				// Valid == Compliant ie "license info (valid or not)" column alone is enough.  Thus, I don’t see any reason for the compliant column
				found = true;
			}
		} catch (Exception ex) {
			this.logger.log( "checkXLate error for " + licName + " : " + ex.getMessage());
		}
		
		return found;
	}
	
	protected void generateOutputFiles(String auditRpt, String licList, String licText) {
		try {
			this.generateOutputFile(auditRpt, auditReport);
			this.generateOutputFile(licList, licenseList);
			this.generateOutputFile(licText, licenseText);
		} catch (Exception ex) {
			this.logger.log( "generateOutputFiles error " + ex.getMessage());
		}
	}
	
	protected void generateOutputFile(String fileName, String content) {
		try {
			File file = new File(fileName);
			file.getParentFile().mkdirs();
			file.createNewFile();
			FileOutputStream fileStream = new FileOutputStream(file, false);
			fileStream.write(content.getBytes(StandardCharsets.UTF_8));
			fileStream.close();
		} catch (Exception ex) {
			this.logger.log( "generateOutputFile error: " + fileName + " " + ex.getMessage());
		}
	}
}