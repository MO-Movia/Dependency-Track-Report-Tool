/*
 * 
 */
package com.modusoperandi.utility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import com.modusoperandi.jenkins.plugins.ConsoleLogger;

public class ApiClient {

    private static final String API_KEY_HEADER = "X-Api-Key";
	private static final String PROJECT_DEPENDENCIES_URL = "/api/v1/dependency/project";
	private static final String LICENSE_URL = "/api/v1/license";

    private final String baseUrl;
    private final String apiKey;
    private final ConsoleLogger logger;

    public ApiClient(final String baseUrl, final String apiKey, final ConsoleLogger logger) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.logger = logger;
    }
	
	public String getDependencies(String projectUuid) throws ApiClientException {
		String optionalParams = "?sortOrder=asc&pageNumber=1&pageSize=" + getAPIResponseHeader(PROJECT_DEPENDENCIES_URL, projectUuid, "X-Total-Count");
		return getAPIResponse(PROJECT_DEPENDENCIES_URL, projectUuid + optionalParams);
    }

	public String getLicense(String licenseID) throws ApiClientException {
		return getAPIResponse(LICENSE_URL, licenseID);
    }
	
	public String getLicenseList() throws ApiClientException {
		return getAPIResponse(LICENSE_URL, "concise");
    }

	private String getAPIResponse(String apiName, String apiParam) throws ApiClientException {
		try {
            return getResponseBody(getConnection(apiName, apiParam).getInputStream());
        } catch (IOException e) {
            throw new ApiClientException("An error occurred while retrieving response for " + apiName + "/" + apiParam, e);
        }
    }

	private String getAPIResponseHeader(String apiName, String apiParam, String respHeader) throws ApiClientException {
		try {
            return getConnection(apiName, apiParam).getHeaderField(respHeader);
        } catch (IOException e) {
            throw new ApiClientException("An error occurred while retrieving response Header " + respHeader + " " + apiName + "/" + apiParam, e);
        }
    }

	private HttpURLConnection getConnection(String apiName, String apiParam) throws ApiClientException {
        try {
            final HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + apiName + "/" + apiParam).openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("GET");
            conn.setRequestProperty(API_KEY_HEADER, apiKey);
            conn.connect();
            // Checks the server response
            if (conn.getResponseCode() == 200) {
				return conn;
            } else {
                throw new ApiClientException("An error occurred while retrieving " + apiName + "/" + apiParam + " - HTTP response code: " + conn.getResponseCode() + " " + conn.getResponseMessage());
            }
        } catch (IOException e) {
            throw new ApiClientException("An error occurred while retrieving" + apiName + "/" + apiParam, e);
        }
    }

    private String getResponseBody(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        String line;
        while((line = reader.readLine()) != null) {
            result.append(line);
        }
        return result.toString();
    }
}