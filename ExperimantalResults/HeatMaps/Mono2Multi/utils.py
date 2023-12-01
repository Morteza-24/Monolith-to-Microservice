import matplotlib.pyplot as plt
import matplotlib
import numpy as np
import json


def heatmap(data, row_labels, col_labels, ax, cbarlabel="", **kwargs):
    im = ax.imshow(data, **kwargs)
    cbar = ax.figure.colorbar(im, ax=ax)
    cbar.ax.set_ylabel(cbarlabel, rotation=-90, va="bottom")
    ax.set_xticks(np.arange(data.shape[1]), labels=col_labels)
    ax.set_yticks(np.arange(data.shape[0]), labels=row_labels)
    ax.tick_params(top=True, bottom=False,
                   labeltop=True, labelbottom=False)
    plt.setp(ax.get_xticklabels(), rotation=0,rotation_mode="anchor")
    ax.spines[:].set_visible(False)
    ax.set_xticks(np.arange(data.shape[1]+1)-.5, minor=True)
    ax.set_yticks(np.arange(data.shape[0]+1)-.5, minor=True)
    ax.grid(which="minor", color="w", linestyle='-', linewidth=3)
    ax.tick_params(which="minor", bottom=False, left=False)
    return im, cbar


def annotate_heatmap(im, valfmt="{x:.5f}", textcolors=("black", "white"), **textkw):
    data = im.get_array()
    threshold = im.norm(data.max())/2.
    kw = dict(horizontalalignment="center",
              verticalalignment="center")
    kw.update(textkw)
    if isinstance(valfmt, str):
        valfmt = matplotlib.ticker.StrMethodFormatter(valfmt)
    texts = []
    for i in range(data.shape[0]):
        for j in range(data.shape[1]):
            kw.update(color=textcolors[int(im.norm(data[i, j]) > threshold)])
            text = im.axes.text(j, i, valfmt(data[i, j], None), **kw)
            texts.append(text)
    return texts


def prepare_data(path, measure):
    with open(path, "rt") as f:
        data = json.load(f)
    values = [[]]
    c = data[0]["n_clusters"]
    l_clusters = [c]
    l_thresholds = []
    lt_done = False
    for i in data:
        if i["n_clusters"] != c:
            values.append([])
            c = i["n_clusters"]
            l_clusters.append(i["n_clusters"])
            lt_done = True
        if not lt_done:
            l_thresholds.append(i["threshold"])
        actual_nc = len(set([_ for ms in i["microservices"] for _ in ms]))
        if actual_nc != i["n_clusters"]:
            print(f'({i["threshold"]}, {i["n_clusters"]}) actaully has {actual_nc} cluters')
        values[-1].append(i[measure])   
    values = np.array(values).T
    return values, l_thresholds, l_clusters
