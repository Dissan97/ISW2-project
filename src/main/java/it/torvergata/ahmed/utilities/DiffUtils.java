package it.torvergata.ahmed.utilities;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import it.torvergata.ahmed.model.JavaClass;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class DiffUtils {

    private DiffUtils(){
        throw new IllegalStateException("Utility class");
    }

    public static void analyzeDiff(@NotNull JavaClass javaClass, @NotNull MethodDeclaration methodDeclaration) {
        String methodName = methodDeclaration.getDeclarationAsString();
        if (javaClass.getMethods().get(methodName) != null) {
            BlockStmt oldBody = javaClass.getMethods().get(methodName);

            methodDeclaration.getBody().ifPresent(
                    newBody -> {
                        if (oldBody != null){
                            addedAndRemovedCalculation(javaClass, newBody, oldBody, methodName);
                        }

                        if (!newBody.equals(javaClass.getMethods().get(methodName))) {
                            javaClass.getMethodsMetrics().get(methodName).incFix();

                        }
                    }
            );
        }
    }

    private static void addedAndRemovedCalculation(@NotNull JavaClass javaClass, @NotNull BlockStmt newBody,
                                                   @NotNull BlockStmt oldBody,
                                                   String methodName) {
        List<String> oldLines = Arrays.asList(oldBody.toString()
                .split("\n"));
        List<String> newLines = Arrays.asList(newBody.toString()
                .split("\n"));

        int added = 0;
        int removed = 0;
        for (String line : newLines) {
            if (!oldLines.contains(line)) added++;
        }
        for (String line : oldLines) {
            if (!newLines.contains(line)) removed++;
        }

        javaClass.getMethodsMetrics().get(methodName).addChurn(added, removed);
    }
}
