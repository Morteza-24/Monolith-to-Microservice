from argparse import ArgumentParser
import pandas as pd
import numpy as np
import json


projects = ["JPetStore", "DayTrader", "AcmeAir", "Plants"]
models = ["Mo2oM", "Mono2Multi", "HDBSCAN", "CoGCN", "Mono2Micro", "FoSCI", "MEM", "Bunch"]
metrics = ["SM", "ICP", "IFN", "NED"]

parser = ArgumentParser()
parser.add_argument("-m1", "--model-1", dest="model_1", choices=models, required=True)
parser.add_argument("-m2", "--model-2", dest="model_2", choices=models, required=True)
args = parser.parse_args()

df = pd.DataFrame()
for project in projects:
	for model in models:
		with open(f"results/{model}/{model}_{project}.json", "r") as f:
			data = json.load(f)
			new_df = pd.DataFrame(data)[metrics]
			new_df["Project"] = project
			new_df["Model"] = model
			df = pd.concat([df, new_df])


for metric in metrics:
	print(metric)
	for proj in projects:
		new = np.median(df[(df["Project"] == proj) & (df["Model"] == args.model_1)][metric])
		old = np.median(df[(df["Project"] == proj) & (df["Model"] == args.model_2)][metric])
		new = float(new)
		old = float(old)
		print(f"    {proj}: new-old = {(new - old)}")
		if old != 0:
			print(f"    {proj}: 100*(new-old)/old = {100*(new - old)/old}")
