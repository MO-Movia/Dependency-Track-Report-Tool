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
    public void setRestAPILoc(String restAPILoc) {
        this.restAPILoc = restAPILoc;
    }

    public String getRestAPILoc() {
        return restAPILoc;
    }

    @DataBoundSetter
    public void setRestAPIKey(String restAPIKey) {
        this.restAPIKey = restAPIKey;
    }
    
    public String getRestAPIKey() {
        return restAPIKey;
    }

    @DataBoundSetter
    public void setRestAPIPID(String restAPIPID) {
        this.restAPIPID = restAPIPID;
    }
    
    public String getRestAPIPID() {
        return restAPIPID;
    }

    @DataBoundSetter
    public void setAppovedLic(String appovedLic) {
        this.appovedLic = appovedLic;
    }
    
    public String getAppovedLic() {
        return appovedLic;
    }

    @DataBoundSetter
    public void setLicXlate(String licXlate) {
        this.licXlate = licXlate;
    }
    
    public String getLicXlate() {
        return licXlate;
    }

    @DataBoundSetter
    public void setWhiteList(String whiteList) {
        this.whiteList = whiteList;
    }
    
    public String getWhiteList() {
        return whiteList;
    }

    @DataBoundSetter
    public void setAuditRpt(String auditRpt) {
        this.auditRpt = auditRpt;
    }
    
    public String getAuditRpt() {
        return auditRpt;
    }

    @DataBoundSetter
    public void setLicList(String licList) {
        this.licList = licList;
    }
    
    public String getLicList() {
        return licList;
    }

    @DataBoundSetter
    public void setLicText(String licText) {
        this.licText = licText;
    }
    
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

    @Symbol("greet")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

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
