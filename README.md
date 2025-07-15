# MACHINE LEARNING TO ANALYZE BUGGY METHODS

## Overview

This project extracts method-level software metrics and bug labels from Git repositories linked to JIRA.
The collected data supports training of machine learning models to identify buggy methods across software releases.

## Features

* Extracts method-level metrics from Java source code
* Associates Git commits to JIRA tickets
* Annotates methods as buggy or clean based on ticket resolution
* Compute Features:
* Parses PMD code smells for each release

* ML for evaluation

## Architecture

```
ProgramFlow
 └── Pipeline
      └── TicketInjection
       └── GitInjection
        └── LabelPreprocessing
         └── FeaturePreprocessing
          └── SinkDatasets
           └── MachineLearnig
            └── SinkResults 
```

## Requirements

* Java 21 +
* Maven
* PMD (environment variable `SYS_PMD_HOME` must be set)
* Dual-core Cpu | 8gb RAM

## How to Run

Prepare a JSON config file with project identifiers and Git URLs:

```json
{
  "PROJECT_KEY": "https://github.com/org/project.git"
}
```

Run with:

```bash
mvn clean package
java -jar SEAnalyzer.jar path/to/config.json
```

## PMD

The installation of PMD is needed
and setup `SYS_PMD_HOME=path/to/pmd`

Outputs:

* `pmdAnalysis/<project>/<release>.csv`: PMD smell report
* Internal data structures:

    * List of commits per release
    * List of methods with associated metrics and bug labels

## Machine Learning Ready

You can export the method metrics and bug labels to CSV or ARFF format to use them in supervised learning models such as:

* Random Forests
* Logistic Regression
* Neural Networks

## License

GPL License

## Authors

Developed by the Software Dissan Uddin AHMED at the University of Rome Tor Vergata.
