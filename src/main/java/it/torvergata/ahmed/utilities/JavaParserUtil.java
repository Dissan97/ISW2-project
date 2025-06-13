package it.torvergata.ahmed.utilities;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.stmt.*;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class JavaParserUtil {


    public static final String SINGLE_LINE_COMMENTS = "//.*";
    /**
     * Multiline Comment regex
     */
    public static final String MULTILINE_COMMENTS = "(?s)/\\*.*?\\*/";

    private JavaParserUtil() {
        // util static method
    }

    private static final String EMPTY_BODY = "{}";

    @NotNull
    public static String getStringBody(@NotNull MethodDeclaration methodDeclaration) {
        return methodDeclaration.getBody().map(Object::toString).orElse(EMPTY_BODY);
    }

    public static int computeCyclomaticComplexity(MethodDeclaration method) {
        int complexity = 1; // parte da 1

        complexity += method.findAll(IfStmt.class).size();
        complexity += method.findAll(ForStmt.class).size();
        complexity += method.findAll(ForEachStmt.class).size();
        complexity += method.findAll(WhileStmt.class).size();
        complexity += method.findAll(DoStmt.class).size();
        complexity += method.findAll(CatchClause.class).size();
        complexity += method.findAll(SwitchEntry.class).size();

        complexity += (Math.toIntExact(method.findAll(BinaryExpr.class).stream()
                .filter(expr -> expr.getOperator() == BinaryExpr.Operator.AND
                        || expr.getOperator() == BinaryExpr.Operator.OR)
                .count()));

        return complexity;
    }

    public static int computeStatementCount(@NotNull MethodDeclaration method) {
        return method.getBody()
                .map(body -> body.getStatements().size())
                .orElse(0);
    }

    public static int computeEffectiveLOC(@NotNull MethodDeclaration method) {
        return method.getBody()
                .map(body -> {
                    String code = body.toString()
                            .replaceAll(MULTILINE_COMMENTS, "") // rimuove /* ... */
                            .replaceAll(SINGLE_LINE_COMMENTS, "");           // rimuove // ...
                    return Math.toIntExact(Arrays.stream(code.split("\n"))
                            .map(String::trim)
                            .filter(line -> !line.isEmpty() && !line.equals("{") && !line.equals("}"))
                            .count()
                    );
                })
                .orElse(0);
    }

    public static int computeParameterCount(@NotNull MethodDeclaration method) {
        return method.getParameters().size();
    }

    public static String getSignature(@NotNull MethodDeclaration method) {
        return method.getDeclarationAsString(); // puoi estendere per includere posizione se vuoi
    }

    public static int computeNestingDepth(MethodDeclaration method) {
        return computeNestingDepth(method.getBody().orElse(null), 0);
    }

    private static int computeNestingDepth(Node node, int currentDepth) {
        if (node == null) return currentDepth;

        int maxDepth = currentDepth;

        for (Node child : node.getChildNodes()) {
            int childDepth;
            if (isNestingStructure(child)) {
                childDepth = computeNestingDepth(child, currentDepth + 1);
            } else {
                childDepth = computeNestingDepth(child, currentDepth);
            }
            maxDepth = Math.max(maxDepth, childDepth);
        }

        return maxDepth;
    }

    private static boolean isNestingStructure(Node node) {
        return node instanceof IfStmt
                || node instanceof ForStmt
                || node instanceof ForEachStmt
                || node instanceof WhileStmt
                || node instanceof DoStmt
                || node instanceof SwitchStmt
                || node instanceof TryStmt
                || node instanceof CatchClause;
    }


    public static int calculateCognitiveComplexity(BlockStmt block) {
        return calculate(block, 0);
    }

    private static int calculate(Node node, int nesting) {
        AtomicInteger complexity = new AtomicInteger(0);

        for (Node child : node.getChildNodes()) {
            if (isNestingStructure(child)) {
                complexity.incrementAndGet();       // +1 base
                complexity.addAndGet(nesting);      // + nesting depth
                complexity.addAndGet(calculate(child, nesting + 1)); // ricorsione
            } else if (child instanceof BinaryExpr expr) {
                // Ogni &&/|| dopo il primo aumenta
                complexity.addAndGet(countLogicalOperators(expr));
                complexity.addAndGet(calculate(child, nesting)); // continua parsing
            } else {
                complexity.addAndGet(calculate(child, nesting));
            }
        }

        return complexity.get();
    }


    private static int countLogicalOperators(@NotNull BinaryExpr expr) {
        int count = 0;
        if (expr.getOperator() == BinaryExpr.Operator.AND ||
                expr.getOperator() == BinaryExpr.Operator.OR) {
            count++;
            if (expr.getLeft() instanceof BinaryExpr left)
                count += countLogicalOperators(left);
            if (expr.getRight() instanceof BinaryExpr right)
                count += countLogicalOperators(right);
        }
        return count;
    }

}
