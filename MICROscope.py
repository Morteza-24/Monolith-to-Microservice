from MICROscope.main import MICROscope
from MICROscope.EvaluationMeasures import *
from argparse import ArgumentParser
from json import load
from subprocess import run
from os import makedirs, walk, path, pathsep

parser = ArgumentParser(
    prog='python MICROscope.py',
    description='This program offers tools related to migrating from monolithic architectures to microservices.',
    epilog='example usage: python MICROscope.py -p ./Test_Projects/PetClinic/src -e NED ICP SR -k 5 7')

parser.add_argument("-f", "--file", dest="file_path",
                    help="path to the java source code file (use this option if your whole monolithic program is in one file)")
parser.add_argument("-p", "--project", dest="project_directory",
                    help="path to the java project directory, (use this option if your monolithic program is in multiple files) this option overrides --file")
parser.add_argument("-e", "--evaluation-measure", choices=["Precision", "SR", "SM", "IFN", "NED", "ICP"], nargs="*",
                    help="For the Precision and the SuccessRate (SR) measures, the ground truth microservices must be in different directories of your project's root directory.\
                        And for the SR measure you should also use the -k option to specify a threshold.")
parser.add_argument("-k", type=int, nargs="*",
                    help="The k value for the SR measure (e.g. SR@7). This option can only be used if you are using the SR measure.")

args = parser.parse_args()

if not (args.file_path or args.project_directory):
    parser.print_help()
    print("\nerror: one of the following arguments are required: --file, --project")
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
    libs = path.join(base_dir, "MICROscope/JavaParser/lib/javaparser-core-3.25.5-SNAPSHOT.jar")+pathsep+path.join(base_dir, "MICROscope/JavaParser/lib/json-20230618.jar")
    makedirs(path.join(base_dir, f"data/{project_dir_name}"), exist_ok=True)
    for directory in true_ms_dirs:
        if directory.endswith("/"):
            directory = directory[:-1]
        json_path = path.join(base_dir, f"data/{project_dir_name}/{directory}.json")
        run(['java', '-cp', libs, path.join(base_dir, 'MICROscope/JavaParser/ClassScanner.java'), path.join(args.project_directory, directory), json_path])
        with open(json_path, "rt") as classes_file:
            true_ms_classnames.append(load(classes_file)["classes"])

    args.file_path = path.join(base_dir, f"data/{project_dir_name}/OneFileSource.java")
    merge_java_files(args.project_directory, args.file_path)
    print("done!")

if args.file_path:
    print("\n--- MICROscope ---\n")
    alpha = float(input("alpha: "))
    clusters, classes_info = MICROscope(args.file_path, alpha)

    class_names = list(classes_info.keys())
    if args.project_directory:
        true_microservices = [-1 for _ in classes_info]
        for i, ms in enumerate(true_ms_classnames):
            for clss in ms:
                true_microservices[class_names.index(clss)] = i
        print("\nTrue Microservices:", true_microservices)

    print("\nClasses:")
    for class_number, class_name in enumerate(classes_info):
        print(f"#{class_number}: \t{class_name}")

    print("\nClusters:")
    print(clusters)
    print("\nMicroservices")
    for ms in range(max(max(_) for _ in clusters)+1):
        print(f"ms #{ms}", [class_names[clss_i] for clss_i, ms_list in enumerate(clusters) if ms in ms_list])

    if args.evaluation_measure:
        for measure in args.evaluation_measure:
            if measure in ["SM", "IFN", "ICP"]:
                print(
                    f"{measure}: {measures[measure](clusters, classes_info)}")
            elif measure == "Precision":
                print(f"{measure}: {measures[measure](clusters, true_microservices)}")
            elif measure == "SR":
                for k in args.k:
                    print(f"{measure}@{k}: {measures[measure](clusters, true_microservices, k)}")
            else:
                print(f"{measure}: {measures[measure](clusters)}")
    print()