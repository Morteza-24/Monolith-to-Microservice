from numpy import zeros
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
    structural_similarity_matrix = zeros((len_classes_info, len_classes_info))
    for i in range(len_classes_info):
        ci = list(classes_info.keys())[i]
        for j in range(i+1,len_classes_info):
            cj = list(classes_info.keys())[j]
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

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
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
    return sp.csr_matrix(embeddings)
    # --- 4. uncomment the following line:
    # return sp.load_npz('semantic.npz')

    # --- DEBUG SECTION