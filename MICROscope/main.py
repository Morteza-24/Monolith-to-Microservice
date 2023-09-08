from os import path, pathsep
from subprocess import run
from json import load
from MICROscope.SimilarityAnalysis import class_similarity
from MICROscope.Clustering import fcm


def MICROscope(source_code_path, alpha, n_clusters):
    # parse the source code and get classes, methods, etc.
    print("\n[MICROscope] parsing the code...", end=" ", flush=True)
    base_dir = path.dirname(path.realpath(__file__))
    libs = path.join(base_dir, "JavaParser/lib/javaparser-core-3.25.5-SNAPSHOT.jar")+pathsep+path.join(base_dir, "JavaParser/lib/json-20230618.jar")
    json_path = path.join(base_dir, "JavaParser/classes.json")
    run(['java', '-cp', libs, path.join(base_dir, 'JavaParser/Parser.java'), source_code_path, json_path]) 
    with open(json_path, "rt") as classes_file:
        classes_info = load(classes_file)
    print("done!")

    # finding class names for each method call
    print("[MICROscope] analyzing method calls...", end=" ", flush=True)
    for clss in classes_info:
        for call in classes_info[clss]["method_calls"]:
            for other_clss in classes_info:
                if call["method_name"] in classes_info[other_clss]["methods"]:
                    call["class_name"] = other_clss
                    break
    print("done!\n")

    # get class similarity metric to feed to DBSCAN
    print("\n[MICROscope] building class similarity matrix...", flush=True)
    class_similarity_matrix = class_similarity(alpha, classes_info)
    print("[MICROscope] done!")

    return fcm(class_similarity_matrix, n_clusters), classes_info
