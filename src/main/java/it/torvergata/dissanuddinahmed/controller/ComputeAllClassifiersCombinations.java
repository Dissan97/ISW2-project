package it.torvergata.dissanuddinahmed.controller;

import it.torvergata.dissanuddinahmed.model.CustomClassifier;
import org.jetbrains.annotations.NotNull;
import weka.attributeSelection.BestFirst;
import weka.classifiers.Classifier;
import weka.classifiers.CostMatrix;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.CostSensitiveClassifier;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.RandomForest;
import weka.core.AttributeStats;
import weka.core.SelectedTag;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SMOTE;
import weka.filters.supervised.instance.SpreadSubsample;

import java.util.ArrayList;
import java.util.List;

public class ComputeAllClassifiersCombinations {
    public static final String NO_SELECTION = "NoSelection";
    public static final String NO_SAMPLING = "NoSampling";
    public static final double WEIGHT_FALSE_POSITIVE = 1.0;
    public static final double WEIGHT_FALSE_NEGATIVE = 10.0;

    private ComputeAllClassifiersCombinations() {
    }

    public static List<CustomClassifier> returnAllClassifiersCombinations(AttributeStats isBuggyAttributeStats) {
        List<Classifier> classifierList = new ArrayList<>(List.of(new RandomForest(), new NaiveBayes(), new IBk()));
        List<AttributeSelection> featureSelectionFilters = getFeatureSelectionFilters();
        int majorityClassSize = isBuggyAttributeStats.nominalCounts[1];
        int minorityClassSize = isBuggyAttributeStats.nominalCounts[0];
        List<Filter> samplingFilters = getSamplingFilters(majorityClassSize, minorityClassSize);
        List<CustomClassifier> customClassifiersList = new ArrayList<>();
        //NO FEATURE SELECTION NO SAMPLING NO COST SENSITIVE
        basicClassifiers(classifierList, customClassifiersList);
        //ONLY FEATURE SELECTION
        onlyFeatureSelectionClassifiers(classifierList, featureSelectionFilters, customClassifiersList);
        //ONLY SAMPLING
        onlySamplingClassifiers(classifierList, samplingFilters, customClassifiersList);
        //ONLY COST SENSITIVE
        onlyCostSensitiveClassifiers(classifierList, customClassifiersList);
        //FEATURE SELECTION AND SAMPLING
        featureSelectionAndSamplingClassifiers(classifierList, featureSelectionFilters,
                samplingFilters, customClassifiersList);
        //FEATURE SELECTION AND COST SENSITIVE
        featureSelectionAndCostSensitiveClassifiers(classifierList, featureSelectionFilters, customClassifiersList);
        return customClassifiersList;
    }

    private static void basicClassifiers(List<Classifier> classifierList,
                                         List<CustomClassifier> customClassifiersList) {
        for (Classifier classifier : classifierList) {
            customClassifiersList.add(new CustomClassifier(classifier, classifier.getClass()
                    .getSimpleName(), NO_SELECTION, null, NO_SAMPLING, false));
        }
    }

    private static void featureSelectionAndCostSensitiveClassifiers(List<Classifier> classifierList,
                                                                    List<AttributeSelection> featureSelectionFilters,
                                                                    List<CustomClassifier> customClassifiersList) {
        for (Classifier classifier : classifierList) {
            List<CostSensitiveClassifier> costSensitiveFilters = getCostSensitiveFilters();
            for(CostSensitiveClassifier costSensitiveClassifier: costSensitiveFilters){
                for (AttributeSelection featureSelectionFilter : featureSelectionFilters) {
                    FilteredClassifier filteredClassifier = new FilteredClassifier();
                    filteredClassifier.setFilter(featureSelectionFilter);
                    costSensitiveClassifier.setClassifier(classifier);
                    filteredClassifier.setClassifier(costSensitiveClassifier);

                    customClassifiersList.add(new CustomClassifier(filteredClassifier, classifier.getClass()
                            .getSimpleName(), featureSelectionFilter.getSearch().getClass().getSimpleName(),
                            ((BestFirst)featureSelectionFilter.getSearch()).getDirection().getSelectedTag()
                                    .getReadable(), NO_SAMPLING, true));
                }
            }
        }
    }

    private static void featureSelectionAndSamplingClassifiers(List<Classifier> classifierList,
                                                               List<AttributeSelection> featureSelectionFilters,
                                                               List<Filter> samplingFilters,
                                                               List<CustomClassifier> customClassifiersList) {
        for (AttributeSelection featureSelectionFilter : featureSelectionFilters) {
            for (Filter samplingFilter : samplingFilters) {
                for (Classifier classifier : classifierList) {
                    FilteredClassifier innerClassifier = new FilteredClassifier();
                    innerClassifier.setClassifier(classifier);
                    innerClassifier.setFilter(samplingFilter);

                    FilteredClassifier externalClassifier = new FilteredClassifier();
                    externalClassifier.setFilter(featureSelectionFilter);
                    externalClassifier.setClassifier(innerClassifier);

                    customClassifiersList.add(new CustomClassifier(externalClassifier, classifier.getClass()
                            .getSimpleName(), featureSelectionFilter.getSearch().getClass().getSimpleName(),
                            ((BestFirst)featureSelectionFilter.getSearch()).getDirection().getSelectedTag()
                                    .getReadable(), samplingFilter.getClass().getSimpleName(), false));
                }
            }
        }
    }

    private static void onlySamplingClassifiers(List<Classifier> classifierList,
                                                List<Filter> samplingFilters,
                                                List<CustomClassifier> customClassifiersList) {
        for (Filter samplingFilter : samplingFilters) {
            for (Classifier classifier : classifierList) {
                FilteredClassifier filteredClassifier = new FilteredClassifier();
                filteredClassifier.setClassifier(classifier);
                filteredClassifier.setFilter(samplingFilter);

                customClassifiersList.add(new CustomClassifier(filteredClassifier, classifier.getClass()
                        .getSimpleName(),NO_SELECTION, null, samplingFilter.getClass()
                        .getSimpleName(), false));
            }
        }
    }

    private static void onlyCostSensitiveClassifiers(List<Classifier> classifierList,
                                                     List<CustomClassifier> customClassifiersList) {
        for (Classifier classifier : classifierList) {
            List<CostSensitiveClassifier> costSensitiveFilters = getCostSensitiveFilters();
            for (CostSensitiveClassifier costSensitiveClassifier : costSensitiveFilters) {
                costSensitiveClassifier.setClassifier(classifier);

                customClassifiersList.add(new CustomClassifier(costSensitiveClassifier, classifier.getClass()
                        .getSimpleName(),NO_SELECTION, null, NO_SAMPLING, true));
            }
        }
    }

    private static void onlyFeatureSelectionClassifiers(List<Classifier> classifierList,
                                                        List<AttributeSelection> featureSelectionFilters,
                                                        List<CustomClassifier> customClassifiersList) {
        for (AttributeSelection featureSelectionFilter : featureSelectionFilters) {
            for (Classifier classifier : classifierList) {
                FilteredClassifier filteredClassifier = new FilteredClassifier();
                filteredClassifier.setClassifier(classifier);
                filteredClassifier.setFilter(featureSelectionFilter);

                customClassifiersList.add(new CustomClassifier(filteredClassifier, classifier.getClass()
                        .getSimpleName(), featureSelectionFilter.getSearch().getClass().getSimpleName(),
                        ((BestFirst)featureSelectionFilter.getSearch()).getDirection().getSelectedTag()
                                .getReadable(), NO_SAMPLING, false));
            }
        }
    }

    private static List<Filter> getSamplingFilters(int majorityClassSize, int minorityClassSize) {
        double percentStandardOversampling = ((100.0*majorityClassSize)/(majorityClassSize + minorityClassSize))*2;
        double percentSMOTE;
        if(minorityClassSize==0 || minorityClassSize > majorityClassSize){
            percentSMOTE = 0;
        }else{
            percentSMOTE = (100.0*(majorityClassSize-minorityClassSize))/minorityClassSize;
        }
        return getFilters(percentStandardOversampling, percentSMOTE);
    }

    private static @NotNull List<Filter> getFilters(double percentStandardOversampling, double percentSMOTE) {
        List<Filter> filterList = new ArrayList<>();
        Resample resample = new Resample();
        resample.setBiasToUniformClass(1.0);
        resample.setSampleSizePercent(percentStandardOversampling);
        filterList.add(resample);
        SpreadSubsample spreadSubsample = new SpreadSubsample();
        spreadSubsample.setDistributionSpread(1.0);
        filterList.add(spreadSubsample);
        SMOTE smote = new SMOTE();
        smote.setClassValue("1");
        smote.setPercentage(percentSMOTE);
        filterList.add(smote);
        return filterList;
    }

    private static List<AttributeSelection> getFeatureSelectionFilters() {
        AttributeSelection attributeSelection = new AttributeSelection();
        BestFirst bestFirst = new BestFirst();
        bestFirst.setDirection(new SelectedTag(2, bestFirst.getDirection().getTags()));
        attributeSelection.setSearch(bestFirst);
        return new ArrayList<>(List.of(attributeSelection));
    }

    private static List<CostSensitiveClassifier> getCostSensitiveFilters() {
        CostSensitiveClassifier costSensitiveClassifier = new CostSensitiveClassifier();
        costSensitiveClassifier.setMinimizeExpectedCost(false);
        CostMatrix costMatrix = getCostMatrix();
        costSensitiveClassifier.setCostMatrix(costMatrix);
        return new ArrayList<>(List.of(costSensitiveClassifier));
    }

    private static CostMatrix getCostMatrix() {
        CostMatrix costMatrix = new CostMatrix(2);
        costMatrix.setCell(0, 0, 0.0);
        costMatrix.setCell(1, 0, WEIGHT_FALSE_POSITIVE);
        costMatrix.setCell(0, 1, WEIGHT_FALSE_NEGATIVE);
        costMatrix.setCell(1, 1, 0.0);
        return costMatrix;
    }
}
