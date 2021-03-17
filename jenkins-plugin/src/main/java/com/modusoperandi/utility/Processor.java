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
import java.nio.file.Files;
import java.nio.file.Paths;
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
import com.modusoperandi.jenkins.plugins.ConsoleLogger;

public class Processor {

	private class LibAudit {
		protected String name;
		protected String description;
		protected String usedVersion;
		protected String latestVersion;
		protected String license;
		protected String inValidXlateLicense;
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
	private final String noLicFix;
	private final String licTextInput;
	private final ApiClient apiClient;

	private String auditReport = "";
	private String licenseList = "";
	private String licenseText = "";
	private String[] approvedLicenses = null;
	private HashMap<String,JsonObject> uniqueLicense = null;
	private HashMap<String,String> noLicFixMap = null;
	private HashMap<String,String> licXlateMap = null;
	private ArrayList<LibAudit> auditList = null;
	private ArrayList<String> apiLicenses = null;

    public Processor(final ConsoleLogger logger, final String appovedLic, final String licXlate, final String noLicFix, final String licTextInput, final ApiClient apiClient) {
        this.logger = logger;
		this.appovedLic = appovedLic;
		this.licXlate = licXlate;
		this.noLicFix = noLicFix;
		this.licTextInput = licTextInput;
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
			// I think what I was trying to say was that if you did a translation and you didn’t find the translated license in the no-lic-fix, I would 
			// like an indication that there was a translation, and would like both the original license name and the translated license name in the log 
			// file (being that neither is valid).			
			this.auditReport += libAudit.isCompliant ? OK_TEXT : ((libAudit.isCommercial ? "Note" : "Invalid") + " [" + libAudit.license + "]" + ((null != libAudit.inValidXlateLicense) ? " Invalid Translated License [" + libAudit.inValidXlateLicense + "]" : ""));
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
			// One of the licenses requires that we include GPL license text – even though the license that is asking for this invalidates many aspects of that license. 
			// Instead of hard-coding this into the movia-talon_license_text.txt, would like to have yet another input file that is just any text we put in there, 
			// and you will transfer that to the top of the movia-talon_license_text.txt file, and then append all the license text as you do now. 
			// This way, we can add other items we might need in there to meet other license requirements without having to ask for yet another code change. Maybe that input file could be called License_text_input.txt.
			if(this.licenseText.isEmpty()) {
				this.licenseText += NEW_LINE;
				this.licenseText += getLicenseTextInput();
				this.licenseText += NEW_LINE;
			}
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
			// To be clear, the no_lic_fix operation happens before we start checking for a valid license file.  Thus, it isn’t used in the 
			// valid license checking, but is used at the beginning of the process to find if the license text is missing, and if so, 
			// potentially get it from the no_lic_fix (Formally Known As white_list file), and then see if it is a valid license.  This 
			// prevents a potential mistake of mis-typing the license in the no_lic_fix file, or, placing in the correct license name, but 
			// that license name is still not approved for use (but easier to see the name that isn’t approved in the output audit file).
			// No external checks needed. If you find a license name that is not in the Valid License File (VLF), you check to see if there 
			// is potentially a translate name for it (in the Translate License File (TLF). If so, you translate it, and then again check 
			// to see if the translated license name is in the VLF. If it is, it is valid. If not, both the original license name and the 
			// translated license name goes into the log file as an invalid license..			
			if((null == licName) || (null != licName && 0 == licName.trim().length())) {
				compliant = this.checkWL(libName, license);
			} else {
				compliant = true;
			}
			
			if(compliant) {
				compliant = this.isApproved(license[0]);
				
				if(!compliant) {
					compliant = this.checkXLate(license[0], license);
					
					if(compliant) {
						compliant = this.isApproved(license[0]);
						
						if(!compliant) {
							libAudit.inValidXlateLicense = license[0];
						}
					}
				}
			}
			
			if(compliant) {
				String name = license[0].toLowerCase();
				final String COMMERCIAL = "commercial";
				final String NON = "non";

				if(name.contains(COMMERCIAL)) {
					 int iIndex = name.indexOf(COMMERCIAL) - 4;
					 // check if there is any "non" prefix with COMMERCIAL.
					 if(0 <= iIndex) {
						 libAudit.isCommercial = !name.substring(iIndex, iIndex + 3).equals(NON);
					 }
				}
				libAudit.license = license[0];
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
						// when you grab the license text from the various license files, and purge any leading and trailing spaces.
						approvedLicenses.add(line.trim());
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
	
	private boolean checkWL(final String libName, final String[] license) {		
		return checkLicenseText("checkWL", libName, license, this.noLicFixMap, this.noLicFix);
	}
	
	private boolean checkXLate(final String licName, final String[] license) {
		return checkLicenseText("checkXLate", licName, license, this.licXlateMap, this.licXlate);
	}
	
	private boolean checkLicenseText(final String funName, final String libORLicName, final String[] license, HashMap<String,String> map, final String fileName) {
		boolean found = false;
		String correctLName = null;
		
		try {
			// Check license noLicFix file, see if there is the library name. It will provide the appropriate license name if found, which 
			// will bring this back into compliance.
			// Check license translation file, see if the license text is in there, and then the appropriate license ID that will map to 
			// the appropriate license in Dependency-Track. If found, this will bring this back into compliance.			
			if (null == map) {
				map = new HashMap<String,String>();
				final BufferedReader reader = new BufferedReader(new FileReader(fileName));
				String line = reader.readLine();

				while (line != null) {
					// skip comment lines
					if (!line.startsWith(COMMENT_LINE)) {
						// since license names have commas in it!  use space-right chevron-space format as delimeter
						String[] vars = line.split(SEP_INPUT);
						int iLen = vars.length;
						// when you grab the license text from the various license files, and purge any leading and trailing spaces.
						final String licenseName = (0 < iLen) ? vars[0].trim() : "";
						final String correctLicName = (1 < iLen) ? vars[1].trim() : "";
						map.put(licenseName, correctLicName);
						
						if(libORLicName.equals(licenseName)) {
							correctLName = correctLicName;
						}
					}
					// read next line
					line = reader.readLine();
				}
				reader.close();
			} else {
				correctLName = map.get(libORLicName);
			}

			if (null != correctLName) {
				license[0] = correctLName; 
				// The Valid License column contains all the information we need about the license – 
				// either it is valid (it is in either the white list, approved list, or translate list), or it is not
				// Valid == Compliant ie "license info (valid or not)" column alone is enough.  Thus, I don’t see any reason for the compliant column
				found = true;
			}
		} catch (Exception ex) {
			this.logger.log( funName + " error for " + libORLicName + " : " + ex.getMessage());
		}
		
		return found;
	}
	
	private String getLicenseTextInput() {
		String content = "";

		try {
			content = new String(Files.readAllBytes(Paths.get(this.licTextInput)));
		} catch (Exception ex) {
			this.logger.log( "getLicenseTextInput error " + ex.getMessage());
		}

		return content;
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
			// Get the Parent of the given file.
			File parentFile = file.getParentFile(); 

			// check for parent file found
			if (null != parentFile) {
				parentFile.mkdirs();
			}
			file.createNewFile();
			this.logger.log( "Generated " + file.getCanonicalPath());

			FileOutputStream fileStream = new FileOutputStream(file, false);
			fileStream.write(content.getBytes(StandardCharsets.UTF_8));
			fileStream.close();
		} catch (Exception ex) {
			this.logger.log( "generateOutputFile error: " + fileName + " " + ex.getMessage());
		}
	}
}