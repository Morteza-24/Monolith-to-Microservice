from os import path, pathsep
from subprocess import run
from json import load
from Mo2oM.SimilarityAnalysis import structural_similarity, semantic_similarity
from Mo2oM.Clustering import overlapping_community_detection


def Mo2oM(source_code_path, n_clusters, threshold=None):
    # parse the source code and get classes, methods, etc.
    print("\n[Mo2oM] parsing the code...", end=" ", flush=True)
    base_dir = path.dirname(path.realpath(__file__))
    libs = path.join(base_dir, "JavaParser/lib/javaparser-core-3.25.5-SNAPSHOT.jar")+pathsep+path.join(base_dir, "JavaParser/lib/json-20230618.jar")
    json_path = path.join(base_dir, "JavaParser/classes.json")
    run(['java', '-cp', libs, path.join(base_dir, 'JavaParser/Parser.java'), source_code_path, json_path]) 
    with open(json_path, "rt") as classes_file:
        classes_info = load(classes_file)
    print("done!")

    # find class names for each method call
    print("[Mo2oM] analyzing method calls...", end=" ", flush=True)
    for clss in classes_info:
        for call in classes_info[clss]["method_calls"]:
            for other_clss in classes_info:
                if call["method_name"] in classes_info[other_clss]["methods"]:
                    call["class_name"] = other_clss
                    if clss == other_clss:
                        break
    print("done!")

    # get class similarity metrices to feed to NOCD
    print("[Mo2oM] building similarity matrices", flush=True)
    structural_similarity_matrix = structural_similarity(classes_info)
    semantic_similarity_matrix = semantic_similarity(classes_info)
    print("[Mo2oM] similarity matrices built successfully!", flush=True)

    # --- DEBUG SECTION

    # --- 1. uncomment the following lines and run the script.
    # import scipy.sparse as sp
    # sp.save_npz('structural.npz', structural_similarity_matrix)
    # sp.save_npz('semantic.npz', semantic_similarity_matrix)
    # --- 2. upload structural.npz and semantic.npz to google colab and run the notebook.
    # --- 3. download the membership.npy file from google colab.
    # --- 4. recomment the above lines and uncomment the following lines.
    # layers = np.load(f"Test_Projects/JPetStore/layers-{alpha}-{n_clusters[0]}-1_6.npy", allow_pickle=True)
    # return layers, classes_info
    # --- 5. run the script again.

    # --- DEBUG SECTION

    if isinstance(n_clusters, int):
            return overlapping_community_detection(structural_similarity_matrix, semantic_similarity_matrix, n_clusters, threshold), classes_info
    else:
        clusterings = []
        for i in range(len(n_clusters)):
            print(f"[Mo2oM] n_clusters = {n_clusters[i]}")
            clusterings.append(overlapping_community_detection(structural_similarity_matrix, semantic_similarity_matrix, n_clusters[i], threshold))
        return clusterings, classes_info