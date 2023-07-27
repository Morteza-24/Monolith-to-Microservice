from SimilarityAnalysis import class_similarity
from DBSCAN import DBSCAN
from sys import argv
import subprocess
import json


# parse the source code and get classes, methods, etc.
source_code_path = argv[1]
subprocess.run(["java", "-cp", "JavaParser/javaparser-core-3.25.5-SNAPSHOT.jar", "JavaParser/Parser", source_code_path]) 
with open("JavaParser/classes.json", "rt") as classes_file:
    classes_info = json.load(classes_file)

'''
# words are the class name, variable names, method names, etc.

classes_info = {
    "classes": [
        "class_1": {
            "methods": ["method_1", "method_2", "method_3"],
            "method_calls": ["method_5"] ,
            "words": ["word_1", "word_2", "word_3"]
        },
        "class_2": {
            "methods": ["method_4, "method_5", "method_6"],
            "method_calls": ["method_1", "method_2"],
            "words": ["word_2", "word_4", "word_5"]
        }
    ]
}
'''

# get class similarity metric to feed to DBSCAN
alpha = argv[2]
class_similarity_matrix = class_similarity(alpha, classes_info)


# hyperparameters
minimum_number_of_sample = argv[3]
max_epsilon = argv[4]


# run DBSCAN with different epsilon values to create decomposition layers
layers = []
for epsilon in range(0, max_epsilon):
    layers.append(DBSCAN(minimum_number_of_sample, epsilon, class_similarity_matrix))


# TODO: visualize layers

