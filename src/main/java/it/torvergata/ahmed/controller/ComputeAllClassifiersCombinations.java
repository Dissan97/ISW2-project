package it.torvergata.ahmed.controller;

import it.torvergata.ahmed.model.CustomClassifier;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
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

/**
 * Utility class for generating all meaningful combinations of classifiers
 * with optional sampling, feature selection, and cost-sensitive learning.
 */
public class ComputeAllClassifiersCombinations {
    public static final String NO_SELECTION = "NoSelection";
    public static final String NO_SAMPLING = "NoSampling";
    public static final double WEIGHT_FALSE_POSITIVE = 1.0;
    public static final double WEIGHT_FALSE_NEGATIVE = 10.0;

    private ComputeAllClassifiersCombinations() {}

    /**
     * Generate a list of all classifier configurations based on input class statistics.
     *
     * @param isBuggyAttributeStats AttributeStats for the target class
     * @return list of all {@link CustomClassifier} configurations
     */
    public static @NotNull List<CustomClassifier> returnAllClassifiersCombinations(@NotNull AttributeStats isBuggyAttributeStats) {
        List<Classifier> classifiers = List.of(new RandomForest(), new NaiveBayes(), new IBk());
        List<AttributeSelection> featureSelections = getFeatureSelectionFilters();
        int majority = isBuggyAttributeStats.nominalCounts[1];
        int minority = isBuggyAttributeStats.nominalCounts[0];
        List<Filter> samplings = getSamplingFilters(majority, minority);

        List<CustomClassifier> result = new ArrayList<>();
        basicClassifiers(classifiers, result);
        onlyFeatureSelectionClassifiers(classifiers, featureSelections, result);
        onlySamplingClassifiers(classifiers, samplings, result);
        onlyCostSensitiveClassifiers(classifiers, result);
        featureSelectionAndSamplingClassifiers(classifiers, featureSelections, samplings, result);
        featureSelectionAndCostSensitiveClassifiers(classifiers, featureSelections, result);
        return result;
    }

    /**
     * Add basic classifiers with no sampling, feature selection, or cost-sensitive configuration.
     */
    private static void basicClassifiers(@NotNull List<Classifier> classifiers, List<CustomClassifier> result) {
        for (Classifier c : classifiers) {
            result.add(new CustomClassifier(c, c.getClass().getSimpleName(), NO_SELECTION, null, NO_SAMPLING, false));
        }
    }

    /**
     * Add classifiers with feature selection filters only.
     */
    private static void onlyFeatureSelectionClassifiers(List<Classifier> classifiers, List<AttributeSelection> filters, List<CustomClassifier> result) {
        for (AttributeSelection filter : filters) {
            for (Classifier c : classifiers) {
                FilteredClassifier fc = new FilteredClassifier();
                fc.setFilter(filter);
                fc.setClassifier(c);
                result.add(new CustomClassifier(fc, c.getClass().getSimpleName(), filter.getSearch().getClass().getSimpleName(), ((BestFirst)filter.getSearch()).getDirection().getSelectedTag().getReadable(), NO_SAMPLING, false));
            }
        }
    }

    /**
     * Add classifiers with sampling filters only.
     */
    private static void onlySamplingClassifiers(List<Classifier> classifiers, List<Filter> filters, List<CustomClassifier> result) {
        for (Filter f : filters) {
            for (Classifier c : classifiers) {
                FilteredClassifier fc = new FilteredClassifier();
                fc.setFilter(f);
                fc.setClassifier(c);
                result.add(new CustomClassifier(fc, c.getClass().getSimpleName(), NO_SELECTION, null, f.getClass().getSimpleName(), false));
            }
        }
    }

    /**
     * Add classifiers using cost-sensitive configuration only.
     */
    private static void onlyCostSensitiveClassifiers(List<Classifier> classifiers, List<CustomClassifier> result) {
        for (Classifier c : classifiers) {
            for (CostSensitiveClassifier cost : getCostSensitiveFilters()) {
                cost.setClassifier(c);
                result.add(new CustomClassifier(cost, c.getClass().getSimpleName(), NO_SELECTION, null, NO_SAMPLING, true));
            }
        }
    }

    /**
     * Add classifiers using both feature selection and sampling filters.
     */
    private static void featureSelectionAndSamplingClassifiers(List<Classifier> classifiers, List<AttributeSelection> featureSelections, List<Filter> samplings, List<CustomClassifier> result) {
        for (AttributeSelection feature : featureSelections) {
            for (Filter sampling : samplings) {
                for (Classifier c : classifiers) {
                    FilteredClassifier inner = new FilteredClassifier();
                    inner.setFilter(sampling);
                    inner.setClassifier(c);

                    FilteredClassifier outer = new FilteredClassifier();
                    outer.setFilter(feature);
                    outer.setClassifier(inner);

                    result.add(new CustomClassifier(outer, c.getClass().getSimpleName(), feature.getSearch().getClass().getSimpleName(), ((BestFirst)feature.getSearch()).getDirection().getSelectedTag().getReadable(), sampling.getClass().getSimpleName(), false));
                }
            }
        }
    }

    /**
     * Add classifiers using both feature selection and cost-sensitive configuration.
     */
    private static void featureSelectionAndCostSensitiveClassifiers(List<Classifier> classifiers, List<AttributeSelection> featureSelections, List<CustomClassifier> result) {
        for (Classifier c : classifiers) {
            for (CostSensitiveClassifier cost : getCostSensitiveFilters()) {
                for (AttributeSelection feature : featureSelections) {
                    FilteredClassifier filtered = new FilteredClassifier();
                    filtered.setFilter(feature);
                    cost.setClassifier(c);
                    filtered.setClassifier(cost);
                    result.add(new CustomClassifier(filtered, c.getClass().getSimpleName(), feature.getSearch().getClass().getSimpleName(), ((BestFirst)feature.getSearch()).getDirection().getSelectedTag().getReadable(), NO_SAMPLING, true));
                }
            }
        }
    }

    /**
     * Compute oversampling and SMOTE percentages and create sampling filters.
     */
    private static @NotNull List<Filter> getSamplingFilters(int majority, int minority) {
        double oversamplePercent = ((100.0 * majority) / (majority + minority)) * 2;
        double smotePercent = (minority == 0 || minority > majority) ? 0 : ((100.0 * (majority - minority)) / minority);
        return createSamplingFilters(oversamplePercent, smotePercent);
    }

    /**
     * Create and configure list of Resample, Spread sub sample, and SMOTE filters.
     */
    private static @NotNull List<Filter> createSamplingFilters(double oversamplePercent, double smotePercent) {
        List<Filter> filters = new ArrayList<>();

        Resample resample = new Resample();
        resample.setBiasToUniformClass(1.0);
        resample.setSampleSizePercent(oversamplePercent);
        filters.add(resample);

        SpreadSubsample spread = new SpreadSubsample();
        spread.setDistributionSpread(1.0);
        filters.add(spread);

        SMOTE smote = new SMOTE();
        smote.setClassValue("1");
        smote.setPercentage(smotePercent);
        filters.add(smote);

        return filters;
    }

    /**
     * Create attribute selection filter using BestFirst strategy.
     */
    @Contract(" -> new")
    private static @NotNull @Unmodifiable List<AttributeSelection> getFeatureSelectionFilters() {
        AttributeSelection selection = new AttributeSelection();
        BestFirst best = new BestFirst();
        best.setDirection(new SelectedTag(2, best.getDirection().getTags()));
        selection.setSearch(best);
        return List.of(selection);
    }

    /**
     * Create list containing one CostSensitiveClassifier using default cost matrix.
     */
    @Contract(" -> new")
    private static @NotNull @Unmodifiable List<CostSensitiveClassifier> getCostSensitiveFilters() {
        CostSensitiveClassifier csc = new CostSensitiveClassifier();
        csc.setMinimizeExpectedCost(false);
        csc.setCostMatrix(createCostMatrix());
        return List.of(csc);
    }

    /**
     * Define and return the cost matrix used for cost-sensitive classification.
     */
    private static @NotNull CostMatrix createCostMatrix() {
        CostMatrix matrix = new CostMatrix(2);
        matrix.setCell(0, 0, 0.0);
        matrix.setCell(1, 0, WEIGHT_FALSE_POSITIVE);
        matrix.setCell(0, 1, WEIGHT_FALSE_NEGATIVE);
        matrix.setCell(1, 1, 0.0);
        return matrix;
    }
}
