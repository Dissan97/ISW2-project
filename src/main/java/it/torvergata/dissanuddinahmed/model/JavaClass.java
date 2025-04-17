package it.torvergata.dissanuddinahmed.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class JavaClass {
    private final String name;
    private final String classBody;
    private final Release release;
    private final Metrics metrics;

    private final List<Commit> classCommits;
    private final List<Integer> lOCAddedByClass;
    private final List<Integer> lOCRemovedByClass;

    public JavaClass(String name, String classBody, Release release) {
        this.name = name;
        this.classBody = classBody;
        this.release = release;
        metrics = new Metrics();
        classCommits = new ArrayList<>();
        lOCAddedByClass = new ArrayList<>();
        lOCRemovedByClass = new ArrayList<>();
    }

    public void addCommitToClass(Commit commit) {
        this.classCommits.add(commit);
    }

    public List<Integer> getLOCAddedByClass() {
        return lOCAddedByClass;
    }

    public void addLOCAddedByClass(Integer lOCAddedByEntry) {
        lOCAddedByClass.add(lOCAddedByEntry);
    }

    public void addLOCRemovedByClass(Integer lOCRemovedByEntry) {
        lOCRemovedByClass.add(lOCRemovedByEntry);
    }



    @Override
    public String toString() {
        return "JavaClass{" +
                "name='" + name + '\'' +
                ", contentOfClass='" + classBody + '\'' +
                ", release=" + release +
                ", metrics=" + metrics +
                ", commitsThatTouchTheClass=" + classCommits +
                ", lOCAddedByClass=" + lOCAddedByClass +
                ", lOCRemovedByClass=" + lOCRemovedByClass +
                '}';
    }
}
