from os import makedirs, walk, path, pathsep
from subprocess import run
from json import load
from EvaluationMeasures import *


proj = ""
microservices = []
classes_order = {}

project_directory = None


def _merge_java_files(src_dir, dest_file):
    with open(dest_file, 'w') as outfile:
        for root, dirs, files in walk(src_dir):
            for filename in files:
                if filename.endswith('.java'):
                    filepath = path.join(root, filename)
                    with open(filepath) as infile:
                        lines = infile.readlines()
                        for line in lines:
                            if not line.startswith('package'):
                                outfile.write(line)
                    outfile.write('\n')


base_dir = path.dirname(path.realpath(__file__))
source_code_path = f"Test_Projects/{proj}/OneFileSource.java"

if project_directory:
    if project_directory.endswith("/"):
        project_directory = project_directory[:-1]
    project_dir_name = project_directory.split('/')[-1]
    true_ms_dirs = next(walk(project_directory))[1]
    true_ms_classnames = []
    libs = path.join(base_dir,
                        "Mono2Multi/JavaParser/lib/javaparser-core-3.25.5-SNAPSHOT.jar") + pathsep + path.join(
        base_dir, "Mono2Multi/JavaParser/lib/json-20230618.jar")
    makedirs(path.join(base_dir, f"data/{project_dir_name}"), exist_ok=True)
    for directory in true_ms_dirs:
        if directory.endswith("/"):
            directory = directory[:-1]
        json_path = path.join(base_dir, f"data/{project_dir_name}/{directory}.json")
        run(['java', '-cp', libs, path.join(base_dir, 'Mono2Multi/JavaParser/ClassScanner.java'),
                path.join(project_directory, directory), json_path])
        with open(json_path, "rt") as classes_file:
            true_ms_classnames.append(load(classes_file)["classes"])

    source_code_path = path.join(base_dir, f"data/{project_dir_name}/OneFileSource.java")
    _merge_java_files(project_directory, source_code_path)

libs = path.join(base_dir, "Mono2Multi", "JavaParser","lib","javaparser-core-3.25.5-SNAPSHOT.jar") + pathsep + path.join(
    base_dir,
    "Mono2Multi","JavaParser","lib","json-20230618.jar")
makedirs(path.join(base_dir, "data"), exist_ok=True)
json_path = path.join(base_dir, "data", "expt_classes.json")
run(['java', '-cp', libs, path.join(base_dir, "Mono2Multi","JavaParser","Parser.java"), source_code_path, json_path])
with open(json_path, "rt") as classes_file:
    classes_info = load(classes_file)

# finding class names for each method call
for clss in classes_info:
    for call in classes_info[clss]["method_calls"]:
        for other_clss in classes_info:
            if call["method_name"] in classes_info[other_clss]["methods"]:
                call["class_name"] = other_clss
                if clss == other_clss:
                    break
if project_directory:
    class_names = list(classes_info.keys())
    true_microservices = [-1 for _ in classes_info]
    for i, ms in enumerate(true_ms_classnames):
        for clss in ms:
            true_microservices[class_names.index(clss)] = i
new_microservices = []
for clss in classes_info:
    for num in classes_order:
        if "::" in classes_order[num]:
            classes_order[num] = classes_order[num].split("::")[1]
        if clss == classes_order[num]:
            new_microservices.append(microservices[int(num)])
            break
    else:
        new_microservices.append(-1)
for num in classes_order:
    if classes_order[num] not in classes_info:
        print(f"different sources: {classes_order[num]}")

print(len(microservices), len(new_microservices))
microservices = new_microservices

d = {}
d["microservices"] = microservices
d["ICP"] = ICP(microservices, classes_info)
d["NED"] = NED(microservices)
d["SM"] = SM(microservices, classes_info)
d["IFN"] = IFN(microservices, classes_info)
print(d)