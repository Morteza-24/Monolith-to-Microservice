import numpy as np
from sklearn import manifold
import skfuzzy as fuzz
import matplotlib.pyplot as plt


def fcm(class_similarity_matrix):
    mds = manifold.MDS(
        max_iter=10000000,
        eps=1e-90,
        dissimilarity="precomputed",
        normalized_stress="auto",
    )
    pos = mds.fit(class_similarity_matrix).embedding_
    alldata = np.vstack((pos[:, 0], pos[:, 1]))

    fig, ax = plt.subplots()
    ax.plot(pos[:,0], pos[:,1], ".")
    for i, xy in enumerate(zip(alldata[0], alldata[1])):
        ax.text(xy[0], xy[1], str(i), color="red", fontsize=12)
    fig.show()

    n_clusters = int(input("\nnumber of clusters: "))

    cntr, u, u0, d, jm, p, fpc = fuzz.cluster.cmeans(
        alldata, n_clusters, 2, error=1e-90, maxiter=100000, init=None)


    fig, axs = plt.subplots(n_clusters, sharex='all')
    for i, cluster in enumerate(u):
        axs[i].bar(range(len(class_similarity_matrix)), cluster)

    plt.xticks(range(len(class_similarity_matrix)))
    fig.show()

    cluster_membership = [{-1} for _ in u[0]]
    threshold = float(input("degree of membership threshold: "))
    for cluster_i in range(len(u)):
        for class_i in range(len(u[cluster_i])):
            if u[cluster_i][class_i] >= threshold:
                cluster_membership[class_i].discard(-1)
                cluster_membership[class_i].add(cluster_i)

    return cluster_membership
