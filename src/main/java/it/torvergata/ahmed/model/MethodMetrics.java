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
     * Number of times the method has been modified across its version history.
     */
    private int numOfChanges = 0;
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

    /**
     * All authors email Names
     */
    private final Set<String> authors = new HashSet<>();

    /**
     * Total churn: sum of all lines of code added and deleted in the method over time.
     */
    private int churn = 0;




    /**
     * Number of physical (or non-commented) lines of code in the method.
     */
    @Setter
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
     * Cognitive complexity: a measure of how difficult the method is to understand based on control flow and nesting.
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
     * Number of code smells detected in the method using static analysis tools.
     */
    private int numberOfCodeSmells;
    @Setter
    private int numberOfReference;

    private String methodAccessor;
    private int numberOfFix = 0;

    public void addAuthor(String author) {
        authors.add(author);
    }
    public void addNumChanges() {
        this.numOfChanges++;
    }

    public void addChurn(int churn) {
        this.churn += churn;
    }

    public int getNumOfAuthors() {
        return this.authors.size();
    }

    public void incFix() {
        this.numberOfFix += 1;
    }

    @Override
    public String toString() {
        return "MethodMetrics{" +
                "bug=" + bug +
                ", numOfChanges=" + numOfChanges +
                ", authors=" + authors +
                ", numOfAuthors=" + authors.size() +
                ", churn=" + churn +
                ", linesOfCode=" + linesOfCode +
                ", statementCount=" + statementCount +
                ", cyclomaticComplexity=" + cyclomaticComplexity +
                ", cognitiveComplexity=" + cognitiveComplexity +
                ", nestingDepth=" + nestingDepth +
                ", parameterCount=" + parameterCount +
                ", numberOfCodeSmells=" + numberOfCodeSmells +
                ", numberOfReference=" + numberOfReference +
                ", methodAccessor=" + methodAccessor +
                '}';
    }

    public String asCsvString(){
        return String.valueOf(linesOfCode) + ',' +
                numOfChanges + ',' +
                authors.size() + ',' +
                churn + ',' +
                statementCount + ',' +
                cyclomaticComplexity + ',' +
                cognitiveComplexity + ',' +
                nestingDepth + ',' +
                parameterCount + ',' +
                numberOfCodeSmells + ',' +
                numberOfReference + ',' +
                methodAccessor + ',' +
                bug;

    }


    private static final String PACKAGE_PRIVATE = "package-private";
    public void setMethodAccessor(@NotNull String accessor) {
        if (accessor.isEmpty()){
            this.methodAccessor = PACKAGE_PRIVATE;
            return;
        }
        this.methodAccessor = accessor;
    }


    public void incCodeSmells() {
        this.numberOfCodeSmells++;
    }
}
