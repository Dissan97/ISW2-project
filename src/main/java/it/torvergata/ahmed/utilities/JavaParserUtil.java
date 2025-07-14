package it.torvergata.ahmed.utilities;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    public static @NotNull String normalizeMethod(String method) {
        if (method == null) {
            return "";
        }
        method = method.replaceAll(SINGLE_LINE_COMMENTS, "");
        method = method.replaceAll(MULTILINE_COMMENTS, "");
        method = method.replaceAll("\\s+", " ").trim();
        return method;
    }


    public static int computeStatementCount(@NotNull MethodDeclaration method) {
        return method.getBody()
                .map(body -> body.getStatements().size())
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


    public static double computeHalsteadEffort(MethodDeclaration md) {

        HalsteadCounter visitor = new HalsteadCounter();
        visitor.visit(md, null);
        int n1 = visitor.getUniqueOperators();
        int n2 = visitor.getUniqueOperands();
        int totalOperators = visitor.getTotalOperators();
        int totalOperands = visitor.getTotalOperands();

        if (n1 == 0 || n2 == 0) {
            return 0.0;
        }
        double vocabulary = (double) n1 + n2;
        double length     = (double) totalOperators + totalOperands;
        if (vocabulary <= 0.0 || length <= 0.0) {
            return 0.0;
        }

        double volume     = length * (Math.log(vocabulary) / Math.log(2));
        double difficulty = (n1 / 2.0) * (totalOperands / (double) n2);

        double effort = difficulty * volume;
        if (Double.isFinite(effort)) {
            return effort;
        } else {
            return 0.0;
        }
    }

    public static int computeLOC(MethodDeclaration methodDeclaration) {

        AtomicInteger ret = new AtomicInteger(0);
        methodDeclaration.getRange().ifPresent(
                range -> {
                    ret.set(range.end.line - range.begin.line + 1);
                    methodDeclaration.getBody().ifPresent(
                            body -> {
                                int totalLines = range.end.line - range.begin.line + 1;
                                int startLine  = range.begin.line;
                                String[] lines = getStringBody(methodDeclaration).split("\n");
                                Set<Integer> commentLines = new HashSet<>();
                                for (Comment comment : methodDeclaration.getAllContainedComments())
                                    comment.getRange().ifPresent(cRange -> {
                                        for (int l = cRange.begin.line; l <= cRange.end.line; l++) {
                                            commentLines.add(l);
                                        }
                                    });
                                int toSubtract = getToSubtract(lines, startLine, commentLines);

                                ret.set(totalLines - toSubtract);
                            }
                    );
                }
        );

        return ret.get();
    }

    private static int getToSubtract(String[] lines, int startLine, Set<Integer> commentLines) {
        int toSubtract = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            int absoluteLine = startLine + i;

            // righe vuote, solo parentesi, o commentate → sottrai
            if (line.isEmpty()
                    || line.equals("{")
                    || line.equals("}")
                    || commentLines.contains(absoluteLine)) {
                toSubtract++;
            }
        }
        return toSubtract;
    }


    // Visitor che conta operatori/operandi
    private static class HalsteadCounter extends VoidVisitorAdapter<Void> {
        private final Set<String> uniqueOps = new HashSet<>();
        private final Set<String> uniqueOprs = new HashSet<>();
        private int totalOps = 0;
        private int totalOprs = 0;

        @Override
        public void visit(BinaryExpr n, Void arg) {
            String op = n.getOperator().asString();
            uniqueOps.add(op);
            totalOps++;
            super.visit(n, arg);
        }
        @Override
        public void visit(UnaryExpr n, Void arg) {
            uniqueOps.add(n.getOperator().asString());
            totalOps++;
            super.visit(n, arg);
        }
        @Override
        public void visit(MethodCallExpr n, Void arg) {
            uniqueOps.add("call");
            totalOps++;
            // il nome del metodo come operando
            uniqueOprs.add(n.getNameAsString());
            totalOprs++;
            super.visit(n, arg);
        }
        @Override
        public void visit(NameExpr n, Void arg) {
            uniqueOprs.add(String.valueOf(n.getName()));
            totalOprs++;
            super.visit(n, arg);
        }


        int getUniqueOperators() { return uniqueOps.size(); }
        int getUniqueOperands()  { return uniqueOprs.size(); }
        int getTotalOperators()  { return totalOps; }
        int getTotalOperands()   { return totalOprs; }
    }

    public static double computeCommentDensity(MethodDeclaration md) {
        // 1. count totale righe del metodo
        int begin = md.getBegin().map(p -> p.line).orElse(0);
        int end   = md.getEnd  ().map(p -> p.line).orElse(begin);
        int totalLines = end - begin + 1;
        if (totalLines <= 0) return 0;

        // 2. conta commenti contenuti
        List<Comment> comments = md.getAllContainedComments();
        // ciascun comment ha un range di righe:
        int commentLines = 0;
        for (Comment c : comments) {
            int cb = c.getBegin().map(p -> p.line).orElse(0);
            int ce = c.getEnd  ().map(p -> p.line).orElse(cb);
            commentLines += (ce - cb + 1);
        }
        return commentLines / (double) totalLines;
    }
}
