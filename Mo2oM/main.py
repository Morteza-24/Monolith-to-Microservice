from os import path, pathsep
from subprocess import run
from json import load
import scipy.sparse as sp
import numpy as np
from Mo2oM.similarity_analysis import structural_similarity, semantic_similarity
from Mo2oM.clustering import overlapping_community_detection, process_threshold


def Mo2oM(source_code_path, n_clusters, threshold=None, alpha=0.5, use_tf_idf=False):
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
    adjacency_matrix = sp.csr_matrix((structural_similarity_matrix != 0).astype(int))
    if use_tf_idf:
        print("[Mo2oM] using tf-idf for semantic similarity", flush=True)
        from Mo2oM.similarity_analysis import tf_idf_semantic_similarity
        semantic_similarity_matrix = tf_idf_semantic_similarity(classes_info)
    else:
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
    # layers = np.load(f"test_projects/JPetStore/layers-{alpha}-{n_clusters[0]}-1_6.npy", allow_pickle=True)
    # return layers, classes_info
    # --- 5. run the script again.

    # --- DEBUG SECTION

    if isinstance(n_clusters, int):
        if alpha == 0:
            return overlapping_community_detection(adjacency_matrix, sp.csr_matrix(structural_similarity_matrix), n_clusters, threshold), classes_info
        elif alpha == 1:
            return overlapping_community_detection(adjacency_matrix, semantic_similarity_matrix, n_clusters, threshold), classes_info
        unixcoder_membership = overlapping_community_detection(adjacency_matrix, semantic_similarity_matrix, n_clusters, threshold, membership_only=True)
        structural_membership = overlapping_community_detection(adjacency_matrix, sp.csr_matrix(structural_similarity_matrix), n_clusters, threshold, membership_only=True)
        if isinstance(alpha, list):
            clusterings = []
            for alpha_value in alpha:
                print(f"[Mo2oM] alpha = {alpha_value}", flush=True)
                combined_membership = alpha_value * unixcoder_membership + (1 - alpha_value) * structural_membership
                clusterings.append(process_threshold(threshold, combined_membership))
            return clusterings, classes_info
        combined_membership = alpha * unixcoder_membership + (1 - alpha) * structural_membership
        return process_threshold(threshold, combined_membership), classes_info
    elif isinstance(n_clusters, str) and n_clusters == "Scanniello":
        len_classes = len(classes_info)
        n_clusters = np.arange(2, (len_classes//2)+2, 2)
        print(f"[Mo2oM] clustering with sizes from 2 to {(len_classes//2)+2}", flush=True)
    assert isinstance(n_clusters, list), "n_clusters should either be a list of integers or a single integer"
    clusterings = []
    for i in range(len(n_clusters)):
        print(f"[Mo2oM] n_clusters = {n_clusters[i]}")
        if alpha == 0:
            clusterings.append(overlapping_community_detection(adjacency_matrix, sp.csr_matrix(structural_similarity_matrix), n_clusters[i], threshold))
        elif alpha == 1:
            clusterings.append(overlapping_community_detection(adjacency_matrix, semantic_similarity_matrix, n_clusters[i], threshold))
        else:
            unixcoder_membership = overlapping_community_detection(adjacency_matrix, semantic_similarity_matrix, n_clusters[i], threshold, membership_only=True)
            structural_membership = overlapping_community_detection(adjacency_matrix, sp.csr_matrix(structural_similarity_matrix), n_clusters[i], threshold, membership_only=True)
            if isinstance(alpha, list):
                clusterings_results = []
                for alpha_value in alpha:
                    print(f"[Mo2oM] alpha = {alpha_value}", flush=True)
                    combined_membership = alpha_value * unixcoder_membership + (1 - alpha_value) * structural_membership
                    clusterings_results.append(process_threshold(threshold, combined_membership))
                clusterings.append(clusterings_results)
            else:
                combined_membership = alpha * unixcoder_membership + (1 - alpha) * structural_membership
                clusterings.append(process_threshold(threshold, combined_membership))
    return clusterings, classes_info
