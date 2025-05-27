package it.uniroma2.ahmed.model;

import lombok.Getter;
import weka.classifiers.Classifier;

@Getter
public class CustomClassifier {
    private final Classifier classifier;
    private final String featureSelectionFilterName;
    private final String samplingFilterName;
    private final String classifierName;
    private final boolean costSensitive;

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
        this.costSensitive = isCostSensitive;
        this.classifierName = classifierName;
    }



}