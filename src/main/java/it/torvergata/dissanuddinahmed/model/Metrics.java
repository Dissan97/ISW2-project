package it.torvergata.dissanuddinahmed.model;

public class Metrics {
    private final LOCMetrics removedLOCMetrics;
    private final LOCMetrics churnMetrics;
    private final LOCMetrics addedLOCMetrics;

    private final LOCMetrics touchedLOCMetrics;
    private boolean bugged;
    private int size;
    private int numberOfRevisions;
    private int numberOfDefectFixes;
    private int numberOfAuthors;

    public Metrics() {
        bugged = false;
        size = 0;
        numberOfRevisions = 0;
        numberOfDefectFixes = 0;
        numberOfAuthors = 0;
        removedLOCMetrics = new LOCMetrics();
        churnMetrics = new LOCMetrics();
        addedLOCMetrics = new LOCMetrics();
        touchedLOCMetrics = new LOCMetrics();
    }

    public boolean getBuggyness() {
        return bugged;
    }

    public void setBuggyness(boolean bugged) {
        this.bugged = bugged;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }

    public LOCMetrics getRemovedLOCMetrics() {
        return removedLOCMetrics;
    }

    public LOCMetrics getChurnMetrics() {
        return churnMetrics;
    }

    public LOCMetrics getAddedLOCMetrics() {
        return addedLOCMetrics;
    }

    public LOCMetrics getTouchedLOCMetrics() {
        return touchedLOCMetrics;
    }

    public void setAddedLOCMetrics(int addedLOC, int maxAddedLOC, double avgAddedLOC) {
        this.addedLOCMetrics.setVal(addedLOC);
        this.addedLOCMetrics.setMaxVal(maxAddedLOC);
        this.addedLOCMetrics.setAvgVal(avgAddedLOC);
    }

    public void setRemovedLOCMetrics(int removedLOC, int maxRemovedLOC, double avgRemovedLOC) {
        this.removedLOCMetrics.setVal(removedLOC);
        this.removedLOCMetrics.setMaxVal(maxRemovedLOC);
        this.removedLOCMetrics.setAvgVal(avgRemovedLOC);
    }

    public void setChurnMetrics(int churn, int maxChurningFactor, double avgChurningFactor) {
        this.churnMetrics.setVal(churn);
        this.churnMetrics.setMaxVal(maxChurningFactor);
        this.churnMetrics.setAvgVal(avgChurningFactor);
    }

    public void setTouchedLOCMetrics(int touchedLOC, int maxTouchedLOC, double avgTouchedLOC) {
        this.touchedLOCMetrics.setVal(touchedLOC);
        this.touchedLOCMetrics.setMaxVal(maxTouchedLOC);
        this.touchedLOCMetrics.setAvgVal(avgTouchedLOC);

    }
    public void setNumberOfRevisions(int numberOfRevisions) {
        this.numberOfRevisions = numberOfRevisions;
    }


    public int getNumberOfRevisions() {
        return numberOfRevisions;
    }

    public void setNumberOfDefectFixes(int numberOfDefectFixes) {
        this.numberOfDefectFixes = numberOfDefectFixes;
    }

    public int getNumberOfDefectFixes() {
        return numberOfDefectFixes;
    }

    public void setNumberOfAuthors(int numberOfAuthors) {
        this.numberOfAuthors = numberOfAuthors;
    }

    public int getNumberOfAuthors() {
        return numberOfAuthors;
    }
}
