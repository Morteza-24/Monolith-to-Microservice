from math import log


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
    precision_sum = 0
    n_microservices = max(max(m) for m in microservices) + 1
    for microservice in range(n_microservices):
        ms_classes = {clss for clss, ms in enumerate(microservices)
                      if microservice in ms}
        precision_sum += len(ms_classes.intersection(_corresponding(ms_classes, true_microservices))) / len(ms_classes)
    return precision_sum / n_microservices


def SR(microservices, true_microservices, k):
    threshold = k/10
    matches = 0
    n_microservices = max(max(m) for m in microservices) + 1
    for microservice in range(n_microservices):
        ms_classes = {clss for clss, ms in enumerate(microservices)
                      if microservice in ms}
        matching = len(ms_classes.intersection(_corresponding(ms_classes, true_microservices))) / len(ms_classes)
        if matching >= threshold:
            matches += 1
    return matches / len(set(microservices))


def SM(microservices, classes_info):
    K = max(max(m) for m in microservices) + 1
    m = [0] * K  # number of classes for each microservice
    mu = [0] * K  # number of inside calls
    sigma = [[0]*K for _ in range(K)]  # number of outside calls

    for class_index, class_name in enumerate(classes_info):
        for microservice_i in microservices[class_index]:
            m[microservice_i] += 1
            for call in classes_info[class_name]["method_calls"]:
                class_j = call["class_name"]
                if class_j in classes_info:
                    j = list(classes_info).index(class_j)
                    for microservice_j in microservices[j]:
                        if microservice_i == microservice_j:
                            mu[microservice_i] += 1
                        else:
                            sigma[microservice_i][microservice_j] += 1

    SM1, SM2 = 0, 0
    for i in range(K):
        try:
            SM1 += mu[i] / (m[i] ** 2)
        except ZeroDivisionError:
            continue
        for j in range(K):
            if i != j:
                try:
                    SM2 += sigma[i][j] / (2 * m[i] * m[j])
                except ZeroDivisionError:
                    continue
    try:
        structural_modularity = SM1 / K - SM2 / ((K*(K-1))/2)
        return structural_modularity
    except ZeroDivisionError:
        return 0
    

def IFN(microservices, classes_info):
    num_microservices = max(max(ms) for ms in microservices) + 1
    interfaces_per_microservice = [set() for _ in range(num_microservices)]

    for class_index, class_name in enumerate(classes_info):
        class_ms_list = microservices[class_index]
        for call in classes_info[class_name]["method_calls"]:
            if call['class_name'] in classes_info:
                call_class_index = list(classes_info).index(call["class_name"])
                call_ms_list = microservices[call_class_index]
                for ms_i in class_ms_list:
                    if ms_i not in call_ms_list:
                        interfaces_per_microservice[ms_i].add(call['class_name'])

    total_interfaces = sum([len(interfaces)
                           for interfaces in interfaces_per_microservice])
    interface_number = total_interfaces / num_microservices
    return interface_number


def NED(microservices):
    microservices = [ims for ms in microservices for ims in ms]
    extreme = 0
    for i in set(microservices):
        if 5 < microservices.count(i) < 20:
            extreme += 1
    return 1 - extreme/len(set(microservices))


def _calls(classes_i, classes_j, classes_info):
    sum_calls = 0
    for class_i in classes_i:
        sum_calls += len([1 for call in classes_info[class_i]["method_calls"]
                          if call["class_name"] in classes_j])
    return sum_calls


def ICP(microservices, classes_info):
    class_names = list(classes_info.keys())
    numerator, denominator = 0, 0

    n_microservices = max(max(m) for m in microservices) + 1
    for microservice_i in range(n_microservices):
        for microservice_j in range(n_microservices):
            classes_i = {class_names[class_number] for class_number, microservices_list in enumerate(microservices)
                            if microservice_i in microservices_list}
            classes_j = {class_names[class_number] for class_number, microservices_list in enumerate(microservices)
                        if microservice_j in microservices_list}

            if microservice_i != microservice_j:
                try:
                    numerator += log(_calls(classes_i, classes_j, classes_info))+1
                except ValueError:
                    pass  # log domain error, can be ignored

            try:
                denominator += log(_calls(classes_i, classes_j, classes_info))+1
            except ValueError:
                pass  # log domain error, can be ignored

    try:
        return numerator/denominator
    except ZeroDivisionError:
        return float("inf")