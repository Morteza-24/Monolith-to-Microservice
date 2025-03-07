# Mono2Multi: A Fuzzy Clustering, CodeBERT-Based Method for Extracting Microservices from Monolithic Applications

This method consists of two major steps:
1. Similarity Analysis: which contains two parts:
    - Structural Analysis: In this part, a Structural Similarity Matrix is built from the AST of the source code.
    - Semantic Analysis: In this part, a Semantic Similarity Matrix is built using the CodeBERT language model.
2. Microservice Extraction: In this step, the matrices from step 1 are combined to give a Class Similarity Matrix, which is fed to the fuzzy-cmeans clustering algorithm to generate decomposition layers.


# How to Use

1. Make sure you have Java installed and the `java` command is available.
2. Install python requirements:
```
python -m pip install -r requirements.txt
```
3. import it in your code and use it:
```
# example.py

from Mono2Multi.main import Mono2Multi

source_file_path = "path/to/source.java"
alpha = 0.5

clusters = Mono2Multi(source_file_path, alpha)

```
