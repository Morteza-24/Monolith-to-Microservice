from os import makedirs, walk, path, pathsep
from subprocess import run
from json import load
from math import log


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


def init(classes_order, source_code_path, project_directory=None):
    base_dir = path.dirname(path.realpath(__file__))
    libs = path.join(base_dir, "Mono2Multi/JavaParser/lib/javaparser-core-3.25.5-SNAPSHOT.jar") + pathsep + path.join(
        base_dir,
        "Mono2Multi/JavaParser/lib/json-20230618.jar")
    json_path = path.join(base_dir, "expt_classe_sources.json")
    run(['java', '-cp', libs, 'ClassSources.java', source_code_path, json_path])
    with open(json_path, "rt") as classes_file:
        classes_sources = load(classes_file)
    new_source = ""
    for i in classes_order:
        if classes_order[i] in classes_sources:
            new_source += classes_sources[classes_order[i]]
        else:
            new_source += "public class "+classes_order[i].replace(":","")+"{}"
        new_source += "\n"
    with open("ExptOneFileSource.java", "wt") as new_file:
        new_file.write(new_source)
    source_code_path = "ExptOneFileSource.java"

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

    libs = path.join(base_dir, "Mono2Multi/JavaParser/lib/javaparser-core-3.25.5-SNAPSHOT.jar") + pathsep + path.join(
        base_dir,
        "Mono2Multi/JavaParser/lib/json-20230618.jar")
    json_path = path.join(base_dir, "expt_classes.json")
    run(['java', '-cp', libs, path.join(base_dir, 'Mono2Multi/JavaParser/Parser.java'), source_code_path, json_path])
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
        return classes_info, true_microservices

    return classes_info


def _corresponding(microservice_classes, true_microservices):
    max_common_classes = 0
    for true_ms in set(true_microservices):
        true_ms_classes = {clss for clss, ms in enumerate(true_microservices)
                           if ms == true_ms}  # TODO: this doesn't have to repeat
        if (m := len(true_ms_classes.intersection(microservice_classes))) > max_common_classes:
            max_common_classes = m
            corresponding_ms = true_ms_classes
    return corresponding_ms


def Precision(microservices, true_microservices):
    n_microservices = max(microservice)+1
    precision_sum = 0
    for microservice in range(n_microservices):
        ms_classes = {clss for clss, ms in enumerate(microservices)
                      if ms == microservice}
        precision_sum += len(ms_classes.intersection(_corresponding(ms_classes, true_microservices))) / len(ms_classes)
    return precision_sum / n_microservices


def SR(microservices, true_microservices, k):
    threshold = k/10
    matches = 0
    for microservice in range(max(microservices)+1):
        ms_classes = {clss for clss, ms in enumerate(microservices)
                      if ms == microservice}
        matching = len(ms_classes.intersection(_corresponding(ms_classes, true_microservices))) / len(ms_classes)
        if matching >= threshold:
            matches += 1
    return matches / (max(microservices)+1)


def SM(microservices, classes_info):
    K = max(microservices)+1
    m = [0] * K  # number of classes for each microservice
    mu = [0] * K  # number of inside calls
    sigma = [[0]*K for _ in range(K)]  # number of outside calls

    for class_index, class_name in enumerate(classes_info):
        microservice_i = microservices[class_index]
        if microservice_i == -1:
            continue
        m[microservice_i] += 1
        for call in classes_info[class_name]["method_calls"]:
            class_j = call["class_name"]
            if class_j in classes_info:
                j = list(classes_info).index(class_j)
                microservice_j = microservices[j]
                if microservice_j == -1:
                    continue
                if microservice_i == microservice_j:
                    mu[microservice_i] += 1
                else:
                    sigma[microservice_i][microservice_j] += 1

    SM1, SM2 = 0, 0
    for i in range(K):
        try:
            SM1 += mu[i] / (m[i] ** 2)
        except ZeroDivisionError:
            pass
        for j in range(K):
            if i != j:
                try:
                    SM2 += (sigma[i][j]+sigma[j][i]) / (2 * m[i] * m[j])
                except ZeroDivisionError:
                    pass
    if K == 0:
        SM1 = 0
    else:
        SM1 /= K
    if ((K*(K-1))/2) == 0:
        SM2 = 0
    else:
        SM2 /= ((K*(K-1))/2)
    return SM1 - SM2


def IFN(microservices, classes_info):
    num_microservices = max(microservices) + 1
    interfaces_per_microservice = [set() for i in range(num_microservices)]

    for class_index, class_name in enumerate(classes_info):
        microservice_i = microservices[class_index]
        if microservice_i == -1:
            continue
        for call in classes_info[class_name]["method_calls"]:
            if call['class_name'] in classes_info:
                microservice_j = microservices[list(classes_info).index(call["class_name"])]
                if microservice_j != microservice_i:
                    interfaces_per_microservice[microservice_j].add(
                        call['class_name'])

    total_interfaces = sum([len(interfaces)
                           for interfaces in interfaces_per_microservice])
    interface_number = total_interfaces / num_microservices
    return interface_number


def NED(microservices):
    microservices = list(microservices)
    k = max(microservices)+1
    non_extreme = 0
    for i in range(k):
        if 5 < microservices.count(i) < 20:
            non_extreme += 1
    return 1 - non_extreme/k


def _icp(classes_i, classes_j, classes_info):
    sum_calls = 0
    for class_i in classes_i:
        for class_j in classes_j:
            calls = len([1 for call in classes_info[class_i]["method_calls"]
                         if call["class_name"] == class_j])
            try:
                sum_calls += log(calls)+1
            except ValueError:
                pass  # log domain error is okay to pass
    return sum_calls


def ICP(microservices, classes_info):
    class_names = list(classes_info.keys())
    numerator, denominator = 0, 0

    for microservice_i in range(max(microservices)+1):
        classes_i = {class_names[class_number] for class_number, microservice_number in enumerate(microservices)
                     if microservice_number == microservice_i}
        for microservice_j in range(max(microservices)+1):
            classes_j = {class_names[class_number] for class_number, microservice_number in enumerate(microservices)
                         if microservice_number == microservice_j}

            if microservice_i != microservice_j:
                numerator += _icp(classes_i, classes_j, classes_info)
            denominator += _icp(classes_i, classes_j, classes_info)

    if numerator == 0:
        return 0
    return numerator/denominator
