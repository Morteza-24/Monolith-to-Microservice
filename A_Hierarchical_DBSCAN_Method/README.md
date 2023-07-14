# A Hierarchical DBSCAN Method for Extracting Microservices from Monolithic Applications

The method used in this paper consists of two major steps:
1. Similarity Analysis: which contains two parts:
    - Structural Analysis: In this part, a Structural Similarity Matrix is built from the AST of the source code.
    - Semantic Analysis: In this part, a Semantic Similarity Matrix is built from the word vectors made from the source code.
2. Microservice Extraction: In this step, the matrices from step 1 are combined to give a Class Similarity Matrix, which is fed to the DBSCAN clustering algorithm to generate decomposition layers.

![a summary of the steps taken to extract the microservices](res/steps.png)


# Current Status

- [ ] Creating a java source code parser using JavaParser with the aim of assisting in Similarity Analysis.

