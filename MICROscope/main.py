from os import path, pathsep
from subprocess import run
from json import load
from MICROscope.SimilarityAnalysis import class_similarity
from MICROscope.Clustering import fcm
import numpy as np


def MICROscope(source_code_path, alpha, n_clusters=None, threshold=None, n_fcm_execs=10):
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
                    if clss == other_clss:
                        break
    print("done!")

    # get class similarity metrix to feed to FCM
    print("[MICROscope] building class similarity matrix", flush=True)
    class_similarity_matrix = class_similarity(alpha, classes_info)
    print("[MICROscope] class similarity matrix built successfully!", flush=True)

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

    if isinstance(n_clusters, int) or n_clusters == None:
        if isinstance(threshold, int) or isinstance(threshold, float) or threshold == None:
            return fcm(class_similarity_matrix, n_clusters, threshold, n_fcm_execs), classes_info
        else:
            layers = []
            print("[FuzzyCMeans] 0%", end="", flush=True)
            for i in range(len(threshold)):
                layers.append(fcm(class_similarity_matrix, n_clusters, threshold[i], n_fcm_execs))
                print(f"\r[FuzzyCMeans] {int(100*(i+1)/len(threshold))}%", end="", flush=True)
            print("\r[FuzzyCMeans] 100%", flush=True)
            return layers, classes_info
    else:
        layers = []
        if isinstance(threshold, int) or isinstance(threshold, float):
            print("[FuzzyCMeans] 0%", end="", flush=True)
            for i in range(len(n_clusters)):
                layers.append(fcm(class_similarity_matrix, n_clusters[i], threshold, n_fcm_execs))
                print(f"\r[FuzzyCMeans] {int(100*(i+1)/len(n_clusters))}%", end="", flush=True)
            print("\r[FuzzyCMeans] 100%", flush=True)
            return layers, classes_info
        else:
            print("[FuzzyCMeans] 0%", end="", flush=True)
            for i in range(len(n_clusters)):
                layers.append(fcm(class_similarity_matrix, n_clusters[i], threshold[i], n_fcm_execs))
                print(f"\r[FuzzyCMeans] {int(100*(i+1)/len(n_clusters))}%", end="", flush=True)
            print("\r[FuzzyCMeans] 100%", end="", flush=True)
            return layers, classes_info
