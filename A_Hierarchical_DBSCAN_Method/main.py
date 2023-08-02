from SimilarityAnalysis import class_similarity
from DBSCAN import dbscan
from nltk import download
from sys import argv
from os import pathsep
import subprocess
import json


# parse the source code and get classes, methods, etc.
source_code_path = argv[1]
libs = "JavaParser/lib/javaparser-core-3.25.5-SNAPSHOT.jar"+pathsep+"JavaParser/lib/json-20230618.jar"
subprocess.run(['java', '-cp', libs, 'JavaParser/Parser.java', source_code_path]) 
with open("JavaParser/classes.json", "rt") as classes_file:
    classes_info = json.load(classes_file)


# necessary downloads for nltk
download('punkt')
download('stopwords')


# get class similarity metric to feed to DBSCAN
alpha = float(argv[2])
class_similarity_matrix = class_similarity(alpha, classes_info)


# hyperparameters
minimum_number_of_sample = int(argv[3])
max_epsilon = float(argv[4])


print("-"*50)


# run DBSCAN with different epsilon values to create decomposition layers
epsilon = 0.05
while epsilon <= max_epsilon:
    layer = dbscan(minimum_number_of_sample, epsilon, class_similarity_matrix)
    print("epsilon:", epsilon, "--->", layer)
    epsilon += 0.05
    epsilon = round(epsilon, 2)


# TODO: visualize layers
