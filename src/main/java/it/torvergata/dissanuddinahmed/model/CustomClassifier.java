package it.torvergata.dissanuddinahmed.model;

import weka.classifiers.Classifier;

public class CustomClassifier {
    private final Classifier classifier;
    private final String featureSelectionFilterName;
    private final String samplingFilterName;
    private final String classifierName;
    private final boolean isCostSensitive;

    public CustomClassifier(Classifier classifier, String classifierName, String featureSelectionFilterName,
                            String bestFirstDirection, String samplingFilterName, boolean isCostSensitive) {
        this.classifier = classifier;
        switch (samplingFilterName) {
            case "Resample" -> this.samplingFilterName = "OverSampling";
            case "SpreadSubsample" -> this.samplingFilterName = "UnderSampling";
            case "SMOTE" -> this.samplingFilterName = "SMOTE";
            default -> this.samplingFilterName = samplingFilterName;
        }
        if (featureSelectionFilterName.equals("BestFirst")) {
            this.featureSelectionFilterName = featureSelectionFilterName + "(" + bestFirstDirection + ")";
        } else {
            this.featureSelectionFilterName = featureSelectionFilterName;
        }
        this.isCostSensitive = isCostSensitive;
        this.classifierName = classifierName;
    }


    public Classifier getClassifier() {
        return classifier;
    }

    public String getClassifierName() {
        return classifierName;
    }

    public String getFeatureSelectionFilterName() {
        return featureSelectionFilterName;
    }

    public String getSamplingFilterName() {
        return samplingFilterName;
    }

    public boolean isCostSensitive() {
        return isCostSensitive;
    }
}