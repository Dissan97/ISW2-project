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
            String pmdCommand = pmdHome + File.separator + "bin" + File.separator + (isWindows() ? "pmd.bat" : "pmd");
            new ProcessBuilder(
                    pmdCommand,
                            "--",
                            "version"
            ).start().waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.exit(-1);
        } catch (IOException e) {
            String msg = "cannot find pmd in the system: " + e.getMessage();
            logger.severe(msg);
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

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}