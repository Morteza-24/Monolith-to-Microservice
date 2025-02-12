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
    a = data[0]["alpha"]
    l_alphas = [a]
    l_epsilons = []
    le_done = False
    for i in data:
        if i["alpha"] != a:
            values.append([])
            a = i["alpha"]
            l_alphas.append(i["alpha"])
            le_done = True
        if not le_done:
            l_epsilons.append(i["epsilon"])
        values[-1].append(i[measure])
    values = np.array(values).T
    return values, l_epsilons, l_alphas
