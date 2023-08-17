'''
def structural_similarity(ci, cj, classes_info) -> sim_str(ci, cj)
def semantic_similarity(ci, cj, classes_info) -> sim_sem(ci, cj)
def class_similarity(alpha, classes_info) -> class_similarity_matrix
'''


from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
from numpy import zeros
from A_Hierarchical_DBSCAN_Method.Preprocess import preprocess


def calls(ci, cj, classes_info):
    return len([
        _ for _ in classes_info[ci]["method_calls"]
        if _ in classes_info[cj]["methods"]
    ])


def calls_in(ci, classes_info):
    return sum(
        [calls(cj, ci, classes_info) for cj in classes_info if cj != ci])


def structural_similarity(ci, cj, classes_info):
    if calls_in(ci, classes_info) != 0 and calls_in(cj, classes_info) != 0:
        return (1 / 2) * (
            calls(ci, cj, classes_info) / calls_in(cj, classes_info) +
            calls(cj, ci, classes_info) / calls_in(ci, classes_info))
    elif calls_in(ci, classes_info) == 0 and calls_in(cj, classes_info) != 0:
        return calls(ci, cj, classes_info) / calls_in(cj, classes_info)
    elif calls_in(ci, classes_info) != 0 and calls_in(cj, classes_info) == 0:
        return calls(cj, ci, classes_info) / calls_in(ci, classes_info)
    else:
        return 0


def semantic_similarity(ci, cj, classes_info):
    corpus = []
    for clss in classes_info:
        corpus.append(preprocess(' '.join(classes_info[clss]['words'])))

    vectorizer = TfidfVectorizer()
    tf_idf_vectors = vectorizer.fit_transform(corpus)

    ci = list(classes_info.keys()).index(ci)
    cj = list(classes_info.keys()).index(cj)

    return cosine_similarity(tf_idf_vectors[ci], tf_idf_vectors[cj])[0][0]


def class_similarity(alpha, classes_info):
    class_similarity_matrix = zeros((len(classes_info), len(classes_info)))
    for i in range(len(classes_info)):
        for j in range(i+1,len(classes_info)):
            ci = list(classes_info.keys())[i]
            cj = list(classes_info.keys())[j]
            class_similarity_matrix[i][j] = alpha*structural_similarity(ci, cj, classes_info) + (1-alpha)*semantic_similarity(ci, cj, classes_info)
    return class_similarity_matrix + class_similarity_matrix.T
