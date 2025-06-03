package it.torvergata.ahmed.model;

import lombok.Getter;
import lombok.Setter;

@Getter
public class Metrics {
    private final LOCMetrics removedLOCMetrics;
    private final LOCMetrics churnMetrics;
    private final LOCMetrics addedLOCMetrics;

    private final LOCMetrics touchedLOCMetrics;
    @Setter
    private boolean bug;
    @Setter
    private int size;
    @Setter
    private int numberOfRevisions;
    @Setter
    private int numberOfDefectFixes;
    @Setter
    private int numberOfAuthors;

    public Metrics() {
        bug = false;
        size = 0;
        numberOfRevisions = 0;
        numberOfDefectFixes = 0;
        numberOfAuthors = 0;
        removedLOCMetrics = new LOCMetrics();
        churnMetrics = new LOCMetrics();
        addedLOCMetrics = new LOCMetrics();
        touchedLOCMetrics = new LOCMetrics();
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

    @Override
    public String toString() {
        return "bug=" + bug;
    }
}
