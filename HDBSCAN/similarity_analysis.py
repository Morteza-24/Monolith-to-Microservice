'''
def structural_similarity(ci, cj, classes_info) -> sim_str(ci, cj)
def semantic_similarity(ci, cj, classes_info) -> sim_sem(ci, cj)
def class_similarity(alpha, classes_info) -> class_similarity_matrix
'''


from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
from numpy import zeros
from HDBSCAN.preprocess import preprocess


def calls(ci, cj, classes_info):
    return len([
        1 for call in classes_info[ci]["method_calls"]
        if call["class_name"] == cj
    ])


def calls_in(ci, classes_info):
    return sum(
        [calls(cj, ci, classes_info) for cj in classes_info])


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


def semantic_similarity_vectors(classes_info):
    corpus = []
    for clss in classes_info:
        corpus.append(preprocess(' '.join(classes_info[clss]['words'])))

    vectorizer = TfidfVectorizer()
    tf_idf_vectors = vectorizer.fit_transform(corpus)

    return tf_idf_vectors


def class_similarity(alpha, classes_info):
    class_similarity_matrix = zeros((len(classes_info), len(classes_info)))
    tf_idf_vectors = semantic_similarity_vectors(classes_info)
    len_classes_info = len(classes_info)
    for i in range(len_classes_info):
        for j in range(i+1,len_classes_info):
            ci = list(classes_info.keys())[i]
            cj = list(classes_info.keys())[j]
            class_similarity_matrix[i][j] = 1 - (alpha*structural_similarity(ci, cj, classes_info) + (1-alpha)*cosine_similarity(tf_idf_vectors[i], tf_idf_vectors[j])[0][0])
        print(f"\r[similarity_analysis] {int(100*i/len_classes_info):02d}%", end="", flush=True)
    print(f"\r[similarity_analysis] 100%", flush=True)

    return class_similarity_matrix + class_similarity_matrix.T
