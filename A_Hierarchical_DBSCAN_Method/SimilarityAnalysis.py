'''
def structural_similarity(ci, cj, classes_info) -> sim_str(ci, cj)
def semantic_similarity(ci, cj, classes_info) -> sim_sem(ci, cj)
def class_similarity(alpha, classes_info) -> class_similarity_matrix
'''
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
from nltkPre import preprocess


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
    class_text = []
    for cls in [ci, cj]:
        # text = cls['class_name'] + ' '
        text += ' '.join(cls['methods'])
        text += ' '.join(cls['method_calls'])
        text += ' '.join(cls['words'])
        class_text.append(preprocess(text))

    vectorizer = TfidfVectorizer()
    tf_idf_vectors = vectorizer.fit_transform(class_text)

    semantic_similarity = []
    for i in range(len(classes)):
        scores = cosine_similarity(tf_idf_vectors[i], tf_idf_vectors)
        semantic_similarity.append(scores)

    return semantic_similarity