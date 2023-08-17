# A Hierarchical DBSCAN Method for Extracting Microservices from Monolithic Applications

The method used in this paper consists of two major steps:
1. Similarity Analysis: which contains two parts:
    - Structural Analysis: In this part, a Structural Similarity Matrix is built from the AST of the source code.
    - Semantic Analysis: In this part, a Semantic Similarity Matrix is built from the word vectors made from the source code.
2. Microservice Extraction: In this step, the matrices from step 1 are combined to give a Class Similarity Matrix, which is fed to the DBSCAN clustering algorithm to generate decomposition layers.

![a summary of the steps taken to extract the microservices](res/steps.png)


# How to Use

1. Make sure you have Java installed and the `java` command is available.
2. Make sure that `numpy`, `nltk` and `scikit-learn` are installed:
```
python -m pip install numpy nltk scikit-learn
```
3. import it in your code and use it:
```
# example.py

from A_Hierarchical_DBSCAN_Method.main import hierarchical_DBSCAN

source_file_path = "../test.java"
alpha = 0.5
min_samples = 2
max_epsilon = 0.7

layers, classes_info = hierarchical_DBSCAN(
        source_file_path, alpha, min_samples, max_epsilon)

```
