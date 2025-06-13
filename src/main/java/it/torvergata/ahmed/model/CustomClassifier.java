package it.torvergata.ahmed.model;

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
            case "Resample":
                this.samplingFilterName = "OverSampling";
                break;
            case "SpreadSubsample":
                this.samplingFilterName = "UnderSampling";
                break;
            case "SMOTE":
                this.samplingFilterName = "SMOTE";
                break;
            default:
                this.samplingFilterName = samplingFilterName;
                break;
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