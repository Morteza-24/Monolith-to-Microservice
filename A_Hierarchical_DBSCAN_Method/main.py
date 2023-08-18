from nltk import download
from os import path, pathsep
from subprocess import run
from json import load
from A_Hierarchical_DBSCAN_Method.SimilarityAnalysis import class_similarity
from A_Hierarchical_DBSCAN_Method.DBSCAN import dbscan


def hierarchical_DBSCAN(source_code_path, alpha, minimum_number_of_sample, max_epsilon):
    # parse the source code and get classes, methods, etc.
    print("\n[hierarchical_DBSCAN] parsing the code...", end=" ", flush=True)
    base_dir = path.dirname(path.realpath(__file__))
    libs = path.join(base_dir, "JavaParser/lib/javaparser-core-3.25.5-SNAPSHOT.jar")+pathsep+path.join(base_dir, "JavaParser/lib/json-20230618.jar")
    json_path = path.join(base_dir, "JavaParser/classes.json")
    run(['java', '-cp', libs, path.join(base_dir, 'JavaParser/Parser.java'), source_code_path, json_path]) 
    with open(json_path, "rt") as classes_file:
        classes_info = load(classes_file)
    print("done!")

    # finding class names for each method call
    print("[hierarchical_DBSCAN] analyzing method calls...", end=" ", flush=True)
    for clss in classes_info:
        for call in classes_info[clss]["method_calls"]:
            for other_clss in classes_info:
                if call["method_name"] in classes_info[other_clss]["methods"]:
                    call["class_name"] = other_clss
                    break
    print("done!\n")

    # necessary downloads for nltk
    download('punkt')
    download('stopwords')

    # get class similarity metric to feed to DBSCAN
    print("\n[hierarchical_DBSCAN] building class similarity matrix...", flush=True)
    class_similarity_matrix = class_similarity(alpha, classes_info)
    print("[hierarchical_DBSCAN] done!")

    # run DBSCAN with different epsilon values to create decomposition layers
    layers = {}
    epsilon = 0.01
    while epsilon <= max_epsilon:
        layer = dbscan(minimum_number_of_sample, epsilon, class_similarity_matrix)
        layers[epsilon] = layer
        epsilon += 0.01
        epsilon = round(epsilon, 2)

    # TODO: visualize layers

    return layers, classes_info
