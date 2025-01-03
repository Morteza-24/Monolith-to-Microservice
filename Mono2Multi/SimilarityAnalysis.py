from numpy import zeros


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
    return structural_similarity_matrix


def semantic_similarity(classes_info):
    import torch
    from Mono2Multi.unixcoder import UniXcoder

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"[SemanticSimilarity] using device {device}", flush=True)
    model = UniXcoder("microsoft/unixcoder-base")
    model.to(device)

    semantic_similarity_matrix = zeros((len(classes_info), len(classes_info)))
    len_classes_info = len(classes_info)
    i = 0
    for clss in classes_info:
        source_i = classes_info[clss]["source"]
        tokens_ids = model.tokenize([source_i],max_length=512,mode="<encoder-only>")
        source_ids = torch.tensor(tokens_ids).to(device)
        with torch.no_grad():
            embedding = model(source_ids)[1]
            embedding = torch.nn.functional.normalize(embedding, p=2, dim=1)
        classes_info[clss]["norm_embedding"] = embedding
        i += 1
        print(f"\r[SemanticSimilarity] {int(100*i/len_classes_info)}%", end="", flush=True)

    for i in range(len_classes_info):
        ci = list(classes_info.keys())[i]
        norm_embedding_i = classes_info[ci]["norm_embedding"]
        for j in range(i+1,len_classes_info):
            cj = list(classes_info.keys())[j]
            norm_embedding_j = classes_info[cj]["norm_embedding"]
            semantic_similarity_matrix[i][j] = torch.einsum("ac,bc->ab",norm_embedding_i,norm_embedding_j)

    print(f"\r[SemanticSimilarity] 100%", flush=True)
    return semantic_similarity_matrix


def class_similarity(alpha, classes_info):
    structural_similarity_matrix = structural_similarity(classes_info)
    class_similarity_matrix = zeros((len(classes_info), len(classes_info)))
    len_classes_info = len(classes_info)

    if alpha == 1:
        for i in range(len_classes_info):
            for j in range(i+1,len_classes_info):
                class_similarity_matrix[i][j] = 1 - structural_similarity_matrix[i][j]
        return class_similarity_matrix + class_similarity_matrix.T

    # --- DEBUG SECTION

    # --- 1. upload classes.json to google colab and run the notebook
    # --- 2. download ssm.npy from google colab and put it in working directory
    # --- 3. comment the following line:
    semantic_similarity_matrix = semantic_similarity(classes_info)
    # --- 4. uncomment the following lines:
    # from numpy import load
    # semantic_similarity_matrix = load("TestProjects/JPetStore/ssm.npy")

    # --- DEBUG SECTION

    for i in range(len_classes_info):
        for j in range(i+1,len_classes_info):
            class_similarity_matrix[i][j] = 1 - (alpha*structural_similarity_matrix[i][j] + (1-alpha)*semantic_similarity_matrix[i][j])

    return class_similarity_matrix + class_similarity_matrix.T
