import numpy as np
from sklearn import manifold
import skfuzzy as fuzz
import matplotlib.pyplot as plt


def fcm(class_similarity_matrix, n_clusters):
    mds = manifold.MDS(
        max_iter=3000,
        eps=1e-9,
        dissimilarity="precomputed",
        normalized_stress="auto",
    )
    pos = mds.fit(class_similarity_matrix).embedding_

    alldata = np.vstack((pos[:, 0], pos[:, 1]))
    cntr, u, u0, d, jm, p, fpc = fuzz.cluster.cmeans(
        alldata, 5, 2, error=0.005, maxiter=1000, init=None)

    cluster_membership = np.argmax(u, axis=0)
    colors = ['b', 'orange', 'g', 'r', 'c', 'm', 'y', 'k', 'Brown', 'ForestGreen']
    for i in range(n_clusters):
        plt.plot(pos[:,0][cluster_membership == i],
                pos[:,1][cluster_membership == i], '.', color=colors[i])

    for i, xy in enumerate(zip(alldata[0], alldata[1])):
        plt.text(xy[0], xy[1], str(i), color="red", fontsize=12)

    return u, fpc
