from argparse import ArgumentParser
from os import pathsep
from subprocess import run
from json import load
import pandas as pd


parser = ArgumentParser()
parser.add_argument("-m", "--method", dest="method", help="choose between Mo2oM, Mono2Multi, and HDBSCAN", required=True)
parser.add_argument("-f", "--file", dest="file_path", help="path to the java source code file", required=True)
parser.add_argument("-r", "--result", dest="result_path", help="path to the results file", required=True)
args = parser.parse_args()

with open(args.result_path) as f:
	data = load(f)
df = pd.DataFrame(data)
df["overall"] = 0.75 + 0.25 * df["SM"] - 0.25 * df["ICP"] - 0.25 * df["NED"] - 0.25 * df["IFN"]
df.sort_values(by="overall", ascending=False, inplace=True)

libs = f"{args.method}/JavaParser/lib/javaparser-core-3.25.5-SNAPSHOT.jar"+pathsep+f"{args.method}/JavaParser/lib/json-20230618.jar"
json_path = f"{args.method}/JavaParser/classes.json"
run(['java', '-cp', libs, f'{args.method}/JavaParser/Parser.java', args.file_path, json_path]) 
with open(json_path, "rt") as classes_file:
	classes_info = load(classes_file)
class_names = list(classes_info)

if args.method == "Mono2Multi":
	microservices = df.iloc[0, 3]
else:
	microservices = df.iloc[0, 2]
microservices_n = max(max(_) for _ in microservices)
file_content = ""
for ms in range(microservices_n):
	this_ms_class_names = []
	for clss_i, clss_ms in enumerate(microservices):
		if ms in clss_ms:
			this_ms_class_names.append(class_names[clss_i])
	file_content += f"\nMicroservice {ms+1}: {this_ms_class_names}"

with open(f"{args.result_path.split("/")[-1].replace("_output.json", "")}.txt", "wt") as f:
	f.write(file_content)
