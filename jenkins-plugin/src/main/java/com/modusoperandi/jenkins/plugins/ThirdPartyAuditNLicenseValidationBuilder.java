package com.modusoperandi.jenkins.plugins;

import hudson.Launcher;
import hudson.Extension;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.DataBoundSetter;

import edu.umd.cs.findbugs.annotations.NonNull;

import org.jenkinsci.Symbol;

import org.apache.commons.lang.StringUtils;

import jenkins.tasks.SimpleBuildStep;

import javax.servlet.ServletException;

import java.io.IOException;
import java.util.ArrayList;

import com.modusoperandi.utility.ThirdPartyAuditNLicenseValidation;

public class ThirdPartyAuditNLicenseValidationBuilder extends Builder implements SimpleBuildStep {

    /**
	 * Dependency-Track API URL
	 */
	private String restAPILoc = DescriptorImpl.defaultRestAPILoc;
	
	/**
	 * Dependency-Track API KEY
	 */
	private String restAPIKey = DescriptorImpl.defaultRestAPIKey;
	
	private String restAPIPID = DescriptorImpl.defaultRestAPIPID;

	private String licXlate = DescriptorImpl.defaultLicXlate;

	private String appovedLic = DescriptorImpl.defaultAppovedLic;

	private String whiteList = DescriptorImpl.defaultWhiteList;

	private String auditRpt = DescriptorImpl.defaultAuditRpt;

	private String licList = DescriptorImpl.defaultLicList;

	private String licText = DescriptorImpl.defaultLicText;

    @DataBoundConstructor
    public ThirdPartyAuditNLicenseValidationBuilder (final String restAPILoc, final String restAPIKey, final String restAPIPID, 
											final String appovedLic, final String licXlate, final String whiteList, 
											final String auditRpt, final String licList, final String licText) {
        this.restAPILoc = restAPILoc;
        this.restAPIKey = restAPIKey;
        this.restAPIPID = restAPIPID;
        this.appovedLic = appovedLic;
        this.licXlate = licXlate;
        this.whiteList = whiteList;
        this.auditRpt = auditRpt;
        this.licList = licList;
        this.licText = licText;
    }
	
	@DataBoundSetter
    public void setRestAPILoc(@NonNull String restAPILoc) {
        this.restAPILoc = restAPILoc;
    }

    @NonNull
    public String getRestAPILoc() {
        return restAPILoc;
    }

    @DataBoundSetter
    public void setRestAPIKey(@NonNull String restAPIKey) {
        this.restAPIKey = restAPIKey;
    }
    
    @NonNull	
    public String getRestAPIKey() {
        return restAPIKey;
    }

    @DataBoundSetter
    public void setRestAPIPID(@NonNull String restAPIPID) {
        this.restAPIPID = restAPIPID;
    }
    
    @NonNull
    public String getRestAPIPID() {
        return restAPIPID;
    }

    @DataBoundSetter
    public void setAppovedLic(@NonNull String appovedLic) {
        this.appovedLic = appovedLic;
    }
    
    @NonNull
    public String getAppovedLic() {
        return appovedLic;
    }

    @DataBoundSetter
    public void setLicXlate(@NonNull String licXlate) {
        this.licXlate = licXlate;
    }
    
    @NonNull
    public String getLicXlate() {
        return licXlate;
    }

    @DataBoundSetter
    public void setWhiteList(@NonNull String whiteList) {
        this.whiteList = whiteList;
    }
    
    @NonNull
    public String getWhiteList() {
        return whiteList;
    }

    @DataBoundSetter
    public void setAuditRpt(@NonNull String auditRpt) {
        this.auditRpt = auditRpt;
    }
    
    @NonNull
    public String getAuditRpt() {
        return auditRpt;
    }

    @DataBoundSetter
    public void setLicList(@NonNull String licList) {
        this.licList = licList;
    }
    
    @NonNull
    public String getLicList() {
        return licList;
    }

    @DataBoundSetter
    public void setLicText(@NonNull String licText) {
        this.licText = licText;
    }
    
    @NonNull
    public String getLicText() {
        return licText;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
		ConsoleLogger cLogger = new ConsoleLogger(listener);
		ThirdPartyAuditNLicenseValidation tpalvu = new ThirdPartyAuditNLicenseValidation(cLogger);
		
		// Parse and process config
		EnvVars envVars = null;
		StringBuilder parsedPath = new StringBuilder("");
		String parsedAppovedLic = appovedLic;
		String parsedLicXlate = licXlate;
		String parsedWhiteList = whiteList;
		String parsedAuditRpt = auditRpt;
		String parsedLicList = licList;
		String parsedLicText = licText;
		
		envVars = run.getEnvironment(listener);
		
        if (null != envVars) {
				
			if(PluginUtil.parseFilePath(appovedLic, envVars, parsedPath)) {
				parsedAppovedLic = parsedPath.toString();
				cLogger.log("ParsedAppovedLicPath: " + parsedAppovedLic);
			}
			if(PluginUtil.parseFilePath(licXlate, envVars, parsedPath)) {
				parsedLicXlate = parsedPath.toString();
				cLogger.log("ParsedLicXlatePath: " + parsedLicXlate);
			}
			if(PluginUtil.parseFilePath(whiteList, envVars, parsedPath)) {
				parsedWhiteList = parsedPath.toString();
				cLogger.log("ParsedWhiteListPath: " + parsedWhiteList);
			}
			if(PluginUtil.parseFilePath(auditRpt, envVars, parsedPath)) {
				parsedAuditRpt = parsedPath.toString();
				cLogger.log("ParsedAuditRptPath: " + parsedAuditRpt);
			}
			if(PluginUtil.parseFilePath(licList, envVars, parsedPath)) {
				parsedLicList = parsedPath.toString();
				cLogger.log("ParsedLicListPath: " + parsedLicList);
			}
			if(PluginUtil.parseFilePath(licText, envVars, parsedPath)) {
				parsedLicText = parsedPath.toString();
				cLogger.log("ParsedLicTextPath: " + parsedLicText);
			}
        }
		
		if(null != tpalvu) {
			tpalvu.initialize(restAPILoc, restAPIKey, restAPIPID, parsedAppovedLic, parsedLicXlate, parsedWhiteList, parsedAuditRpt, parsedLicList, parsedLicText);
		}
    }

    @Symbol("tpalv")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		
		public static final String defaultRestAPILoc = "http://localhost:8080";
		public static final String defaultRestAPIKey = "LPfV2H90mbapj6TWLUV6tgu1PXYThFDi";
		public static final String defaultRestAPIPID = "588d64a8-a208-4d5f-b3f0-1288acd5ee5a";
		public static final String defaultAppovedLic = "${JENKINS_HOME}\\plugins\\modusoperandi-tpalv\\inputs\\movia_approved_licenses.txt";
		public static final String defaultLicXlate = "${JENKINS_HOME}\\plugins\\modusoperandi-tpalv\\inputs\\movia_lic_xlate_list.txt";
		public static final String defaultWhiteList = "${JENKINS_HOME}\\plugins\\modusoperandi-tpalv\\inputs\\movia_white_list.txt";
		public static final String defaultAuditRpt = "${WORKSPACE}\\outputs\\movia_audit_out.csv";
		public static final String defaultLicList = "${WORKSPACE}\\outputs\\movia_license_list.csv";
		public static final String defaultLicText = "${WORKSPACE}\\outputs\\movia_license_text.txt";

        public FormValidation doCheckRestAPILoc(@QueryParameter String value)
                throws IOException, ServletException {
            return PluginUtil.doCheckUrl(value);
        }
		
		public FormValidation doCheckAppovedLic(@QueryParameter String value)
                throws IOException, ServletException {
            return PluginUtil.doCheckPath(value);
        }
		
		public FormValidation doCheckLicXlate(@QueryParameter String value)
                throws IOException, ServletException {
            return PluginUtil.doCheckPath(value);
        }
		
		public FormValidation doCheckWhiteList(@QueryParameter String value)
                throws IOException, ServletException {
            return PluginUtil.doCheckPath(value);
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.ThirdPartyAuditNLicenseValidationBuilder_DescriptorImpl_DisplayName();
        }
    }
}
