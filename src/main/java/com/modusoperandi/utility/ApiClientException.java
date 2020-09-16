/*
 * 
 */
package com.modusoperandi.utility;

import java.io.IOException;

public class ApiClientException extends IOException {

    public ApiClientException(String message) {
        super(message);
    }

    public ApiClientException(String message, Throwable cause) {
        super(message, cause);
    }

}