from numpy import zeros
from transformers import RobertaTokenizer, RobertaModel
import torch
from torch.nn import CosineSimilarity


def calls(ci, cj, classes_info):
    return len([
        _ for _ in classes_info[ci]["method_calls"]
        if _["method_name"] in classes_info[cj]["methods"]
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


def semantic_similarity(ci, cj, classes_info, tokenizer, model):
    source_i = classes_info[ci]["source"]
    source_j = classes_info[cj]["source"]

    tokens_i = tokenizer.encode(source_i, add_special_tokens=True, max_length=512)
    tokens_j = tokenizer.encode(source_j, add_special_tokens=True, max_length=512)

    max_length = max(len(tokens_i), len(tokens_j))
    padded_tokens_i = torch.tensor(tokens_i + [tokenizer.pad_token_id] * (max_length - len(tokens_i)))
    padded_tokens_j = torch.tensor(tokens_j + [tokenizer.pad_token_id] * (max_length - len(tokens_j)))

    batched_tokens = torch.stack([padded_tokens_i, padded_tokens_j])

    with torch.no_grad():
        embeddings = model(batched_tokens)[0]

    sim = CosineSimilarity(dim=-1)
    cosine = sim(embeddings[0], embeddings[1])
    average_similarity = torch.mean(cosine)
    return average_similarity.item()


def class_similarity(alpha, classes_info):
    # get the models ready
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    tokenizer = RobertaTokenizer.from_pretrained("microsoft/codebert-base")
    model = RobertaModel.from_pretrained("microsoft/codebert-base")
    model.to(device)

    class_similarity_matrix = zeros((len(classes_info), len(classes_info)))
    len_classes_info = len(classes_info)

    for i in range(len_classes_info):
        for j in range(i+1,len_classes_info):
            ci = list(classes_info.keys())[i]
            cj = list(classes_info.keys())[j]
            class_similarity_matrix[i][j] = 1 - (alpha*structural_similarity(ci, cj, classes_info) + (1-alpha)*semantic_similarity(ci, cj, classes_info, tokenizer, model))
        print(f"\r[SimilarityAnalysis] {int(100*i/len_classes_info):02d}%", end="", flush=True)
    print(f"\r[SimilarityAnalysis] 100%", flush=True)

    return class_similarity_matrix + class_similarity_matrix.T
