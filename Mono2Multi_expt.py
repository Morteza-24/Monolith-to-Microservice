from Mono2Multi.main import Mono2Multi
from EvaluationMeasures import *
from argparse import ArgumentParser
from os import makedirs, walk, path, pathsep
from subprocess import run
from json import load, dump
import numpy as np

parser = ArgumentParser(
    prog='python Mono2Multi_expt.py',
    description='Experiment versoin of the Mono2Multi tool.',
    epilog='example usage: python Mono2Multi_expt.py -f test_projects/JPetStore/OneFileSource.java --alpha 1 --n-clusters 3 --threshold 0.1 0.7 -e ICP NED -o output.json')

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
                    help="number of clusters hyperparameter, Enter two values to run the method on an interval of n_clusters values. Leave empty to use Scanniello's approach.")
parser.add_argument("--threshold", dest="threshold", type=float, nargs="*", default=[None],
                    help="degree of membership threshold hyperparameter. Enter two values to run the method on an interval of threshold values.")
parser.add_argument("--multiprocessing", dest="use_multiprocessing", action="store_true",
                    help="use multiprocessing to reduce runtime when running the model with different values of alpha.")
parser.add_argument("--n-execs", dest="n_execs", type=int,
                    help="(deprecated) the number of times FCM is run to get an average")

args = parser.parse_args()

if not (args.file_path or args.project_directory):
    parser.print_help()
    print("\nerror: one of the following arguments are required: --file, --project")
    exit()

if not args.output_file:
    parser.print_help()
    print("\nerror: -o, --output-file is required in the experment version")
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


def _listify(x):
    return x if isinstance(x, list) else [x]


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


def run_with_alpha(all_args):
    alpha, file_path, n_clusters, thresholds, n_execs, project_directory = [*all_args]
    print(f"alpha = {alpha}", flush=True)
    outputs = []
    clusters, classes_info = Mono2Multi(file_path, alpha, n_clusters, thresholds, n_execs)
    class_names = list(classes_info.keys())
    if project_directory:
        true_microservices = [{-1} for _ in classes_info]
        for i, ms in enumerate(true_ms_classnames):
            for clss in ms:
                true_microservices[class_names.index(clss)].add(i)
                if -1 in true_microservices[class_names.index(clss)]:
                    true_microservices[class_names.index(clss)].discard(-1)
    if n_clusters == "Scanniello":
        n_clusters = list(np.arange(2, (len(classes_info)//2)+2, 2))

    for i in range(len(_listify(n_clusters))):
        for j in range(len(_listify(thresholds))):
            if isinstance(n_clusters, int) and isinstance(thresholds, int):
                this_clusters = clusters
            elif isinstance(n_clusters, int):
                this_clusters = clusters[j]
            elif isinstance(thresholds, int):
                this_clusters = clusters[i]
            else:
                this_clusters = clusters[i][j]
            output = {"alpha": float(alpha),
                        "n_clusters": int(_listify(n_clusters)[i]),
                        "threshold": float(_listify(thresholds)[j]),
                        "microservices": [list(_) for _ in this_clusters]}
            if args.evaluation_measure:
                for measure in args.evaluation_measure:
                    if measure in ["SM", "IFN", "ICP"]:
                        output[measure] = measures[measure](this_clusters, classes_info)
                    elif measure == "Precision":
                        output[measure] = measures[measure](this_clusters, true_microservices)
                    elif measure == "SR":
                        for k in args.k:
                            output[measure+"@"+str(k)] = measures[measure](this_clusters, true_microservices, k)
                    else:
                        output[measure] = measures[measure](this_clusters)
            outputs.append(output)
    return outputs


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
        args.n_execs = 1
    if args.n_clusters == [None]:
        args.n_clusters = ["Scanniello"]
    if len(args.alpha) == 2:
        alphas = [round(_, 3) for _ in np.arange(args.alpha[0], args.alpha[1]+0.01, 0.05)]
    else:
        alphas = args.alpha
    if len(args.n_clusters) == 2:
        n_clusters = list(np.arange(args.n_clusters[0], args.n_clusters[1]+1, 1))
    else:
        n_clusters = args.n_clusters[0]
    if len(args.threshold) == 2:
        thresholds = [round(_, 3) for _ in np.arange(args.threshold[0], args.threshold[1]+0.01, 0.05)]
    else:
        thresholds = args.threshold[0]

    if args.use_multiprocessing:
        print("Using multiprocessing. Don't get confused by unordered logs.")
        from concurrent.futures import ProcessPoolExecutor as Pool
        inputs = [(alpha, args.file_path, n_clusters, thresholds, args.n_execs, args.project_directory) for alpha in alphas]
        with Pool() as pool:
            output_lists = pool.map(run_with_alpha, inputs)
    else:
        output_lists = []
        for alpha in alphas:
            output_lists.append(run_with_alpha(alpha, args.file_path, n_clusters, thresholds, args.n_execs, args.project_directory))
    outputs = []
    for l in output_lists:
        for output in l:
            outputs.append(output)
    with open("tmp_"+args.output_file, "w") as output_file:
        dump(outputs, output_file, indent=2)

    in_ms = False
    inn_ms = False
    with (open("tmp_"+args.output_file, "rt") as in_f,
        open(args.output_file, "wt") as out_f):
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

    run(["rm", "tmp_"+args.output_file])
