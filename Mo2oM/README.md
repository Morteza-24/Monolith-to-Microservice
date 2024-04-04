# Mo2oM: from Monoliths to Overlapping Microservices, using CodeBERT and Neural Overlapping Community Detection

This method consists of two major steps:
1. Similarity Analysis: which contains two parts:
    - Structural Analysis: In this part, a Structural Similarity Matrix is built from the AST of the source code regarding method calls.
    - Semantic Analysis: In this part, the source code of each class is embedded using the CodeBERT language model.
2. Microservice Extraction: In this step, the results from step 1 are fed to a Neural Overlapping Community Detection model to generate overlapping microservices.


# How to Use

1. Make sure you have Java installed and the `java` command is available.
2. Install python requirements:
```
python -m pip install -r requirements.txt
```
3. import it in your code and use it:
```
# example.py

from Mono2Multi.main import Mo2oM

source_file_path = "path/to/source.java"
n_clusters = 4
threshold = 0.5

clusters = Mo2oM(source_file_path, n_clusters, threshold)[0]

```
