package it.torvergata.dissanuddinahmed.controller;

import it.torvergata.dissanuddinahmed.logging.SeLogger;
import it.torvergata.dissanuddinahmed.model.Release;
import it.torvergata.dissanuddinahmed.utilities.Sink;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

public class Pipeline implements Runnable{

    private final String targetName;
    private final String targetUrl;
    private final Logger logger;
    private final CountDownLatch latch;
    private final String threadIdentity;

    public Pipeline(int threadId, CountDownLatch countDownLatch, @NotNull String projectName, String projectUrl){
        this.targetName = projectName.toUpperCase();
        this.targetUrl = projectUrl;
        this.logger = SeLogger.getInstance().getLogger();
        this.threadIdentity = "Thread: { Id: " + threadId + ", project: " + projectName  +" }<< ";
        this.latch = countDownLatch;
    }
    

    private void injectAndProcess() {
        String info = this.threadIdentity + "starting processing project:\n\tname=" + this.targetName +
                "\n\turlTarget=" + this.targetUrl;
        logger.info(info);

        long overallStart = System.nanoTime();

        String seconds = " seconds";
        try {
            long start;
            long end;

            // Start Injection Phase
            start = System.nanoTime();
            info = this.threadIdentity + "start injection phase";
            logger.info(info);

            JiraInjection jiraInjection = new JiraInjection(this.targetName);
            jiraInjection.injectReleases();
            end = System.nanoTime();
            info = this.threadIdentity + "start releases injection took: " + getTimeInSeconds(start, end) + seconds;
            logger.info(info);

            start = System.nanoTime();
            List<Release> releases = jiraInjection.getReleases();
            end = System.nanoTime();
            info = this.threadIdentity + "releases injection complete took: " + getTimeInSeconds(start, end) +
                    seconds;
            logger.info(info);

            // Commit Injection
            start = System.nanoTime();
            info = this.threadIdentity + "start commit injection";
            logger.info(info);

            GitInjection gitInjection = new GitInjection(this.targetName, this.targetUrl, releases);
            gitInjection.injectCommits();
            end = System.nanoTime();
            info = this.threadIdentity + "commits injection complete took: " + getTimeInSeconds(start, end) +
                    seconds;
            logger.info(info);

            start = System.nanoTime();
            jiraInjection.injectTickets();
            gitInjection.setTickets(jiraInjection.getFixedTickets());
            gitInjection.preprocessCommitsWithIssue();
            end = System.nanoTime();
            info = this.threadIdentity + "ticket injection and commit preprocessing took: " +
                    getTimeInSeconds(start, end) + seconds;
            logger.info(info);

            // Java Class Injection
            start = System.nanoTime();
            info = this.threadIdentity + "start java class injection";
            logger.info(info);

            gitInjection.preprocessJavaClasses();
            end = System.nanoTime();
            info = this.threadIdentity + "java class injection complete took: " + getTimeInSeconds(start, end)
                    + seconds;
            logger.info(info);

            gitInjection.closeRepo();

            // Preprocessing Project
            start = System.nanoTime();
            info = this.threadIdentity + "start preprocessing project";
            logger.info(info);

            PreprocessMetrics preprocessMetrics = new PreprocessMetrics(gitInjection);
            preprocessMetrics.startPreprocessing();
            storeCurrentData(jiraInjection, gitInjection);
            end = System.nanoTime();
            info = this.threadIdentity + "preprocessing project complete took: " + getTimeInSeconds(start, end) +
                    seconds;
            logger.info(info);

            info = this.threadIdentity + "injection phase complete";
            logger.info(info);

            // Dataset Generation
            start = System.nanoTime();
            preprocessMetrics.generateDataset(targetName);
            end = System.nanoTime();
            info = this.threadIdentity + "dataset generation complete took: " + getTimeInSeconds(start, end) +
                    seconds;
            logger.info(info);

            // Classification Phase
            start = System.nanoTime();
            info = this.threadIdentity + "start processing phase";
            logger.info(info);

            info = this.threadIdentity + "starting classification";
            logger.info(info);

            WekaProcessing wekaProcessing = new WekaProcessing(this.targetName,
                    jiraInjection.getReleases().size() / 2);
            wekaProcessing.classify();
            end = System.nanoTime();
            info = this.threadIdentity + "classification complete took: " + getTimeInSeconds(start, end) +
                    seconds;
            logger.info(info);

            // Sinking Results
            start = System.nanoTime();
            wekaProcessing.sinkResults();
            end = System.nanoTime();
            info = this.threadIdentity + "sink results complete took: " + getTimeInSeconds(start, end) +
                    seconds;
            logger.info(info);

        } catch (IOException | URISyntaxException | GitAPIException e) {
            logger.severe(e.getMessage());
        } finally {
            long overallEnd = System.nanoTime();
            info = this.threadIdentity + "total processing took: " + getTimeInSeconds(overallStart, overallEnd) +
                    seconds;
            logger.info(info);
        }
    }

    // Helper method to convert nanoseconds to seconds
    @Contract(pure = true)
    private @NotNull String getTimeInSeconds(long start, long end) {
        return String.format("%.2f", (end - start) / 1_000_000_000.0);
    }

    private void storeCurrentData(@NotNull JiraInjection jiraInjection, @NotNull GitInjection gitInjection) {

        Sink.serializeToJson(this.targetName, "Releases", new JSONObject(jiraInjection.getMapReleases()),
                Sink.FileExtension.JSON);
        Sink.serializeToJson(this.targetName, "Tickets",  new JSONObject(gitInjection.getMapTickets()),
                Sink.FileExtension.JSON);
        Sink.serializeToJson(this.targetName, "Commits", new JSONObject(gitInjection.getMapCommits()),
                Sink.FileExtension.JSON);
        Sink.serializeToJson(this.targetName, "Summary", new JSONObject(gitInjection.getMapSummary()),
                Sink.FileExtension.JSON);
    }


    @Override
    public void run() {
        long startTime = System.nanoTime();
        this.injectAndProcess();
        long endTime = System.nanoTime();
        String finalMessage = this.threadIdentity+ SeLogger.ELAPSED_TIME + ((endTime - startTime) / Math.pow(10, 9)) +
                SeLogger.SECONDS;
        logger.info(finalMessage);
        this.latch.countDown();
    }
}
