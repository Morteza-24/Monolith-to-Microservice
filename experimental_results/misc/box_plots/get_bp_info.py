from argparse import ArgumentParser
import pandas as pd
import numpy as np
import json


projects = ["JPetStore", "DayTrader", "AcmeAir", "Plants"]
models = ["Mo2oM", "HDBSCAN", "CoGCN", "Mono2Micro", "FoSCI", "MEM", "Bunch"]#, "Mono2Multi"
metrics = ["SM", "ICP", "IFN", "NED"]

df = pd.DataFrame()
for project in projects:
	for model in models:
		with open(f"../results/{model}/{model}_{project}.json", "r") as f:
			data = json.load(f)
			new_df = pd.DataFrame(data)[metrics]
			new_df["Project"] = project
			new_df["Model"] = model
			df = pd.concat([df, new_df])


def calculate_boxplot_stats(data):
    Q1 = np.percentile(data, 25)
    Q3 = np.percentile(data, 75)
    IQR = Q3 - Q1
    median = np.median(data)
    
    lower_bound = Q1 - 1.5 * IQR
    upper_bound = Q3 + 1.5 * IQR
    outliers = [x for x in data if x < lower_bound or x > upper_bound]
    
    return {
        "median": median,
        "IQR": IQR,
        "num_outliers": len(outliers)
    }


for metric in metrics:
	print(metric)
	for proj in projects:
		print(" "*4+proj)
		for model in models:
			print(" "*8+model)
			data = df[(df["Project"] == proj) & (df["Model"] == model)][metric]
			stats = calculate_boxplot_stats(data)
			for k, v in stats.items():
				print(" "*12+f"{k}:", v)
