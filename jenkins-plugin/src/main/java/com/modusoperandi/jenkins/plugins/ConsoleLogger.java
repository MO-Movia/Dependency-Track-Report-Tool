package com.modusoperandi.jenkins.plugins;

import hudson.console.LineTransformationOutputStream;
import hudson.model.TaskListener;
import java.io.IOException;
import java.io.PrintStream;

public class ConsoleLogger extends LineTransformationOutputStream {

    private static final String PREFIX = "[" + TPALVPlugin.PLUGIN_NAME + "] ";
    private transient final PrintStream logger;

    public ConsoleLogger(TaskListener listener) {
        this.logger = listener.getLogger();
    }

    /**
     * Log messages to the builds console.
     * @param message The message to log
     */
    public void log(String message) {
        logger.println(PREFIX + message.replaceAll("\\n", "\n" + PREFIX));
    }

    /**
     * Changes each new line to append the prefix before logging
     */
    @Override
    public void eol(byte[] b, int len) throws IOException {
        logger.append(PREFIX);
        logger.write(b, 0, len);
    }
}