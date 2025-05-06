import numpy as np
from sklearn import manifold
import skfuzzy as fuzz
import matplotlib.pyplot as plt


def fcm(class_similarity_matrix, n_clusters, threshold, n_execs):
    mds = manifold.MDS(
        max_iter=10000000,
        eps=1e-90,
        dissimilarity="precomputed",
        normalized_stress="auto",
    )
    pos = mds.fit(class_similarity_matrix).embedding_
    alldata = np.vstack((pos[:, 0], pos[:, 1]))

    if n_clusters == None:
        fig, ax = plt.subplots()
        ax.plot(pos[:,0], pos[:,1], ".")
        for i, xy in enumerate(zip(alldata[0], alldata[1])):
            ax.text(xy[0], xy[1], str(i), color="red", fontsize=12)
        fig.show()
        n_clusters = int(input("\nnumber of clusters: "))
        print()

    print("[FuzzyCMeans] 0%", end="", flush=True)
    cntr, total_u, u0, d, jm, p, fpc = fuzz.cluster.cmeans(
        alldata, n_clusters, 2, error=1e-90, maxiter=100000, seed=42)
    print(f"\r[FuzzyCMeans] {int(100*(1)/n_execs)}%", end="", flush=True)
    for i in range(n_execs-1):
        cntr, u, u0, d, jm, p, fpc = fuzz.cluster.cmeans(
            alldata, n_clusters, 2, error=1e-90, maxiter=100000, init=None)
        total_u += u
        print(f"\r[FuzzyCMeans] {int(100*(i+2)/n_execs)}%", end="", flush=True)
    memberships = total_u/n_execs
    print("\r[FuzzyCMeans] 100%", flush=True)

    if threshold == None:
        fig, axs = plt.subplots(n_clusters, sharex='all')
        for i, cluster in enumerate(memberships):
            axs[i].bar(range(len(class_similarity_matrix)), cluster)

        plt.xticks(range(len(class_similarity_matrix)))
        fig.show()
        threshold = float(input("degree of membership threshold: "))
    elif not (isinstance(threshold, int) or isinstance(threshold, float)):
        layers = []
        for threshold_i in threshold:
            print(f"[Mono2Multi] threshold = {threshold_i}")
            clusters = [{-1} for _ in memberships[0]]
            for cluster_i in range(len(memberships)):
                for class_i in range(len(memberships[cluster_i])):
                    if memberships[cluster_i][class_i] >= threshold_i:
                        clusters[class_i].discard(-1)
                        clusters[class_i].add(cluster_i)
            layers.append(clusters)
        return layers

    clusters = [{-1} for _ in memberships[0]]
    for cluster_i in range(len(memberships)):
        for class_i in range(len(memberships[cluster_i])):
            if memberships[cluster_i][class_i] >= threshold:
                clusters[class_i].discard(-1)
                clusters[class_i].add(cluster_i)

    return clusters
