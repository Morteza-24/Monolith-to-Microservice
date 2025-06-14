from sklearn.preprocessing import MinMaxScaler
import pandas as pd
import json


def _get_tops(df, n_top, metric):
	ascending = False if metric == "SM" else True
	df_sorted = df.sort_values(by=metric, ascending=ascending)
	# n_top = int(len(df) * percent)
	top = df_sorted.head(n_top)
	thresholds_top = top['threshold'].tolist()
	return set(sorted(thresholds_top))


def get_optimal_range(method, metrics, verbose=False):
	df = pd.DataFrame()
	for proj in ["JPetStore", "DayTrader", "AcmeAir", "Plants"]:
		with open(f"../results/{method}/{method}_{proj}.json") as f:
			data = json.load(f)
		new_df = pd.DataFrame(data).drop(["microservices", "n_clusters"], axis=1)
		new_df = new_df.groupby("threshold", as_index=False).median()
		df = pd.concat([df, new_df])
	df = df.groupby("threshold", as_index=False).median()
	normalized_df = pd.DataFrame(MinMaxScaler().fit_transform(df.values), columns=df.columns)
	df['score'] = normalized_df["SM"] - normalized_df["ICP"] - normalized_df["IFN"] - normalized_df["NED"]
	if verbose:
		print(df, end="\n\n")

	top_thresholds = {}
	for metric in metrics:
		n_top = metrics[metric]
		top_thresholds[metric] = _get_tops(df, n_top, metric)
		if verbose:
			print(f"top thresholds for {metric}: {sorted(list(top_thresholds[metric]))}")

	intersection_thresholds = sorted(list(set.intersection(*top_thresholds.values())))
	if verbose:
		print(f"\nintersection: {intersection_thresholds}\n")
	result_df = df[df['threshold'].isin(intersection_thresholds)]
	print(result_df)


methods = ["Structural", "UniXcoder", "Combined"]
n_top = {"Structural": 11, "UniXcoder": 11, "Combined": 13}
for method in methods:
	metrics = {"SM": n_top[method], "ICP": n_top[method], "IFN": n_top[method], "NED": n_top[method]}
	print(method)
	get_optimal_range(f"Mo2oM_{method}", metrics)
	print()
# get_optimal_range("Mo2oM_Combined", {"SM": 13, "ICP": 13, "IFN": 13, "NED": 13}, verbose=True)


# Structural [0.15 - 0.20]
# UniXcoder [0.10 - 0.25]
# Combined [0.05 - 0.20]
