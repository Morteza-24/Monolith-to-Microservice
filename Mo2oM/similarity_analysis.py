import numpy as np
import scipy.sparse as sp


def _calls(ci, cj, classes_info):
    return len([
        1 for call in classes_info[ci]["method_calls"]
        if call["class_name"] == cj
    ])


def _calls_in(ci, classes_info):
    return sum(
        [_calls(cj, ci, classes_info) for cj in classes_info])


def structural_similarity(classes_info):
    len_classes_info = len(classes_info)
    structural_similarity_matrix = np.zeros((len_classes_info, len_classes_info))
    clss_names = list(classes_info.keys())
    for i in range(len_classes_info):
        ci = clss_names[i]
        for j in range(i+1,len_classes_info):
            cj = clss_names[j]
            if _calls_in(ci, classes_info) != 0 and _calls_in(cj, classes_info) != 0:
                structural_similarity_matrix[i][j] = (1 / 2) * (
                    _calls(ci, cj, classes_info) / _calls_in(cj, classes_info) +
                    _calls(cj, ci, classes_info) / _calls_in(ci, classes_info))
            elif _calls_in(ci, classes_info) == 0 and _calls_in(cj, classes_info) != 0:
                structural_similarity_matrix[i][j] = _calls(ci, cj, classes_info) / _calls_in(cj, classes_info)
            elif _calls_in(ci, classes_info) != 0 and _calls_in(cj, classes_info) == 0:
                structural_similarity_matrix[i][j] = _calls(cj, ci, classes_info) / _calls_in(ci, classes_info)
            else:
                structural_similarity_matrix[i][j] = 0
        print(f"\r[StructuralSimilarity] {int(100*(i+1)/len_classes_info)}%", end="", flush=True)
    print(f"\r[StructuralSimilarity] 100%", flush=True)
    return sp.csr_matrix((structural_similarity_matrix != 0).astype(int))


def semantic_similarity(classes_info):
    import torch
    from Mono2Multi.unixcoder import UniXcoder

    torch.manual_seed(42)
    torch.cuda.manual_seed(42)
    torch.cuda.manual_seed_all(42)
    torch.backends.cudnn.deterministic = True
    torch.use_deterministic_algorithms(True)
    np.random.seed(42)

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"[SemanticSimilarity] using device {device}", flush=True)
    model = UniXcoder("microsoft/unixcoder-base")
    model.to(device)

    len_classes_info = len(classes_info)
    i = 0
    for clss in classes_info:
        source_i = classes_info[clss]["source"]
        tokens_ids = model.tokenize([source_i],max_length=512,mode="<encoder-only>")
        source_ids = torch.tensor(tokens_ids).to(device)
        with torch.no_grad():
            embedding = model(source_ids)[1]
            embedding = torch.nn.functional.normalize(embedding, p=2, dim=1)
            if i == 0:
                embeddings = embedding
            else:
                embeddings = torch.vstack((embeddings, embedding))
        i += 1
        print(f"\r[SemanticSimilarity] {int(100*i/len_classes_info)}%", end="", flush=True)
    print(f"\r[SemanticSimilarity] 100%", flush=True)

    # --- DEBUG SECTION

    # --- 1. upload classes.json to google colab and run the notebook
    # --- 2. download embeddings.npz from google colab and put it in working directory
    # --- 3. comment the following line:
    return sp.csr_matrix(embeddings.to('cpu'))
    # --- 4. uncomment the following line:
    # return sp.load_npz('semantic.npz')

    # --- DEBUG SECTION


def tf_idf_semantic_similarity(classes_info):
    from sklearn.feature_extraction.text import TfidfVectorizer
    from HDBSCAN.preprocess import preprocess
    from nltk import download
    download('punkt')
    download('punkt_tab')
    download('stopwords')
    corpus = []
    for clss in classes_info:
        corpus.append(preprocess(' '.join(classes_info[clss]['words'])))
    vectorizer = TfidfVectorizer()
    tf_idf_vectors = vectorizer.fit_transform(corpus)
    return tf_idf_vectors
