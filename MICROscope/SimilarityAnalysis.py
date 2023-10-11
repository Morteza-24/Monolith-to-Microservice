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
    from torch.nn import CosineSimilarity
    from transformers import RobertaTokenizer, RobertaModel

    # get the models ready
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    tokenizer = RobertaTokenizer.from_pretrained("microsoft/codebert-base")
    model = RobertaModel.from_pretrained("microsoft/codebert-base")
    model.to(device)

    semantic_similarity_matrix = zeros((len(classes_info), len(classes_info)))
    len_classes_info = len(classes_info)

    for i in range(len_classes_info):
        ci = list(classes_info.keys())[i]
        source_i = classes_info[ci]["source"]
        tokens_i = tokenizer.encode(source_i, add_special_tokens=True, padding='max_length', truncation=True, return_tensors='pt')
        for j in range(i+1,len_classes_info):
            cj = list(classes_info.keys())[j]
            source_j = classes_info[cj]["source"]
            tokens_j = tokenizer.encode(source_j, add_special_tokens=True, padding='max_length', truncation=True, return_tensors='pt')

            tokens = torch.stack([tokens_i[0], tokens_j[0]])
            with torch.no_grad():
                embeddings = model(tokens)[0]

            sim = CosineSimilarity(dim=-1)
            cosine = sim(embeddings[0], embeddings[1])
            average_similarity = torch.mean(cosine)
            semantic_similarity_matrix[i][j] = average_similarity.item()
            print(f"\r[SemanticSimilarity] {int(100*((len_classes_info*i)+(j+1))/(len_classes_info**2))}%", end="", flush=True)
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
    # semantic_similarity_matrix = load("Test_Projects/JPetStore/ssm.npy")

    # --- DEBUG SECTION

    for i in range(len_classes_info):
        for j in range(i+1,len_classes_info):
            class_similarity_matrix[i][j] = 1 - (alpha*structural_similarity_matrix[i][j] + (1-alpha)*semantic_similarity_matrix[i][j])

    return class_similarity_matrix + class_similarity_matrix.T
