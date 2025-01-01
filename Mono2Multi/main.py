from os import path, pathsep
from subprocess import run
from json import load
from Mono2Multi.SimilarityAnalysis import class_similarity
from Mono2Multi.Clustering import fcm
import numpy as np


def Mono2Multi(source_code_path, alpha, n_clusters=None, threshold=None, n_fcm_execs=1):
    # parse the source code and get classes, methods, etc.
    print("\n[Mono2Multi] parsing the code...", end=" ", flush=True)
    base_dir = path.dirname(path.realpath(__file__))
    libs = path.join(base_dir, "JavaParser/lib/javaparser-core-3.25.5-SNAPSHOT.jar")+pathsep+path.join(base_dir, "JavaParser/lib/json-20230618.jar")
    json_path = path.join(base_dir, "JavaParser/classes.json")
    run(['java', '-cp', libs, path.join(base_dir, 'JavaParser/Parser.java'), source_code_path, json_path]) 
    with open(json_path, "rt") as classes_file:
        classes_info = load(classes_file)
    print("done!")

    # finding class names for each method call
    print("[Mono2Multi] analyzing method calls...", end=" ", flush=True)
    for clss in classes_info:
        for call in classes_info[clss]["method_calls"]:
            for other_clss in classes_info:
                if call["method_name"] in classes_info[other_clss]["methods"]:
                    call["class_name"] = other_clss
                    if clss == other_clss:
                        break
    print("done!")

    # get class similarity metrix to feed to FCM
    print("[Mono2Multi] building class similarity matrix", flush=True)
    class_similarity_matrix = class_similarity(alpha, classes_info)
    print("[Mono2Multi] class similarity matrix built successfully!", flush=True)

    # --- DEBUG SECTION

    # --- 1. uncomment the following lines and run the script.
    # np.save("csm.npy", class_similarity_matrix)
    # return
    # --- 2. upload csm.npy to google colab and run the notebook.
    # --- 3. download the layers.npy file from google colab.
    # --- 4. recomment the above lines and uncomment the following lines.
    # layers = np.load(f"Test_Projects/JPetStore/layers-{alpha}-{n_clusters[0]}-1_6.npy", allow_pickle=True)
    # return layers, classes_info
    # --- 5. run the script again.

    # --- DEBUG SECTION

    if isinstance(n_clusters, int) or n_clusters is None:
            return fcm(class_similarity_matrix, n_clusters, threshold, n_fcm_execs), classes_info
    elif isinstance(n_clusters, str) and n_clusters == "Scanniello":
        len_classes = len(classes_info)
        n_clusters = np.arange(2, (len_classes//2)+2, 2)
        print(f"[Mono2Multi] clustering with sizes from 2 to {(len_classes//2)+2}", flush=True)
    layers = []
    for i in range(len(n_clusters)):
        print(f"[Mono2Multi] n_clusters = {n_clusters[i]}", flush=True)
        layers.append(fcm(class_similarity_matrix, n_clusters[i], threshold, n_fcm_execs))
    return layers, classes_info
