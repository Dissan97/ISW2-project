package it.torvergata.ahmed;

import it.torvergata.ahmed.controller.ProgramFlow;
import it.torvergata.ahmed.logging.SeLogger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import static it.torvergata.ahmed.controller.GitInjection.SYS_CUT_PERCENTAGE;


public class Main {

    public static final String SYS_PMD_HOME = "SYS_PMD_HOME";
    private static final double DEFAULT_PERCENTAGE = 0.34;



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
        checkPercentage();
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


        logger.info("____________________________START____________________________");
        long startTime = System.nanoTime();
        ProgramFlow.run(args[0]);
        long endTime = System.nanoTime();

        String finalMessage = SeLogger.ELAPSED_TIME + ((endTime - startTime) / Math.pow(10, 9)) +
                SeLogger.SECONDS;
        logger.info(finalMessage);
    }

    private static void checkPercentage() {
        String cut = System.getenv(SYS_CUT_PERCENTAGE);
        try {
            double aDouble = Double.parseDouble(cut);
            String msg = "Checking percentage: " + aDouble;
            SeLogger.getInstance().getLogger().info(msg);
            System.setProperty(SYS_CUT_PERCENTAGE, cut);
        } catch (NumberFormatException | NullPointerException e) {
            String exceptionMsg = SYS_CUT_PERCENTAGE  + " exception: " + e.getClass().getSimpleName() + " ";
            exceptionMsg += e instanceof NumberFormatException ? " " + e.getMessage() : "env variable not setup";
            String warning = exceptionMsg +  " Invalid percentage: " + cut;
            SeLogger.getInstance().getLogger().warning(warning);
            System.setProperty(SYS_CUT_PERCENTAGE, "" + DEFAULT_PERCENTAGE);
            cut = System.getProperty(SYS_CUT_PERCENTAGE);
            double aDouble = Double.parseDouble(cut);
            warning = "now is setup to: " + aDouble;
            SeLogger.getInstance().getLogger().warning(warning);
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}