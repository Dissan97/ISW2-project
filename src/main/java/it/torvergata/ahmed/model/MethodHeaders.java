package it.torvergata.ahmed.model;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public enum MethodHeaders {
    RELEASE,
    CLASS_NAME,
    METHOD_SIGNATURE,
    LINES_OF_CODE,
    NUMBER_OF_CHANGES,
    NUMBER_OF_AUTHORS,
    CHURN,
    STATEMENT_COUNT,
    CYCLOMATIC_COMPLEXITY,
    COGNITIVE_COMPLEXITY,
    NESTING_DEPTH,
    PARAMETER_COUNT,
    NUMBER_OF_CODE_SMELLS,
    BUG;


    public static @NotNull String getCsvHeaders(){
        StringBuilder sb = new StringBuilder();
        for(MethodHeaders mh : MethodHeaders.values()){
            sb.append(mh.toString()).append(";");
        }
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }

    public static @NotNull String getCsvValues(@NotNull String className,
                                               @NotNull Map<String, MethodMetrics> methodMetricsMap){
        StringBuilder sb = new StringBuilder();
        if (!methodMetricsMap.isEmpty()) {
            methodMetricsMap.forEach((key, metrics) -> {
                sb.append(className).append(",");
                sb.append(key).append(",");
                sb.append(metrics.getNumOfChanges()).append(",");
                sb.append(metrics.getNumOfAuthors()).append(",");
                sb.append(metrics.getChurn()).append(",");
                sb.append(metrics.getLinesOfCode()).append(",");
                sb.append(metrics.getStatementCount()).append(",");
                sb.append(metrics.getCyclomaticComplexity()).append(",");
                sb.append(metrics.getCognitiveComplexity()).append(",");
                sb.append(metrics.getCyclomaticComplexity()).append(",");
                sb.append(metrics.getParameterCount()).append(",");
                sb.append(metrics.getNumberOfCodeSmells()).append(",");
                sb.append(metrics.isBug()).append("\n");
            });
        }
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }
}
