package it.uniroma2.ahmed.controller;

import it.uniroma2.ahmed.logging.SeLogger;
import it.uniroma2.ahmed.model.*;
import it.uniroma2.ahmed.utilities.Sink;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class PreprocessMetrics {

    private final GitInjection gitCtrl;

    public PreprocessMetrics(GitInjection gitController) {
        this.gitCtrl = gitController;
    }

    public void start() {
        this.computeSize();
        this.computeRevisionsNumber();
        this.computeFixNumber();
        this.computeAuthorsNumber();
        this.computeLOCMetrics();
    }

    private void computeSize() {
        this.gitCtrl.getJavaClasses().parallelStream().forEach(javaClass -> {
            String[] lines = javaClass.getClassBody().split("\r\n|\r|\n");
            javaClass.getMetrics().setSize(lines.length);
        });
    }

    private void computeRevisionsNumber() {
        for (JavaClass javaClass : this.gitCtrl.getJavaClasses()) {
            javaClass.getMetrics().setNumberOfRevisions(javaClass.getClassCommits().size());
        }
    }

    private void computeFixNumber() {
        int fixNumber;
        for (JavaClass javaClass : this.gitCtrl.getJavaClasses()) {
            fixNumber = 0;
            for (Commit commitThatTouchesTheClass : javaClass.getClassCommits()) {
                if (this.gitCtrl.getCommitsWithIssues().contains(commitThatTouchesTheClass)) {
                    fixNumber++;
                }
            }
            javaClass.getMetrics().setNumberOfDefectFixes(fixNumber);
        }
    }

    private void computeAuthorsNumber() {
        for (JavaClass javaClass : this.gitCtrl.getJavaClasses()) {
            List<String> authorsOfClass = new ArrayList<>();
            for (Commit commit : javaClass.getClassCommits()) {
                RevCommit revCommit = commit.getRevCommit();
                if (!authorsOfClass.contains(revCommit.getAuthorIdent().getName())) {
                    authorsOfClass.add(revCommit.getAuthorIdent().getName());
                }
            }
            javaClass.getMetrics().setNumberOfAuthors(authorsOfClass.size());
        }
    }

    private void computeLOCMetrics() {
        this.gitCtrl.getJavaClasses().parallelStream().forEach(javaClass -> {
            LOCMetrics addedLOC = new LOCMetrics();
            LOCMetrics removedLOC = new LOCMetrics();
            LOCMetrics churnLOC = new LOCMetrics();
            LOCMetrics touchedLOC = new LOCMetrics();

            // Check for LOC information for the current Java class
            this.gitCtrl.checkLOCInfo(javaClass);

            List<Integer> locAddedByClass = javaClass.getLOCAddedByClass();
            List<Integer> locRemovedByClass = javaClass.getLOCRemovedByClass();

            // Compute metrics in a single pass through the lists
            for (int i = 0; i < Math.max(locAddedByClass.size(), locRemovedByClass.size()); i++) {
                if (i < locAddedByClass.size()) {
                    addedLOC.updateMetrics(locAddedByClass.get(i));
                }
                if (i < locRemovedByClass.size()) {
                    removedLOC.updateMetrics(locRemovedByClass.get(i));
                }
                if (i < locAddedByClass.size() && i < locRemovedByClass.size()) {
                    int added = locAddedByClass.get(i);
                    int removed = locRemovedByClass.get(i);
                    int churn = Math.abs(added - removed);

                    churnLOC.updateMetrics(churn);
                    touchedLOC.updateMetrics(added + removed);
                } else if (i < locAddedByClass.size()) {
                    // If only additions exist, update touched metrics
                    touchedLOC.updateMetrics(locAddedByClass.get(i));
                } else if (i < locRemovedByClass.size()) {
                    // If only removals exist, update touched metrics
                    touchedLOC.updateMetrics(locRemovedByClass.get(i));
                }
            }

            // Set the computed metrics in the JavaClass
            setMetrics(removedLOC, churnLOC, addedLOC, touchedLOC, javaClass, locAddedByClass, locRemovedByClass);
        });
    }


    private void setMetrics(LOCMetrics removedLOC, LOCMetrics churnLOC, LOCMetrics addedLOC,
                            LOCMetrics touchedLOC, JavaClass javaClass,
                            List<Integer> locAddedByClass, List<Integer> locRemovedByClass) {
        int nRevisions = javaClass.getMetrics().getNumberOfRevisions();

        // Calculate averages only if there are additions or removals
        if (!locAddedByClass.isEmpty()) {
            addedLOC.setAvgVal((double) addedLOC.getVal() / nRevisions);
        }
        if (!locRemovedByClass.isEmpty()) {
            removedLOC.setAvgVal((double) removedLOC.getVal() / nRevisions);
        }
        if (!locAddedByClass.isEmpty() || !locRemovedByClass.isEmpty()) {
            churnLOC.setAvgVal((double) churnLOC.getVal() / nRevisions);
            touchedLOC.setAvgVal((double) touchedLOC.getVal() / nRevisions);
        }

        // Use setters in Metrics to update all LOC metrics in a single call
        Metrics metrics = javaClass.getMetrics();
        metrics.setAddedLOCMetrics(addedLOC.getVal(), addedLOC.getMaxVal(), addedLOC.getAvgVal());
        metrics.setRemovedLOCMetrics(removedLOC.getVal(), removedLOC.getMaxVal(), removedLOC.getAvgVal());
        metrics.setChurnMetrics(churnLOC.getVal(), churnLOC.getMaxVal(), churnLOC.getAvgVal());
        metrics.setTouchedLOCMetrics(touchedLOC.getVal(), touchedLOC.getMaxVal(), touchedLOC.getAvgVal());
    }


    public void generateDataset(String projectName) throws IOException {
        List<Release> releases = this.gitCtrl.getReleases();
        List<Ticket> tickets = this.gitCtrl.getTickets();
        List<JavaClass> classes = this.gitCtrl.getJavaClasses();
        int lastReleaseForDatasetId = releases.get((releases.size() / 2) - 1).getId();

        for (int i = 1; i <= lastReleaseForDatasetId; i++) {
            int finalI = i;

            List<Release> firstIReleases = releases.stream()
                    .filter(release -> release.getId() <= finalI)
                    .toList();
            try {
                int lastReleaseId = firstIReleases.getLast().getId();


                List<Ticket> firstITickets = tickets.stream()
                        .filter(ticket -> ticket.getFixedVersion().getId() <= lastReleaseId)
                        .toList();

                List<JavaClass> firstIProjectClassesTraining = classes.stream()
                        .filter(javaClass -> javaClass.getRelease().getId() <= lastReleaseId)
                        .toList();
                gitCtrl.fillClassesInfo(firstITickets, firstIProjectClassesTraining);
                final String filename = projectName + '_' + finalI;
                Sink.serializeInjectionToCsv(projectName, filename,
                        firstIReleases, firstIProjectClassesTraining, Sink.DataSetType.TRAINING);
                Sink.serializeInjectionToArff(projectName, filename,
                        firstIReleases, firstIProjectClassesTraining, Sink.DataSetType.TRAINING);


                List<Release> releasesForTestSet = new ArrayList<>();
                for (Release release : releases) {
                    if (release.getId() == (firstIReleases.getLast().getId() + 1)) {
                        releasesForTestSet.add(release);
                    }
                }

                List<JavaClass> firstIProjectClassesTesting = classes.stream()
                        .filter(javaClass -> javaClass.getRelease().getId() == releasesForTestSet.getLast(
                        ).getId())
                        .toList();

                Sink.serializeInjectionToCsv(projectName, filename,
                        releasesForTestSet, firstIProjectClassesTesting, Sink.DataSetType.TESTING);
                Sink.serializeInjectionToArff(projectName, filename,
                        releasesForTestSet, firstIProjectClassesTesting, Sink.DataSetType.TESTING);

            } catch (NoSuchElementException | NullPointerException e) {
                String severe = "generateDataset " + e.getMessage();
                SeLogger.getInstance().getLogger().severe(severe);
            }

        }

    }

}
