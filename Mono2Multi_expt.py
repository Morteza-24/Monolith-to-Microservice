from Mono2Multi.main import Mono2Multi
from Mono2Multi.EvaluationMeasures import *
from argparse import ArgumentParser
from os import makedirs, walk, path, pathsep
from subprocess import run
from json import load, dump
import numpy as np

parser = ArgumentParser(
    prog='python Mono2Multi_expt.py',
    description='Experiment versoin of the Mono2Multi tool.',
    epilog='example usage: python Mono2Multi_expt.py -f Test_Projects/JPetStore/OneFileSource.java --alpha 1 --n-clusters 3 --threshold 0.1 0.7 --n-execs 10 -e ICP NED -o output.json')

parser.add_argument("-f", "--file", dest="file_path",
                    help="path to the java source code file (use this option if your whole monolithic program is in one file)")
parser.add_argument("-p", "--project", dest="project_directory",
                    help="path to the java project directory, (use this option if your monolithic program is in multiple files) this option overrides --file")
parser.add_argument("-o", "--output-file", dest="output_file",
                    help="output file name to save results in")
parser.add_argument("-e", "--evaluation-measure", choices=["Precision", "SR", "SM", "IFN", "NED", "ICP"], nargs="*",
                    help="For the Precision and the SuccessRate (SR) measures, the ground truth microservices must be in different directories of your project's root directory.\
                        And for the SR measure you should also use the -k option to specify a threshold.")
parser.add_argument("-k", type=int, nargs="*",
                    help="The k value for the SR measure (e.g. SR@7). This option can only be used if you are using the SR measure.")
parser.add_argument("--alpha", dest="alpha", type=float, nargs="*", default=[None],
                    help="alpha hyperparameter, determines the semantic similarity affect percentage. Enter two values to run the method on an interval of alpha values.")
parser.add_argument("--n-clusters", dest="n_clusters", type=int, nargs="*", default=[None],
                    help="number of clusters hyperparameter, Enter two values to run the method on an interval of n_clusters values")
parser.add_argument("--threshold", dest="threshold", type=float, nargs="*", default=[None],
                    help="degree of membership threshold hyperparameter. Enter two values to run the method on an interval of threshold values.")
parser.add_argument("--n-execs", dest="n_execs", type=int,
                    help="the number of times FCM is run to get an average")

args = parser.parse_args()

if not (args.file_path or args.project_directory):
    parser.print_help()
    print("\nerror: one of the following arguments are required: --file, --project")
    exit()

if not args.output_file:
    parser.print_help()
    print("\nerror: -o, --output-file is required in the experment version")
    exit()

if int(len(args.alpha)==2) + int(len(args.n_clusters)==2) + int(len(args.threshold)==2) != 1:
    parser.print_help()
    print("\nerror: you should use an interval for one and only one hyperparameter at each run")
    exit()

if args.evaluation_measure:
    if bool(args.k) != ("SR" in args.evaluation_measure):
        parser.print_help()
        print("\nerror: The SR evaluation measure and the -k flag should always be used together to specify the k value for the SR measure. (e.g. -e SR -k 7")
        exit()

    if (("Precision" in args.evaluation_measure) or ("SR" in args.evaluation_measure)) and not args.project_directory:
        parser.print_help()
        print("\nerror: For the Precision and the SuccessRate (SR) measures, you should use the --project flag and the ground truth microservices must be in different directories of your project's root directory.")
        exit()

measures = {"Precision": Precision, "SR": SR,
            "SM": SM, "IFN": IFN, "NED": NED, "ICP": ICP}
base_dir = path.dirname(path.realpath(__file__))


def merge_java_files(src_dir, dest_file):
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


if args.project_directory:
    print("merging source files...", end="\t", flush=True)
    if args.project_directory.endswith("/"):
        args.project_directory = args.project_directory[:-1]
    project_dir_name = args.project_directory.split('/')[-1]
    true_ms_dirs = next(walk(args.project_directory))[1]
    true_ms_classnames = []
    libs = path.join(base_dir, "Mono2Multi/JavaParser/lib/javaparser-core-3.25.5-SNAPSHOT.jar")+pathsep+path.join(base_dir, "Mono2Multi/JavaParser/lib/json-20230618.jar")
    makedirs(path.join(base_dir, f"data/{project_dir_name}"), exist_ok=True)
    for directory in true_ms_dirs:
        if directory.endswith("/"):
            directory = directory[:-1]
        json_path = path.join(base_dir, f"data/{project_dir_name}/{directory}.json")
        run(['java', '-cp', libs, path.join(base_dir, 'Mono2Multi/JavaParser/ClassScanner.java'), path.join(args.project_directory, directory), json_path])
        with open(json_path, "rt") as classes_file:
            true_ms_classnames.append(load(classes_file)["classes"])

    args.file_path = path.join(base_dir, f"data/{project_dir_name}/OneFileSource.java")
    merge_java_files(args.project_directory, args.file_path)
    print("done!")

if args.file_path:
    print("\n--- Mono2Multi ---\n")
    if args.alpha == [None]:
        args.alpha = [float(input("alpha: "))]
    if args.n_execs == None:
        args.n_execs = 10

    outputs = []
    if len(args.alpha) == 2:
        alphas = [round(_, 3) for _ in np.arange(args.alpha[0], args.alpha[1], 0.1)]
        for alpha in alphas:
            clusters, classes_info = Mono2Multi(args.file_path, args.alpha, args.n_clusters[0], args.threshold[0], args.n_execs)
            class_names = list(classes_info.keys())
            if args.project_directory:
                true_microservices = [-1 for _ in classes_info]
                for i, ms in enumerate(true_ms_classnames):
                    for clss in ms:
                        true_microservices[class_names.index(clss)] = i

            output = {"alpha": float(alpha), "microservices": [list(i) for i in clusters]}

            if args.evaluation_measure:
                for measure in args.evaluation_measure:
                    if measure in ["SM", "IFN", "ICP"]:
                        output[measure] = measures[measure](clusters, classes_info)
                    elif measure == "Precision":
                        output[measure] = measures[measure](clusters, true_microservices)
                    elif measure == "SR":
                        for k in args.k:
                            output[measure+"@"+str(k)] = measures[measure](clusters, true_microservices, k)
                    else:
                        output[measure] = measures[measure](clusters)
            outputs.append(output)

        with open(args.output_file, "w") as output_file:
            dump(outputs, output_file, indent=2)
    elif len(args.n_clusters) == 2:
        n_clusterss = np.arange(args.n_clusters[0], args.n_clusters[1], 1)
        clusters, classes_info = Mono2Multi(args.file_path, args.alpha[0], n_clusterss, args.threshold[0], args.n_execs)
        class_names = list(classes_info.keys())
        if args.project_directory:
            true_microservices = [-1 for _ in classes_info]
            for i, ms in enumerate(true_ms_classnames):
                for clss in ms:
                    true_microservices[class_names.index(clss)] = i
        for i in range(len(n_clusterss)):
            output = {"n_clusters": int(n_clusterss[i]), "microservices": [list(_) for _ in clusters[i]]}
            if args.evaluation_measure:
                for measure in args.evaluation_measure:
                    if measure in ["SM", "IFN", "ICP"]:
                        output[measure] = measures[measure](clusters[i], classes_info)
                    elif measure == "Precision":
                        output[measure] = measures[measure](clusters[i], true_microservices)
                    elif measure == "SR":
                        for k in args.k:
                            output[measure+"@"+str(k)] = measures[measure](clusters[i], true_microservices, k)
                    else:
                        output[measure] = measures[measure](clusters[i])
            outputs.append(output)

    elif len(args.threshold) == 2:
        thresholds = [round(_, 3) for _ in np.arange(args.threshold[0], args.threshold[1], 0.05)]
        clusters, classes_info = Mono2Multi(args.file_path, args.alpha[0], args.n_clusters[0], thresholds, args.n_execs)
        class_names = list(classes_info.keys())
        if args.project_directory:
            true_microservices = [-1 for _ in classes_info]
            for i, ms in enumerate(true_ms_classnames):
                for clss in ms:
                    true_microservices[class_names.index(clss)] = i
        for i in range(len(thresholds)):
            output = {"threshold": float(thresholds[i]), "microservices": [list(_) for _ in clusters[i]]}
            if args.evaluation_measure:
                for measure in args.evaluation_measure:
                    if measure in ["SM", "IFN", "ICP"]:
                        output[measure] = measures[measure](clusters[i], classes_info)
                    elif measure == "Precision":
                        output[measure] = measures[measure](clusters[i], true_microservices)
                    elif measure == "SR":
                        for k in args.k:
                            output[measure+"@"+str(k)] = measures[measure](clusters[i], true_microservices, k)
                    else:
                        output[measure] = measures[measure](clusters[i])
            outputs.append(output)

    with open(args.output_file, "w") as output_file:
        dump(outputs, output_file, indent=2)