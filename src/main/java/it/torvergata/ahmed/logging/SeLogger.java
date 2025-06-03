package it.torvergata.ahmed.logging;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class SeLogger {

    public static final String ELAPSED_TIME = "Elapsed Time: ";
    public static final String SECONDS = " seconds";
    private static SeLogger instance = null;
    private Logger logger = null;

    private SeLogger() {}

    public static synchronized SeLogger getInstance() {
        if (instance == null) {
            instance = new SeLogger();
        }
        return instance;
    }

    public Logger getLogger() {
        if (logger == null) {
            try(InputStream inputStream = getClass().getClassLoader().getResourceAsStream("logging.properties")){
                LogManager.getLogManager().readConfiguration(inputStream);
                this.logger = Logger.getLogger(SeLogger.class.getSimpleName());
            } catch (IOException ignored) {
                System.exit(-1);
            }
        }
        return this.logger;
    }
}
