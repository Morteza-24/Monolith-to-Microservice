from similarity_analysis import class_similarity
from DBSCAN import DBSCAN
from sys import argv
import subprocess


# parse the source code and get classes, methods, etc.
source_code_path = argv[1]
subprocess.run(["java", "JavaParser/Parser", source_code_path]) 
with open("JavaParser/classes.json", "rt") as classes_file:
    classes_info = json.load(classes_file)


# get class similarity metric to feed to DBSCAN
alpha = argv[2]
structrual_similarity_matrix = structrual_similarity(classes_info)
semantic_similarity_matrix = semantic_similarity(classes_info)
class_similarity_matrix = class_similarity(alpha, structural_similarity_matrix, semantic_similarity_matrix)


# hyperparameters
minimum_number_of_sample = argv[3]
max_epsilon = argv[4]


# run DBSCAN with different epsilon values to create decomposition layers
layers = []
for epsilon in range(0, max_epsilon):
    layers.append(DBSCAN(minimum_number_of_sample, epsilon, class_similarity_matrix))


# TODO: visualize layers

