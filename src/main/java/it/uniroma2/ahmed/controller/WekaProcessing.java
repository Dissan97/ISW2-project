package it.uniroma2.ahmed.controller;

import it.uniroma2.ahmed.logging.SeLogger;
import it.uniroma2.ahmed.model.ClassifierResult;
import it.uniroma2.ahmed.model.CustomClassifier;
import it.uniroma2.ahmed.utilities.Sink;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

public class WekaProcessing {

    private final String projName;
    private final int numIter;
    private final List<ClassifierResult> classifierResults;

    public WekaProcessing(String projectName, int iterations) {
        this.projName = projectName;
        this.numIter = iterations;
        this.classifierResults = new ArrayList<>();
    }


    public void classify() {

        final String arff = Sink.FileExtension.ARFF.name().toLowerCase(Locale.getDefault());
        final String head = Sink.DELIVERY_OUTPUT + File.separator + "datasets" + File.separator + projName +
                File.separator + arff + File.separator;
        final String training_path = head + "training" + File.separator + this.projName;
        final String testing_path = head + "testing" + File.separator + this.projName;

        // Create a CountDownLatch with numIter count
        CountDownLatch latch = new CountDownLatch(this.numIter);

        for (int walkForwardIteration = 1; walkForwardIteration <= this.numIter; walkForwardIteration++) {
            final int iteration = walkForwardIteration;

            Runnable task = () -> {
                try {
                    ConverterUtils.DataSource trainingSetDataSource = new ConverterUtils
                            .DataSource(training_path + '_' + iteration + '.' + arff);
                    ConverterUtils.DataSource testingSetDataSource = new ConverterUtils
                            .DataSource(testing_path + '_' + iteration + '.' + arff);
                    Instances trainingSetInstance = trainingSetDataSource.getDataSet();
                    Instances testingSetInstance = testingSetDataSource.getDataSet();

                    int numAttr = trainingSetInstance.numAttributes();
                    trainingSetInstance.setClassIndex(numAttr - 1);
                    testingSetInstance.setClassIndex(numAttr - 1);

                    List<CustomClassifier> customClassifiers =
                            ComputeAllClassifiersCombinations.returnAllClassifiersCombinations(
                                    trainingSetInstance.attributeStats(numAttr - 1));

                    for (CustomClassifier customClassifier : customClassifiers) {
                        Evaluation evaluator = new Evaluation(testingSetInstance);
                        Classifier classifier = customClassifier.getClassifier();
                        classifier.buildClassifier(trainingSetInstance);
                        evaluator.evaluateModel(classifier, testingSetInstance);
                        ClassifierResult resultOfClassifier = new ClassifierResult(iteration, customClassifier, evaluator);
                        resultOfClassifier.setTrainingPercent(100.0 * (
                                (double) trainingSetInstance.numInstances() /
                                        (trainingSetInstance.numInstances() + testingSetInstance.numInstances())));

                        synchronized (classifierResults) { // Ensure thread-safe access
                            classifierResults.add(resultOfClassifier);
                        }
                    }
                } catch (Exception e) {
                    final String severe = "Error in classify() during walkForwardIteration: " + iteration;
                    SeLogger.getInstance().getLogger().severe(severe + ": " + e.getMessage());
                } finally {
                    latch.countDown(); // Decrement latch count when done
                }
            };

            // Start each task in a new thread
            new Thread(task).start();
        }

        // Wait for all threads to complete
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            SeLogger.getInstance().getLogger().severe("Classification interrupted: " + e.getMessage());
        }
    }

    public void sinkResults() {
        Sink.serializeResultsToCsv(this.projName, this.classifierResults);
    }
}
