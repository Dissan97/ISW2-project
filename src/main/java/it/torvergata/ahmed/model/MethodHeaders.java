package it.torvergata.ahmed.model;

import org.jetbrains.annotations.NotNull;

public enum MethodHeaders {
    RELEASE,
    CLASS_NAME,
    METHOD_SIGNATURE,
    LINES_OF_CODE,
    NUMBER_OF_CHANGES,
    AVG_CHURN,
    STATEMENT_COUNT,
    CYCLOMATIC_COMPLEXITY,
    COGNITIVE_COMPLEXITY,
    NESTING_DEPTH,
    PARAMETER_COUNT,
    NUM_OF_TESTS,
    AGE_RELATIVE_THIS_RELEASE,
    FAN_IN,
    FAN_OUT,
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
