# from unixcoder import UniXcoder
# from adjustText import adjust_text
# from sklearn.manifold import TSNE
# import numpy as np
# import torch
import matplotlib.patches as mpatches
import matplotlib.lines as mlines
import matplotlib.pyplot as plt
from subprocess import run
from os import pathsep
import seaborn as sns
import networkx as nx
import json


MODEL = "Mo2oM"
PROJECT = "DayTrader"
MICROSERVICES = [  # you can get these from ./call_graph_expt.py
['TradeAppJSF', 'TradeScenarioServlet', 'Log', 'TradeServletAction', 'TradeSLSBLocal', 'QuoteDataBean', 'TradeConfigJSF', 'TradeAppServlet'], ['Log', 'TradeWebContextListener', 'QuoteDataBean', 'TradeConfigJSF', 'Handler', 'PingJDBCRead'], ['PingSession3Object', 'Log', 'ActionDecoder', 'TradeSLSBBean', 'QuoteDataBean', 'MarketSummarySingleton', 'TradeConfigJSF', 'OrderDataJSF'], ['Log', 'MarketSummaryJSF', 'QuoteDataBean', 'TradeConfigJSF']
]


ms_ranges = []
len_ranges = 0
for ms in MICROSERVICES:
    ms_ranges.append((len_ranges, len_ranges + len(ms)))
    len_ranges += len(ms)

nodes_labels = []
for ms in MICROSERVICES:
    nodes_labels.extend(ms)

libs = "../../Mo2oM/JavaParser/lib/javaparser-core-3.25.5-SNAPSHOT.jar"+pathsep+"../../Mo2oM/JavaParser/lib/json-20230618.jar"
json_path = "../../Mo2oM/JavaParser/classes.json"
run(['java', '-cp', libs, "../../Mo2oM/JavaParser/Parser.java", f"../../TestProjects/{PROJECT}/OneFileSource.java", json_path])
with open(json_path, "rt") as classes_file:
    classes_info = json.load(classes_file)
immutables = []
current_ms = 0
for i, clss in enumerate(nodes_labels):
    if i == ms_ranges[current_ms][1]:
        current_ms += 1
    for j, call in enumerate(classes_info[clss]["method_calls"]):
        if (clss, j) in immutables:
            continue
        for other_clss in nodes_labels:
            if call["method_name"] in classes_info[other_clss]["methods"]:
                call["class_name"] = other_clss
                if clss == other_clss:
                    break
                if call["class_name"] in nodes_labels[ms_ranges[current_ms][0]:ms_ranges[current_ms][1]]:
                    immutables.append((clss, j))
                    break

inter = 0
intra = 0
all_edges = []
current_ms = 0
for i, clss in enumerate(nodes_labels):
    if i == ms_ranges[current_ms][1]:
        current_ms += 1
    for call in classes_info[clss]["method_calls"]:
        if call["class_name"] in nodes_labels:
            if call["class_name"] == clss:
                continue
            if call["class_name"] in MICROSERVICES[current_ms]:
                idx = ms_ranges[current_ms][0] + nodes_labels[ms_ranges[current_ms][0]:ms_ranges[current_ms][1]].index(call["class_name"])
                inter += 1
                all_edges.append((i, idx))
            else:
                all_edges.append((i, nodes_labels.index(call["class_name"])))
                intra += 1
edges = [edge for edge in all_edges if all_edges.count(edge) > 1]

labels = {}
current_ms = 0
for i in range(len(nodes_labels)):
    if i == ms_ranges[current_ms][1]:
        current_ms += 1
    labels[i] = current_ms

sns.set_style("whitegrid")
plt.rcParams.update({
    'font.family': 'serif',
    'font.serif': ['Computer Modern'],
    "text.usetex": True
})

G = nx.Graph()
G.add_nodes_from([(i, {"label": lbl}) for i, lbl in enumerate(nodes_labels)])
G.add_edges_from(edges)

node_colors = []
for i, ms in enumerate(MICROSERVICES):
    node_colors.extend([sns.color_palette("Set2")[i]] * len(ms))
edge_colors = ['#B0B0B0' if labels[edge[0]] == labels[edge[1]] else '#FF4C4C' for edge in G.edges()]
print("inter:", edge_colors.count('#B0B0B0'), "- intra:", edge_colors.count('#FF4C4C'))

def format_label(label):
    result = label[0]
    for i in range(1, len(label)):
        if label[i].isupper() and not label[i-1].isupper():
            result += "\n" + label[i]
        else:
            result += label[i]
    return result

# pos = {}
# num_clusters = len(MICROSERVICES)
# cluster_centers = {i: (np.cos(2 * np.pi * i / num_clusters), np.sin(2 * np.pi * i / num_clusters))
#                    for i in range(len(MICROSERVICES))}
# for node in G.nodes():
#     cluster = labels[node]
#     offset = np.random.normal(0, 0.4, 2)
#     pos[node] = np.array(cluster_centers[cluster]) + offset

supergraph = nx.cycle_graph(len(MICROSERVICES))
superpos = nx.spring_layout(supergraph, scale=3, seed=42)
centers = list(superpos.values())
pos = {}
for center, comm in zip(centers, [list(range(*ms_ranges[i])) for i in range(len(MICROSERVICES))]):
    pos.update(nx.spring_layout(nx.subgraph(G, comm), center=center, k=3.5, seed=42))

# pos = nx.arf_layout(G)

nx.draw_networkx_nodes(G, pos, node_color=node_colors, alpha=1, node_size=500)#, node_shape='o')
nx.draw_networkx_edges(G, pos, edge_color=edge_colors, width=1.5, arrows=True)#, min_source_margin=30, min_target_margin=30)
nx.draw_networkx_labels(
    G,
    pos,
    labels={node: format_label(data['label']) for node, data in G.nodes(data=True)},
    font_size=4,
    font_color='black',
    font_weight='normal',
    horizontalalignment='center',
    verticalalignment='center'
)
plt.axis('off')

node_legend_handles = [
    # mlines.Line2D([], [], color=sns.color_palette("Set2")[0], marker='o', linestyle='None', markersize=6, label='A Class from Microservice 1'),
    # mlines.Line2D([], [], color=sns.color_palette("Set2")[1], marker='o', linestyle='None', markersize=6, label='A Class from Microservice 2'),
    # mlines.Line2D([], [], color=sns.color_palette("Set2")[2], marker='o', linestyle='None', markersize=6, label='A Class from Microservice 3'),
    # mlines.Line2D([], [], color=sns.color_palette("Set2")[3], marker='o', linestyle='None', markersize=6, label='A Class from Microservice 4'),
]
edge_legend_handles = [
    mlines.Line2D([], [], color='#B0B0B0', label='More than one inter-service call'),
    mlines.Line2D([], [], color='#FF4C4C', label='More than one intra-service call'),
]
handles = node_legend_handles + edge_legend_handles
plt.legend(handles=handles, loc='upper left', fontsize=5, frameon=True, labelspacing=1.1, borderpad=0.7)

plt.savefig(f"{MODEL}_{PROJECT}_Call_Graph.pdf", bbox_inches='tight')#, dpi=300)
plt.show()

# classes = list(set(nodes_labels))
# torch.manual_seed(42)
# torch.cuda.manual_seed(42)
# torch.cuda.manual_seed_all(42)
# torch.backends.cudnn.deterministic = True
# torch.use_deterministic_algorithms(True)
# np.random.seed(42)
# device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
# print(f"[SemanticSimilarity] using device {device}", flush=True)
# model = UniXcoder("microsoft/unixcoder-base")
# model.to(device)
# len_classes = len(classes)
# i = 0
# for clss in classes_info:
#     if clss in classes:
#         source_i = classes_info[clss]["source"]
#         tokens_ids = model.tokenize([source_i],max_length=512,mode="<encoder-only>")
#         source_ids = torch.tensor(tokens_ids).to(device)
#         with torch.no_grad():
#             embedding = model(source_ids)[1]
#             embedding = torch.nn.functional.normalize(embedding, p=2, dim=1)
#             if i == 0:
#                 embeddings = embedding
#             else:
#                 embeddings = torch.vstack((embeddings, embedding))
#         i += 1
#         print(f"\r[SemanticSimilarity] {int(100*i/len_classes)}%", end="", flush=True)
# print(f"\r[SemanticSimilarity] 100%", flush=True)

# tsne = TSNE(n_components=2, random_state=42, perplexity=3)
# proj = tsne.fit_transform(embeddings)

# all_colors = {node: [] for node in classes}
# for i, ms in enumerate(MICROSERVICES):
#     for node in ms:
#         all_colors[node].append(sns.color_palette("Set2")[i])
# colors = []
# for node_colors in all_colors.values():
#     colors.append(np.average(node_colors, axis=0))
# print(colors)

# # plt.scatter(proj[:, 0], proj[:, 1], c=colors, s=100)
# # plt.figure(figsize=(6, 4), dpi=300)
# scatter = sns.scatterplot(
#     x=proj[:, 0], y=proj[:, 1],
#     hue=list(range(len(classes))),
#     palette=colors,
#     s=350, alpha=0.8, linewidth=0.5#, edgecolor='k'
# )
# plt.axis('off')
# plt.legend([],[], frameon=False)
# texts = [plt.text(proj[i, 0], proj[i, 1], classes[i], fontsize=4, ha='center') for i in range(len(classes))]
# adjust_text(texts, arrowprops=dict(arrowstyle='-', color='gray', lw=0.5))
# handles, _ = scatter.get_legend_handles_labels()
# plt.savefig("tsne.pdf", dpi=300, bbox_inches='tight')
# plt.show()
