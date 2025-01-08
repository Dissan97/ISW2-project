package it.torvergata.dissanuddinahmed;

import it.torvergata.dissanuddinahmed.controller.ProgramFlow;
import it.torvergata.dissanuddinahmed.logging.SeLogger;

import java.util.logging.Logger;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        Logger logger = SeLogger.getInstance().getLogger();
        if (args.length != 1) {
            logger.severe("Usage: Main <input-file>");
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