'''
def structural_similarity(ci, cj, classes_info) -> sim_str(ci, cj)
def semantic_similarity(ci, cj, classes_info) -> sim_sem(ci, cj)
def class_similarity(alpha, classes_info) -> class_similarity_matrix
'''
from sklearn.feature_extraction.text import TfidfVectorizer, CountVectorizer
from sklearn.metrics.pairwise import cosine_similarity
from Preprocess import preprocess, dummy


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
        text = []
        text.append(' '.join(cls['methods']))
        text.append(' '.join(cls['method_calls']))
        text.append(' '.join(cls['words']))
        class_text.append(preprocess(' '.join(text)))

    vectorizer = CountVectorizer(
        tokenizer=dummy,
        preprocessor=dummy,
    )
    tf_idf_vectors = vectorizer.fit_transform(class_text)

    semantic_similarity = []
    for i in range(len(classes_info['classes'].keys())):
        scores = cosine_similarity(tf_idf_vectors[i], tf_idf_vectors)
        semantic_similarity.append(scores)

    return semantic_similarity