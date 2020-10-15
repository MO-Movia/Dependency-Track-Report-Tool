package com.modusoperandi.jenkins.plugins;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.util.FormValidation;

import org.apache.commons.lang.StringUtils;

import org.kohsuke.stapler.QueryParameter;

import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

public class PluginUtil implements Serializable {

    private static final long serialVersionUID = 5780698407191725723L;
	
	private static final String ENV_VAR_BEGIN = "${";
	private static final String ENV_VAR_END = "}";

    private PluginUtil() { }

    /**
     * Performs input validation when submitting the global config
     * @param value The value of the URL as specified in the global config
     * @return a FormValidation object
     */
    static FormValidation doCheckUrl(@QueryParameter String value) {
        if (StringUtils.isBlank(value)) {
            return FormValidation.ok();
        }
        try {
            new URL(value);
        } catch (MalformedURLException e) {
            return FormValidation.error("The specified value is not a valid URL");
        }
        return FormValidation.ok();
    }

    /**
     * Performs input validation when submitting the global config
     * @param value The value of the path as specified in the global config
     * @return a FormValidation object
     */
    static FormValidation doCheckPath(@QueryParameter String value) {
        if (StringUtils.isBlank(value)) {
            return FormValidation.ok();
        }
        try {
            final FilePath filePath = new FilePath(new File(value));
            filePath.exists();
        } catch (Exception e) {
            return FormValidation.error("The specified value is not a valid path");
        }
        return FormValidation.ok();
    }

    static String parseBaseUrl(String baseUrl) {
        baseUrl = StringUtils.trimToNull(baseUrl);
        if (baseUrl != null && baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() -1);
        }
        return baseUrl;
    }
	
	static boolean parseFilePath(String path, EnvVars envVars, StringBuilder parsedPath) {
		boolean success = true;
		String newPath = path;
		String[] variables = StringUtils.substringsBetween(path, ENV_VAR_BEGIN, ENV_VAR_END);
		
		if(null != variables) {
			for (String variable : variables) {
				String rep = ENV_VAR_BEGIN + variable + ENV_VAR_END;
				String value = envVars.get(variable, rep);
				success &= !value.equals(rep);
				newPath = newPath.replace(rep, value);
			}
		}
		
		parsedPath.delete(0, parsedPath.length());
        parsedPath.append(newPath);

		return success;
	}
}