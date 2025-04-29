import os
import subprocess
import json


def count_calls(project: str, microservices: list[list[str]]):
	"""
	Extracts method calls between classes in a project.
	This function processes the source code of a project to determine the method calls between
	classes. It takes into account the organization of classes into microservices. The output is
	a list of calls, where each call is represented as a pair of class indices [a, b], indicating
	a method call from class `a` to class `b`. Class indices start from 0 for the first class of the
	first microservice, and are incremented sequentially for each subsequent class until the last
	class of the last microservice.
	Args:
		project (str): The name of the project to analyze. The project is expected to
			have its source code located in a predefined directory structure. Allowed
			project names are: "JPetStore", "DayTrader", "AcmeAir", and "Plants".
		microservices (list[list[str]]): A list of microservices, where each microservice
			is represented as a list of class names. The order of classes in this list
			determines their indices.
	Returns:
		list[tuple[int, int]]: A list of tuples representing method calls between classes.
			Each tuple (a, b) indicates a method call from class `a` to class `b`, where
			class indices are determined based on their order in the `microservices` input.
	Notes:
		- Run this code from the directory its file (count_calls.py) is located in.
		- Classes are indexed sequentially starting from 0, with indices assigned based
		  on their order in the `microservices` input.
	"""

	cwd = os.getcwd().split(os.sep)
	if cwd[-3:] != ["Monolith-to-Microservice", "ExperimentalResults", "utils"]:
		raise ValueError("Please run this code from the 'Monolith-to-Microservice/ExperimentalResults/utils' directory.")
	if project not in ["JPetStore", "DayTrader", "AcmeAir", "Plants"]:
		raise ValueError("Project not supported. Supported projects are: JPetStore, DayTrader, AcmeAir, Plants.")
	ms_ranges = []
	len_ranges = 0
	for ms in microservices:
		ms_ranges.append((len_ranges, len_ranges + len(ms)))
		len_ranges += len(ms)
	nodes_labels = []
	for ms in microservices:
		nodes_labels.extend(ms)
	libs = "../../Mo2oM/JavaParser/lib/javaparser-core-3.25.5-SNAPSHOT.jar"+os.pathsep+"../../Mo2oM/JavaParser/lib/json-20230618.jar"
	json_path = "../../Mo2oM/JavaParser/classes.json"
	subprocess.run(['java', '-cp', libs, "../../Mo2oM/JavaParser/Parser.java", f"../../TestProjects/{project}/OneFileSource.java", json_path])
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
	all_calls = []
	current_ms = 0
	for i, clss in enumerate(nodes_labels):
		if i == ms_ranges[current_ms][1]:
			current_ms += 1
		for call in classes_info[clss]["method_calls"]:
			if call["class_name"] in nodes_labels:
				if call["class_name"] == clss:
					continue
				if call["class_name"] in microservices[current_ms]:
					idx = ms_ranges[current_ms][0] + nodes_labels[ms_ranges[current_ms][0]:ms_ranges[current_ms][1]].index(call["class_name"])
					all_calls.append((i, idx))
				else:
					all_calls.append((i, nodes_labels.index(call["class_name"])))
	return all_calls


# example usage

microservices = [
	['TradeAppJSF', 'TradeScenarioServlet', 'Log', 'TradeServletAction', 'TradeSLSBLocal', 'QuoteDataBean', 'TradeConfigJSF', 'TradeAppServlet'], ['Log', 'TradeWebContextListener', 'QuoteDataBean', 'TradeConfigJSF', 'Handler', 'PingJDBCRead'], ['PingSession3Object', 'Log', 'ActionDecoder', 'TradeSLSBBean', 'QuoteDataBean', 'MarketSummarySingleton', 'TradeConfigJSF', 'OrderDataJSF'], ['Log', 'MarketSummaryJSF', 'QuoteDataBean', 'TradeConfigJSF']
]
print(count_calls("DayTrader", microservices))
