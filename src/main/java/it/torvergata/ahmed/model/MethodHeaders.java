package it.torvergata.ahmed.model;

import org.jetbrains.annotations.NotNull;

public enum MethodHeaders {
    RELEASE,
    CLASS_NAME,
    METHOD_SIGNATURE,
    LINES_OF_CODE,
    STATEMENT_COUNT,
    NUMBER_OF_AUTHORS,
    ADDED_CHURN,
    REMOVED_CHURN,
    MAX_ADDED_CHURN,
    MAX_REMOVED_CHURN,
    CYCLOMATIC_COMPLEXITY,
    COGNITIVE_COMPLEXITY,
    PARAMETER_COUNT,
    AGE_RELATIVE_THIS_RELEASE,
    FAN_IN,
    FAN_OUT,
    HALSTEAD_EFFORT,
    COMMENT_DENSITY,
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


}
