import os
import json
import shutil
import subprocess
from EvaluationMeasures import *


src = "Mo2oM"
projects = ["JPetStore", "DayTrader", "AcmeAir", "Plants"]
measures = {"SM": SM, "ICP": ICP, "IFN": IFN, "NED": NED}
base_dir = os.path.dirname(os.path.realpath(__file__))
libs = os.path.join(base_dir, "Mo2oM/JavaParser/lib/javaparser-core-3.25.5-SNAPSHOT.jar")+os.pathsep+os.path.join(base_dir, "Mono2Multi/JavaParser/lib/json-20230618.jar")

for project in projects:
	print("project:", project)
	print("parsing the code...", end=" ", flush=True)
	shutil.rmtree(".data/", ignore_errors=True)
	os.makedirs(os.path.join(base_dir, f".data/{project}"), exist_ok=True)
	json_path = os.path.join(base_dir, f".data/{project}/classes.json")
	subprocess.run(['java', '-cp', libs, os.path.join(base_dir, 'Mo2oM/JavaParser/Parser.java'), f"test_projects/{project}/OneFileSource.java", json_path]) 
	with open(json_path, "rt") as classes_file:
		classes_info = json.load(classes_file)
	print("done!")

	print("analyzing method calls...", end=" ", flush=True)
	for clss in classes_info:
		for call in classes_info[clss]["method_calls"]:
			for other_clss in classes_info:
				if call["method_name"] in classes_info[other_clss]["methods"]:
					call["class_name"] = other_clss
					if clss == other_clss:
						break
	print("done!")

	with open(f"experimental_results/results/{src}/{src}_{project}.json", "rt") as file:
		data = json.load(file)

	new_data = []
	for d in data:
		for measure in ["SM", "IFN", "ICP", "NED"]:
			if measure in ["SM", "IFN", "ICP"]:
				d[measure] = measures[measure](d["microservices"], classes_info)
			else:
				d[measure] = measures[measure](d["microservices"])
		new_data.append(d)

	with open(f"experimental_results/results/{src}/tmp_{src}_{project}.json", "wt") as output_file:
		json.dump(new_data, output_file, indent=2)
	in_ms = False
	inn_ms = False
	with (open(f"experimental_results/results/{src}/tmp_{src}_{project}.json", "rt") as in_f,
		open(f"experimental_results/results/{src}/{src}_{project}.json.f", "wt") as out_f):
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
	subprocess.run(["rm", f"experimental_results/results/{src}/tmp_{src}_{project}.json"])
