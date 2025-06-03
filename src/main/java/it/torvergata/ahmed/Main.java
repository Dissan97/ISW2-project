package it.torvergata.ahmed;

import it.torvergata.ahmed.controller.ProgramFlow;
import it.torvergata.ahmed.logging.SeLogger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;


public class Main {

    public static final String SYS_PMD_HOME = "SYS_PMD_HOME";

    public static void main(String @NotNull [] args) {

        Logger logger = SeLogger.getInstance().getLogger();
        if (args.length != 1) {
            logger.severe("Usage: Main <input-file>");
            System.exit(-1);
        }
        String pmdHome = System.getenv(SYS_PMD_HOME);
        if (pmdHome == null || pmdHome.isEmpty()){
            logger.severe("Environment variable " + SYS_PMD_HOME + " not set");
            System.exit(-1);
        }

        try {
            new ProcessBuilder(
                    pmdHome + File.separator + "bin" + File.separator + "pmd",
                            "--",
                            "version"
            ).start().waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.exit(-1);
        } catch (IOException e) {
            logger.severe("cannot find pmd in the system");
            System.exit(-1);
        }


        logger.info("Start...");
        long startTime = System.nanoTime();
        ProgramFlow.run(args[0]);
        long endTime = System.nanoTime();

        String finalMessage = SeLogger.ELAPSED_TIME + ((endTime - startTime) / Math.pow(10, 9)) +
                SeLogger.SECONDS;
        logger.info(finalMessage);
    }

}