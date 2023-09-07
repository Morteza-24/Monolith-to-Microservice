import numpy as np
from sklearn import manifold
import skfuzzy as fuzz


def fcm(class_similarity_matrix):
    pos = _mds(class_similarity_matrix)
    alldata = np.vstack((pos[:,0],pos[:,1]))
    cntr, u, u0, d, jm, p, fpc = fuzz.cluster.cmeans(
        alldata, 5, 2, error=0.005, maxiter=1000, init=None)
    return u0


def _mds(class_similarity_matrix):
    mds = manifold.MDS(
    max_iter=3000,
    eps=1e-9,
    dissimilarity="precomputed",
    n_jobs=1,
    normalized_stress="auto",
    )
    return mds.fit(class_similarity_matrix).embedding_

