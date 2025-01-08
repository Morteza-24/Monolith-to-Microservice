from EvaluationMeasures import SM
from subprocess import run
from os import pathsep
import json


def fix(mthd, proj):
	source_code_path = f"TestProjects/{proj}/OneFileSource.java"
	libs = f"{mthd}/JavaParser/lib/javaparser-core-3.25.5-SNAPSHOT.jar" + pathsep + f"{mthd}/JavaParser/lib/json-20230618.jar"
	json_path = "classes.json"
	run(['java', '-cp', libs, f'{mthd}/JavaParser/Parser.java', source_code_path, json_path]) 
	with open(json_path, "rt") as classes_file:
		classes_info = json.load(classes_file)
	for clss in classes_info:
		for call in classes_info[clss]["method_calls"]:
			for other_clss in classes_info:
				if call["method_name"] in classes_info[other_clss]["methods"]:
					call["class_name"] = other_clss
					if clss == other_clss:
						break
	with open(f"ExperimentalResults/{mthd}/{mthd}_{proj}_output.json", "rt") as results_file:
		results = json.load(results_file)
	for result in results:
		clusters = result["microservices"]
		result["SM"] = SM(clusters, classes_info)
	with open("tmp_output.json", "w") as output_file:
		json.dump(results, output_file, indent=2)
	in_ms = False
	inn_ms = False
	with (open("tmp_output.json", "rt") as in_f,
		open(f"ExperimentalResults/{mthd}/{mthd}_{proj}_output.json", "wt") as out_f):
		for line in in_f.readlines():
			if line.endswith("[\n"):
				if in_ms:
					inn_ms = True
				elif "microservices" in line:
					in_ms = True
					out_f.write(line[:-1])
					continue
			elif line.endswith("],\n") or line.endswith("]\n"):
				if inn_ms:
					inn_ms = False
				else:
					in_ms = False
					out_f.write(line.lstrip())
					continue
			if in_ms:
				out_f.write(line.strip())
			else:
				out_f.write(line)
	run(["rm", "tmp_output.json"])


projs = ["JPetStore", "DayTrader", "AcmeAir", "Plants"]
mthds = ["HDBSCAN", "Mono2Multi", "Mo2oM"]
for proj in projs:
	for mthd in mthds:
		print(proj, mthd)
		fix(mthd, proj)
