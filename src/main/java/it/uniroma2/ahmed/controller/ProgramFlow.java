package it.uniroma2.ahmed.controller;

import it.uniroma2.ahmed.logging.SeLogger;
import it.uniroma2.ahmed.utilities.WorkLoader;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class ProgramFlow {


    private ProgramFlow() {
    }

    public static void run(String path) {
        JSONObject targets = Objects.requireNonNull(WorkLoader.load(path));
        Iterator<String> keys = targets.keys();
        int threads = targets.length();
        Logger logger = SeLogger.getInstance().getLogger();
        String startMessage = "Starting executor... number of threads: " + threads;
        logger.info(startMessage);

        CountDownLatch latch = new CountDownLatch(threads);
        int count = 0;

        try (ExecutorService executorService = Executors.newFixedThreadPool(threads)) {
            while (keys.hasNext()) {
                String key = keys.next();
                String value = targets.getString(key);

                Pipeline pipeline = new Pipeline(count, latch, key, value);
                executorService.submit(pipeline);
                count++;
            }

            latch.await();
        } catch (InterruptedException e) {
            logger.severe(e.getMessage());
            Thread.currentThread().interrupt();
        }


    }
}
