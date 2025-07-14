package it.torvergata.ahmed;

import it.torvergata.ahmed.logging.SeLogger;
import it.torvergata.ahmed.model.JavaClass;
import it.torvergata.ahmed.model.MethodHeaders;
import it.torvergata.ahmed.model.Release;
import it.torvergata.ahmed.utilities.CodeSmellParser;

import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AnalyzeTheSmells {

    public static void main(String[] args) {
        try  (BufferedReader br = new BufferedReader(new FileReader("BookkeperAdmin.java"))){


            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
            JavaClass jc = new JavaClass("BookkeperAdmin.java" , sb.toString(), new Release(4, "custom",
                    LocalDate.now()), true);
            List<CodeSmellParser.JavaCsvInfo> list = new ArrayList<>();
            CodeSmellParser.extractCsvInfo("bk.csv", list, 3);
            list.forEach(
                    CodeSmellParser.findCorrectValue(jc)
            );
            StringBuilder builder = new StringBuilder();
            builder.append(MethodHeaders.getCsvHeaders()).append('\n');
            jc.getMethodsMetrics().forEach(
                    (methodSignature, methodMetric) -> builder.append(4).append(';')
                            .append(jc.getName()).append(';')
                            .append(methodSignature).append(';')
                            .append(methodMetric.getLinesOfCode()).append(';')
                            .append(methodMetric.getStatementCount()).append(';')
                            .append(methodMetric.getAuthorCount()).append(';')
                            .append(methodMetric.getAddedChurn()).append(';')
                            .append(methodMetric.getRemovedChurn()).append(';')
                            .append(methodMetric.getMaxAddedChurn()).append(';')
                            .append(methodMetric.getMaxRemovedChurn()).append(';')
                            .append(methodMetric.getCyclomaticComplexity()).append(';')
                            .append(methodMetric.getCognitiveComplexity()).append(';')
                            .append(methodMetric.getParameterCount()).append(';')
                            .append(methodMetric.getAge()).append(';')
                            .append(methodMetric.getFanIn()).append(';')
                            .append(methodMetric.getFanOut()).append(';')
                            .append(methodMetric.getHalsteadEffort()).append(';')
                            .append(methodMetric.getCommentDensity()).append(';')
                            .append(methodMetric.getNumberOfCodeSmells()).append(';')
                            .append(methodMetric.isBug()).append('\n')
            );
            String msg = builder.toString();
            SeLogger.getInstance().getLogger().info(msg);

        } catch (IOException e) {
            SeLogger.getInstance().getLogger().severe(e.getMessage());
        }
    }
}
