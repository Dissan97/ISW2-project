package it.torvergata.ahmed.utilities;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import it.torvergata.ahmed.controller.GitInjection;
import it.torvergata.ahmed.logging.SeLogger;
import it.torvergata.ahmed.model.ClassifierResult;
import it.torvergata.ahmed.model.JavaClass;
import it.torvergata.ahmed.model.MethodHeaders;
import it.torvergata.ahmed.model.Release;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Sink {

    public static final String DELIVERY_OUTPUT = "deliveryOutput";
    private static final String INJECTION_PATH = DELIVERY_OUTPUT + File.separator
            + "injection" + File.separator;
    private static final String DATASET_PATH = DELIVERY_OUTPUT + File.separator
            + "datasets" + File.separator;
    private static final String RESULT_PATH = DELIVERY_OUTPUT + File.separator
            + "results" + File.separator;
    private static final String METHOD = "method";

    private static final String CSV_HEADERS_INPUTS = "RELEASE_ID," +
            "FILE_NAME," +
            "SIZE," +
            "LOC_ADDED,LOC_ADDED_AVG,LOC_ADDED_MAX," +
            "LOC_REMOVED,LOC_REMOVED_AVG,LOC_REMOVED_MAX," +
            "LOC_TOUCHED,LOC_TOUCHED_AVG,LOC_TOUCHED_MAX," +
            "CHURN,CHURN_AVG,CHURN_MAX," +
            "NUMBER_OF_REVISIONS," +
            "NUMBER_OF_DEFECT_FIXES," +
            "NUMBER_OF_AUTHORS," +
            "IS_BUGGY\n";

    private static final String CSV_HEADER_RESULTS = "DATASET," +
            "#TRAINING_RELEASES," +
            "%TRAINING_INSTANCES," +
            "CLASSIFIER," +
            "FEATURE_SELECTION," +
            "BALANCING," +
            "COST_SENSITIVE," +
            "PRECISION," +
            "RECALL," +
            "AREA_UNDER_ROC," +
            "KAPPA," +
            "TRUE_POSITIVES," +
            "FALSE_POSITIVES," +
            "TRUE_NEGATIVES," +
            "FALSE_NEGATIVES\n";

    private static final String ARFF_RELATION = "@relation ";
    private static final String ARFF_ATTRIBUTE_AND_DATA = """
            @attribute SIZE numeric
            @attribute LOC_ADDED numeric
            @attribute LOC_ADDED_AVG numeric
            @attribute LOC_ADDED_MAX numeric
            @attribute LOC_REMOVED numeric
            @attribute LOC_REMOVED_AVG numeric
            @attribute LOC_REMOVED_MAX numeric
            @attribute CHURN numeric
            @attribute CHURN_AVG numeric
            @attribute CHURN_MAX numeric
            @attribute LOC_TOUCHED numeric
            @attribute LOC_TOUCHED_AVG numeric
            @attribute LOC_TOUCHED_MAX numeric
            @attribute NUMBER_OF_REVISIONS numeric
            @attribute NUMBER_OF_DEFECT_FIXES numeric
            @attribute NUMBER_OF_AUTHORS numeric
            @attribute IS_BUGGY {'YES', 'NO'}
            
            @data
            """;


    private Sink() {
    }

    private static int compareJsonMap(String o1, String o2) {
        try {
            int num1;
            int num2;
            if (o1.contains("-") && o2.contains("-")) {
                num1 = Integer.parseInt(o1.substring(o1
                        .lastIndexOf("-") + 1));
                num2 = Integer.parseInt(o2.substring(o2
                        .lastIndexOf("-") + 1));
            } else {
                num1 = Integer.parseInt(o1);
                num2 = Integer.parseInt(o2);
            }
            return Integer.compare(num1, num2);
        } catch (NumberFormatException e) {
            return o1.compareTo(o2);
        }
    }

    public enum FileExtension {
        JSON,
        ARFF, CSV
    }

    public enum DataSetType {
        TRAINING,
        TESTING
    }

    public static void serializeToJson(String projectName, String fileName, Object data,
                                       FileExtension fileExtension) {

        if (data instanceof JSONObject jsonObject) {
            Sink.sinkJson(projectName, fileName, jsonObject, fileExtension);

        }
    }
    public static void serializeProjectAsCsv(@NotNull GitInjection gitInjection) throws IOException {
        String dirPath = DATASET_PATH + File.separator + METHOD + File.separator;
        File directory = new File(dirPath);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Unable to create directory: " + dirPath);
        }


        try (BufferedWriter writer = new BufferedWriter(new FileWriter(
                dirPath + gitInjection.getProject().toLowerCase() + ".csv"))) {

            writer.write(MethodHeaders.getCsvHeaders() + "\n");

            gitInjection.getJavaClassPerRelease().keySet().stream().sorted(Comparator.comparing(Release::getId))
                    .forEach(
                    release -> gitInjection.getJavaClassPerRelease().get(release).forEach(
                            javaClass -> {
                                StringBuilder builder = new StringBuilder();
                                javaClass.getMethodsMetrics().forEach(
                                        (s, me) -> builder.append(release.getId()).append(';')
                                                .append(javaClass.getName()).append(';')
                                                .append(s).append(';')
                                                .append(me.getLinesOfCode()).append(';')
                                                .append(me.getNumberOfChanges()).append(';')
                                                .append(me.getAvgChurn()).append(';')
                                                .append(me.getStatementCount()).append(';')
                                                .append(me.getCyclomaticComplexity()).append(';')
                                                .append(me.getCognitiveComplexity()).append(';')
                                                .append(me.getNestingDepth()).append(';')
                                                .append(me.getParameterCount()).append(';')
                                                .append(me.getNumberOfTests()).append(';')
                                                .append(me.getAge()).append(';')
                                                .append(me.getFanIn()).append(';')
                                                .append(me.getFanOut()).append(';')
                                                .append(me.getNumberOfCodeSmells()).append(';')
                                                .append(me.isBug()).append('\n')
                                );
                                try {
                                    writer.write(builder.toString());
                                } catch (IOException ignored) {
                                    throw new IllegalStateException("Something went wrong while writing CSV file");
                                }
                            }
                    ));


        }
    }

    private static final String CLASSES = "classes";
    public static void serializeInjectionToCsv(String projectName, String filename,
                                               List<Release> releases, List<JavaClass> javaClasses,
                                               DataSetType dataSetType) {

        final String datasetPath = DATASET_PATH + CLASSES + File.separator + projectName + File.separator +
                FileExtension.CSV.name().toLowerCase(Locale.getDefault()) + File.separator
                + dataSetType.toString()
                .toLowerCase(Locale.getDefault());
        try {
            File file = getFile(filename, FileExtension.CSV, datasetPath);


            try (FileWriter fileWriter = new FileWriter(file)) {

                fileWriter.append(Sink.CSV_HEADERS_INPUTS);
                appendInjectionData(fileWriter, releases, javaClasses, false);

            }
        } catch (IOException e) {
            SeLogger.getInstance().getLogger().severe(e.getMessage());
        }


    }

    public static void serializeInjectionToArff(String projectName, String filename,
                                                List<Release> releases, List<JavaClass> javaClasses,
                                                @NotNull DataSetType dataSetType) {

        final String datasetPath = DATASET_PATH + CLASSES + File.separator + projectName + File.separator +
                FileExtension.ARFF.name().toLowerCase(Locale.getDefault()) + File.separator
                + dataSetType.toString()
                .toLowerCase(Locale.getDefault());
        try {
            File file = getFile(filename, FileExtension.ARFF, datasetPath);


            try (FileWriter fileWriter = new FileWriter(file)) {
                fileWriter.append(Sink.ARFF_RELATION).append(filename)
                        .append('.')
                        .append(FileExtension.ARFF.name().toLowerCase(Locale.getDefault()))
                        .append("\n\n")
                        .append(Sink.ARFF_ATTRIBUTE_AND_DATA);
                appendInjectionData(fileWriter, releases, javaClasses, true);
            }
        } catch (IOException e) {
            SeLogger.getInstance().getLogger().severe(e.getMessage());
        }


    }

    public static void serializeResultsToCsv(@NotNull String projectName, List<ClassifierResult> results) {

        final String resultsPath = Sink.RESULT_PATH + projectName + File.separator;
        final String filename = projectName.toLowerCase(Locale.getDefault()) + "_report";
        try {
            File file = getFile(filename, FileExtension.CSV, resultsPath);
            try (FileWriter fileWriter = new FileWriter(file)) {
                fileWriter.append(Sink.CSV_HEADER_RESULTS);
                appendResultsData(projectName, fileWriter, results);
            }
        } catch (IOException e) {
            SeLogger.getInstance().getLogger().severe(e.getMessage());
        }


    }

    private static void appendInjectionData(FileWriter fileWriter, List<Release> releases,
                                            List<JavaClass> javaClasses, boolean arffFile) throws IOException {
        for (Release release : releases) {
            for (JavaClass javaClass : javaClasses) {
                if (javaClass.getRelease().getId() == release.getId()) {
                    doAppendData(fileWriter, release, javaClass, arffFile);
                }
            }
        }
    }

    private static void doAppendData(FileWriter fileWriter, Release release, JavaClass javaClass,
                                     boolean isArff) throws IOException {
        String releaseID = Integer.toString(release.getId());
        String isClassBugged = javaClass.getMetrics().isBug() ? "YES" : "NO";
        String sizeOfClass = String.valueOf(javaClass.getMetrics().getSize());
        String addedLOC = String.valueOf(javaClass.getMetrics().getAddedLOCMetrics().getVal());
        String avgAddedLOC = String.valueOf(javaClass.getMetrics().getAddedLOCMetrics().getAvgVal());
        String maxAddedLOC = String.valueOf(javaClass.getMetrics().getAddedLOCMetrics().getMaxVal());
        String removedLOC = String.valueOf(javaClass.getMetrics().getRemovedLOCMetrics().getVal());
        String avgRemovedLOC = String.valueOf(javaClass.getMetrics().getRemovedLOCMetrics().getAvgVal());
        String maxRemovedLOC = String.valueOf(javaClass.getMetrics().getRemovedLOCMetrics().getMaxVal());
        String touchedLOC = String.valueOf(javaClass.getMetrics().getTouchedLOCMetrics().getVal());
        String avgTouchedLOC = String.valueOf(javaClass.getMetrics().getTouchedLOCMetrics().getAvgVal());
        String maxTouchedLOC = String.valueOf(javaClass.getMetrics().getTouchedLOCMetrics().getMaxVal());
        String churn = String.valueOf(javaClass.getMetrics().getChurnMetrics().getVal());
        String avgChurn = String.valueOf(javaClass.getMetrics().getChurnMetrics().getAvgVal());
        String maxChurn = String.valueOf(javaClass.getMetrics().getChurnMetrics().getMaxVal());
        String nRevisions = String.valueOf(javaClass.getMetrics().getNumberOfRevisions());
        String nDefectFixes = String.valueOf(javaClass.getMetrics().getNumberOfDefectFixes());
        String nAuthors = String.valueOf(javaClass.getMetrics().getNumberOfAuthors());
        String className = javaClass.getName();
        if (!isArff) {
            fileWriter.append(releaseID).append(",")
                    .append(className).append(",");
        }
        fileWriter.append(sizeOfClass).append(",")
                .append(addedLOC).append(",")
                .append(avgAddedLOC).append(",")
                .append(maxAddedLOC).append(",")
                .append(removedLOC).append(",")
                .append(avgRemovedLOC).append(",")
                .append(maxRemovedLOC).append(",")
                .append(touchedLOC).append(",")
                .append(avgTouchedLOC).append(",")
                .append(maxTouchedLOC).append(",")
                .append(churn).append(",")
                .append(avgChurn).append(",")
                .append(maxChurn).append(",")
                .append(nRevisions).append(",")
                .append(nDefectFixes).append(",")
                .append(nAuthors).append(",")
                .append(isClassBugged).append("\n");
    }

    private static void appendResultsData(String projectName, FileWriter fileWriter,
                                          List<ClassifierResult> results) throws IOException {
        for (ClassifierResult classifierResult : results) {
            fileWriter.append(projectName).append(",")
                    .append(String.valueOf(classifierResult.getWalkForwardIteration())).append(",")
                    .append(String.valueOf(classifierResult.getTrainingPercent())).append(",")
                    .append(classifierResult.getClassifierName()).append(",");
            if (classifierResult.isFeatureSelection()) {
                fileWriter.append(classifierResult.getCustomClassifier().getFeatureSelectionFilterName()).append(",");
            } else {
                fileWriter.append("None").append(",");
            }
            if (classifierResult.hasSampling()) {
                fileWriter.append(classifierResult.getCustomClassifier().getSamplingFilterName()).append(",");
            } else {
                fileWriter.append("None").append(",");
            }
            if (classifierResult.isCostSensitive()) {
                fileWriter.append("SensitiveLearning").append(",");
            } else {
                fileWriter.append("None").append(",");
            }
            fileWriter.append(String.valueOf(classifierResult.getPrecision())).append(",")
                    .append(String.valueOf(classifierResult.getRecall())).append(",")
                    .append(String.valueOf(classifierResult.getAreaUnderROC())).append(",")
                    .append(String.valueOf(classifierResult.getKappa())).append(",")
                    .append(String.valueOf(classifierResult.getTruePositives())).append(",")
                    .append(String.valueOf(classifierResult.getFalsePositives())).append(",")
                    .append(String.valueOf(classifierResult.getTrueNegatives())).append(",")
                    .append(String.valueOf(classifierResult.getFalseNegatives())).append("\n");
        }

    }


    private static void sinkJson(String projectName, String filename, JSONObject data, FileExtension fe) {

        final String projectPath = INJECTION_PATH + projectName;
        try {
            File file = getFile(filename, fe, projectPath);
            try (FileWriter fileWriter = new FileWriter(file)) {
                Map<String, Object> sorted = new TreeMap<>(Sink::compareJsonMap);
                sorted.putAll(data.toMap());
                ObjectMapper mapper = new ObjectMapper();
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
                mapper.writeValue(fileWriter, sorted);
            }
        } catch (IOException e) {
            final String severe = "sinkJson: " + e.getMessage();
            SeLogger.getInstance().getLogger().severe(severe);
        }


    }

    private static @NotNull File getFile(String filename, FileExtension fe, String path) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            boolean created = file.mkdirs();
            if (!created) {
                throw new IOException("Failed to create directories");
            }
        }
        file = new File(path + File.separator + filename + "." +
                fe.name().toLowerCase(Locale.getDefault()));
        return file;
    }

}