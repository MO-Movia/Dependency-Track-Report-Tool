/*
 *
 */
package com.modusoperandi.utility;

import java.io.IOException;
import java.io.PrintStream;
import java.io.OutputStream;

public class ConsoleLogger {

    private static final String PREFIX = "[TPALVU] ";
    private transient final PrintStream logger;

    public ConsoleLogger(PrintStream ps) {
        this.logger = (null != ps) ? ps : new PrintStream((OutputStream)(System.out));
    }

    /**
     * Log formatted messages to the builds console.
     * @param message The message to log
     */
    protected void log(String message) {
        this.logger.println(PREFIX + message.replaceAll("\\n", "\n" + PREFIX));
    }
	
	/**
     * Log messages (without any formatting) to the builds console.
     * @param message The message to log
     */
    protected void log0(String message) {
        this.logger.println(message);
    }
}