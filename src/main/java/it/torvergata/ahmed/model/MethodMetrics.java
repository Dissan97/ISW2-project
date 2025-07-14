package it.torvergata.ahmed.model;


import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;


@Getter
public class MethodMetrics {

    /**
     * True if the method is known to be buggy (the classification label).
     */
    @Setter
    private boolean bug = false;

    /**
     * begin line in the class
     */
    @Setter
    private int beginLine;
    /**
     * end line in the class
     */
    @Setter
    private int endLine;

    @Setter
    private String simpleName;

    /**
     * Total churn: sum of all lines of code added and deleted in the method over time.
     */

    private int addedChurn = 0;
    private int removedChurn = 0;
    private int maxAddedChurn = 0;
    private int maxRemovedChurn = 0;


    /**
     * Number of physical (or non-commented) lines of code in the method.
     */

    private int linesOfCode;

    /**
     * Total number of executable statements within the method.
     */
    @Setter
    private int statementCount;

    /**
     * Cyclomatic complexity: the number of independent paths through the method logic.
     */
    @Setter
    private int cyclomaticComplexity;

    /**
     * Cognitive complexity: a measure of how challenging the method is to understand based on control flow and nesting.
     */
    @Setter
    private int cognitiveComplexity;

    /**
     * Maximum nesting depth of control structures (if, for, while, etc.) in the method.
     */
    @Setter
    private int nestingDepth;

    /**
     * Number of parameters the method accepts; high values may indicate high coupling or low cohesion.
     */
    @Setter
    private int parameterCount;

    /**
     * The Amount of code smells detected in the method using static analysis tools.
     */

    @Setter
    private double halsteadEffort;
    @Setter
    private double commentDensity;  // tra 0 e 1
    @Setter
    private int numberOfCodeSmells;
    @Setter
    private int age = 1;
    @Setter
    private int fanIn = 0;
    @Setter
    private int fanOut = 0;
    private String methodAccessor;
    private int numberOfChanges = 0;
    private final Set<String> authors = new HashSet<>();

    public void addChurn(int addedChurn, int removedChurn) {
        this.addedChurn += addedChurn;
        this.removedChurn += removedChurn;
        if (maxAddedChurn == 0 || addedChurn > maxAddedChurn) {
            maxAddedChurn = addedChurn;
        }
        if (maxRemovedChurn == 0 || removedChurn > maxRemovedChurn) {
            maxRemovedChurn = removedChurn;
        }
    }

    public void incChanges() {
        this.numberOfChanges += 1;
    }

    @Override
    public String toString() {
        return "MethodMetrics{" +
                "bug=" + bug +
                ", numberOfFix=" + numberOfChanges +
                ", churn=" + addedChurn +
                ", linesOfCode=" + linesOfCode +
                ", cyclomaticComplexity=" + cyclomaticComplexity +
                ", cognitiveComplexity=" + cognitiveComplexity +
                ", nestingDepth=" + nestingDepth +
                ", parameterCount=" + parameterCount +
                ", numberOfCodeSmells=" + numberOfCodeSmells +
                ", methodAccessor=" + methodAccessor +
                ", startLine=" + beginLine +
                ", endLine=" + endLine +
                '}';
    }


    private static final String PACKAGE_PRIVATE = "package-private";
    public void setMethodAccessor(@NotNull String accessor) {
        if (accessor.isEmpty()){
            this.methodAccessor = PACKAGE_PRIVATE;
            return;
        }
        this.methodAccessor = accessor;
    }
    public void setLinesOfCode(int linesOfCode) {
        this.linesOfCode = linesOfCode > 0 ?  linesOfCode : 1 ;
    }

    public void incCodeSmells() {
        this.numberOfCodeSmells++;
    }


    public void addAuthor(String name) {
        this.authors.add(name);
    }

    public int getAuthorCount() {
        return !this.authors.isEmpty() ? this.authors.size() : 1;
    }
}

