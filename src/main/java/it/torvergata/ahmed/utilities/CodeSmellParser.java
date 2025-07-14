package it.torvergata.ahmed.utilities;

import it.torvergata.ahmed.model.JavaClass;
import it.torvergata.ahmed.model.Release;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static it.torvergata.ahmed.controller.GitInjection.PMD_ANALYSIS;

public class CodeSmellParser {

    private static final String CSV_SEPARATOR = ",";

    private CodeSmellParser() {
        throw new IllegalStateException("Utility class");
    }

    public static void extractCodeSmell(Map<Release, List<JavaClass>> javaClassPerRelease, String project) {

        javaClassPerRelease.forEach(
                (release, classes) -> {
                    if (release.getId() == 1){
                        CodeSmellParser
                                .parseCsvFile(PMD_ANALYSIS + File.separator + project
                                        + File.separator + 0 + ".csv", 0 ,javaClassPerRelease
                                );
                    }
                    CodeSmellParser
                    .parseCsvFile(PMD_ANALYSIS + File.separator + project
                            + File.separator + (release.getId()) + ".csv", release.getId(), javaClassPerRelease);
                }
        );

    }

    private static void  parseCsvFile(String csvFilePath, int release, Map<Release, List<JavaClass>> classesPerRelease) {

        List<JavaCsvInfo> javaCsvInfos = new ArrayList<>();
        extractCsvInfo(csvFilePath, javaCsvInfos, release);
        List<JavaClass> classes;
        Optional<Map.Entry<Release, List<JavaClass>>> optional = classesPerRelease.entrySet().stream().filter(
                releaseListEntry -> releaseListEntry.getKey().getId() == (release + 1)
        ).findFirst();
        if (optional.isEmpty()){
            return;
        }
        classes = optional.get().getValue();
        for (JavaClass jc : classes) {

            javaCsvInfos.stream().filter(
                    javaCsvInfo -> javaCsvInfo.getFilename().endsWith(jc.getName()+"\"")
                            && javaCsvInfo.getRelease() == release + 1
            ).forEach(
                    findCorrectValue(jc)
            );
        }
    }
    
    public static @NotNull Consumer<JavaCsvInfo> findCorrectValue(JavaClass jc) {

        return info -> jc.getMethodsMetrics().forEach(
                (key, value) -> {

                    if (info.getLine() >= value.getBeginLine()
                            && info.getLine() <= value.getEndLine()) {
                        value.setNumberOfCodeSmells(value.getNumberOfCodeSmells() + 1);
                    }
                }
        );
    }

    public static void extractCsvInfo(String csvFilePath, List<JavaCsvInfo> javaCsvInfos, int release) {
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFilePath))) {
            String row = reader.readLine();
            String[] columns = row.split(CSV_SEPARATOR);
            checkRowStructure(columns);
            while ((row = reader.readLine()) != null){
                javaCsvInfos.add(new JavaCsvInfo(row, release + 1));
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("File not found: " + csvFilePath + ": " + e.getMessage());
        }
    }

    private static void checkRowStructure(String @NotNull [] columns) {
        if (columns.length != CsvHeader.values().length){
            throw new IllegalArgumentException("bad csv file format passed");
        }
        for (CsvHeader header : CsvHeader.values()) {
            if ( !columns[header.ordinal()].equals(header.getValue())){
                throw new IllegalArgumentException("header not match: actual=" + columns[header.ordinal()] +
                        ", expected=" + header.getValue());
            }
        }
    }




    @Getter
    @Setter
    public static class JavaCsvInfo{
        private String problem;
        private String packagePath;
        private String filename;
        private String priority;
        private int line;
        private int release;

        public JavaCsvInfo(@NotNull String row, int release) {
            String[] values = row.split(CSV_SEPARATOR);
            problem = values[CsvHeader.PROBLEM.ordinal()];
            packagePath = values[CsvHeader.PACKAGE.ordinal()];
            filename = values[CsvHeader.FILE.ordinal()];
            priority = values[CsvHeader.PRIORITY.ordinal()];
            line = Integer.parseInt(values[CsvHeader.LINE.ordinal()].replace("\"", ""));
            this.release = release;
        }
    }

    @Getter
    private enum CsvHeader {
        PROBLEM("\"Problem\""),
        PACKAGE("\"Package\""),
        FILE("\"File\""),
        PRIORITY("\"Priority\""),
        LINE("\"Line\""),
        DESCRIPTION("\"Description\""),
        RULE_SET("\"Rule set\""),
        RULE("\"Rule\"");

        private final String value;

        CsvHeader(String value) {
            this.value = value;
        }

    }

}