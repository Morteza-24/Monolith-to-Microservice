from EvaluationMeasures import *
from argparse import ArgumentParser
import subprocess
import shutil
import json
import os


parser = ArgumentParser(
    prog='python ground_truth_evaluator.py',
    description='a script to calculate metrics on microservices-based source codes (ground truths).',
    epilog='example usage: python ground_truth_evaluator.py -p test_projects/PetClinic/src/ -e SM ICP IFN NED -o output.json')
parser.add_argument("-p", "--project", dest="project_directory", required=True,
                    help="path to the microservices-based java project directory.")
parser.add_argument("-e", "--evaluation-measure", choices=["SM", "ICP", "IFN", "NED"], nargs="*", required=True,
                    help="The ground truth microservices must be in different directories of your project's root directory.")
parser.add_argument("-o", "--output-file", dest="output_file", required=True,
                    help="output file name to save results in")
args = parser.parse_args()

measures = {"SM": SM, "ICP": ICP, "IFN": IFN, "NED": NED}
base_dir = os.path.dirname(os.path.realpath(__file__))

def merge_java_files(src_dir, dest_file):
    with open(dest_file, 'w') as outfile:
        for root, dirs, files in os.walk(src_dir):
            for filename in files:
                if filename.endswith('.java'):
                    filepath = os.path.join(root, filename)
                    with open(filepath) as infile:
                        lines = infile.readlines()
                        for line in lines:
                            if not line.startswith('package'):
                                outfile.write(line)
                    outfile.write('\n')

if args.project_directory.endswith("/"):
    args.project_directory = args.project_directory[:-1]
project_dir_name = args.project_directory.split('/')[-1]
true_ms_dirs = next(os.walk(args.project_directory))[1]
print("the following microservices are detected:\n\t", end="", flush=True)
print(*true_ms_dirs, sep="\n\t", flush=True)

if os.path.isdir(".data/"):
    if input("the previous data directory will be deleted; proceed? (Y/n): ") in ["n", "no", "N", "No", "NO"]:
        print("operation cancelled.")
        exit()
    shutil.rmtree(".data/", ignore_errors=True)

print("analyzing microservices... 0%", end="", flush=True)
true_ms_classnames = []
libs = os.path.join(base_dir, "Mo2oM/JavaParser/lib/javaparser-core-3.25.5-SNAPSHOT.jar")+os.pathsep+os.path.join(base_dir, "Mono2Multi/JavaParser/lib/json-20230618.jar")
os.makedirs(os.path.join(base_dir, f".data/{project_dir_name}"), exist_ok=True)
for i, directory in enumerate(true_ms_dirs):
    if directory.endswith("/"):
        directory = directory[:-1]
    json_path = os.path.join(base_dir, f".data/{project_dir_name}/{directory}.json")
    subprocess.run(['java', '-cp', libs, os.path.join(base_dir, 'Mo2oM/JavaParser/ClassScanner.java'), os.path.join(args.project_directory, directory), json_path])
    with open(json_path, "rt") as classes_file:
        true_ms_classnames.append(json.load(classes_file)["classes"])
    print(f"\ranalyzing microservices... {int(100*(i+1)/len(true_ms_dirs))}%", end="", flush=True)
print(f"\ranalyzing microservices... 100%", flush=True)

# debug
# print(*true_ms_classnames, sep="\n")
# debug

print("merging source files...", end=" ", flush=True)
file_path = os.path.join(base_dir, f".data/{project_dir_name}/OneFileSource.java")
merge_java_files(args.project_directory, file_path)
print("done!")

print("parsing the code...", end=" ", flush=True)
json_path = os.path.join(base_dir, f".data/{project_dir_name}/classes.json")
subprocess.run(['java', '-cp', libs, os.path.join(base_dir, 'Mo2oM/JavaParser/Parser.java'), f".data/{project_dir_name}/OneFileSource.java", json_path])
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

class_names = list(classes_info)
print("class_names:", class_names)
true_microservices = [{-1} for _ in classes_info]
for i, ms in enumerate(true_ms_classnames):
    for clss in ms:
        true_microservices[class_names.index(clss)].add(i)
        if -1 in true_microservices[class_names.index(clss)]:
            true_microservices[class_names.index(clss)].discard(-1)
print("ground truth microservices:", true_microservices)

print("calculating metrics...", end=" ", flush=True)
output = {}
if args.evaluation_measure:
    for measure in args.evaluation_measure:
        if measure in ["SM", "IFN", "ICP"]:
            print(f"{measure}: {measures[measure](true_microservices, classes_info)}")
            output[measure] = measures[measure](true_microservices, classes_info)
        else:
            print(f"{measure}: {measures[measure](true_microservices)}")
            output[measure] = measures[measure](true_microservices)
output["microservices"] = [list(_) for _ in true_microservices]
print("done!")

if args.output_file:
    with open(args.output_file, "w") as output_file:
        json.dump(output, output_file, indent=2)
