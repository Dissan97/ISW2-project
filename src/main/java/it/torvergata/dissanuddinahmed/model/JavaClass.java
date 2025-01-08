package it.torvergata.dissanuddinahmed.model;

import java.util.ArrayList;
import java.util.List;

public class JavaClass {
    private final String name;
    private final String contentOfClass;
    private final Release release;
    private final Metrics metrics;
    private final List<Commit> classCommits;
    private final List<Integer> lOCAddedByClass;
    private final List<Integer> lOCRemovedByClass;

    public JavaClass(String name, String contentOfClass, Release release) {
        this.name = name;
        this.contentOfClass = contentOfClass;
        this.release = release;
        metrics = new Metrics();
        classCommits = new ArrayList<>();
        lOCAddedByClass = new ArrayList<>();
        lOCRemovedByClass = new ArrayList<>();
    }

    public List<Commit> getClassCommits() {
        return classCommits;
    }
    public void addCommitToClass(Commit commit) {
        this.classCommits.add(commit);
    }

    public Release getRelease() {
        return release;
    }

    public String getClassContent() {
        return contentOfClass;
    }

    public String getName() {
        return name;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public List<Integer> getLOCAddedByClass() {
        return lOCAddedByClass;
    }

    public void addLOCAddedByClass(Integer lOCAddedByEntry) {
        lOCAddedByClass.add(lOCAddedByEntry);
    }

    public List<Integer> getLOCRemovedByClass() {
        return lOCRemovedByClass;
    }

    public void addLOCRemovedByClass(Integer lOCRemovedByEntry) {
        lOCRemovedByClass.add(lOCRemovedByEntry);
    }

    @Override
    public String toString() {
        return "JavaClass{" +
                "name='" + name + '\'' +
                ", contentOfClass='" + contentOfClass + '\'' +
                ", release=" + release +
                ", metrics=" + metrics +
                ", commitsThatTouchTheClass=" + classCommits +
                ", lOCAddedByClass=" + lOCAddedByClass +
                ", lOCRemovedByClass=" + lOCRemovedByClass +
                '}';
    }
}
