from math import log
from random import choice


def _ms_to_clss(microservices, n_microservices):
    ms_clss = [set() for _ in range(n_microservices)]
    for clss_i, clss_ms in enumerate(microservices):
        for ms in clss_ms:
            try:
                ms_clss[ms].add(clss_i)
            except IndexError:
                return []
    return ms_clss


def _corresponding(microservice_classes, true_microservices_classes):
    if not microservice_classes:
        return set()
    max_common_classes = 0
    for true_microservice_classes in true_microservices_classes:
        if (m := len(true_microservice_classes.intersection(microservice_classes))) > max_common_classes:
            max_common_classes = m
            corresponding_ms = true_microservice_classes
    return corresponding_ms


def Precision(microservices, true_microservices):
    n_microservices = max(max(m) for m in microservices) + 1
    microservices_classes = _ms_to_clss(microservices, n_microservices)
    n_true_microservices = max(max(m) for m in true_microservices) + 1
    true_microservices_classes = _ms_to_clss(true_microservices, n_true_microservices)
    precision_sum = 0
    for microservice_classes in microservices_classes:
        try:
            precision_sum += len(microservice_classes.intersection(_corresponding(microservice_classes, true_microservices_classes))) / len(microservice_classes)
        except ZeroDivisionError:
            pass
    if n_microservices == 0:
        return 0
    return precision_sum / n_microservices


def SR(microservices, true_microservices, k):
    n_microservices = max(max(m) for m in microservices) + 1
    microservices_classes = _ms_to_clss(microservices, n_microservices)
    n_true_microservices = max(max(m) for m in true_microservices) + 1
    true_microservices_classes = _ms_to_clss(true_microservices, n_true_microservices)
    threshold = k/10
    matches = 0
    for microservice_classes in microservices_classes:
        try:
            matching = len(microservice_classes.intersection(_corresponding(microservice_classes, true_microservices_classes))) / len(microservice_classes)
        except ZeroDivisionError:
            matching = 0
        if matching >= threshold:
            matches += 1
    if n_microservices == 0:
        return 0
    return matches / n_microservices


def SM(microservices, classes_info):
    K = max(max(m) for m in microservices) + 1  # number of microservices
    m = [0] * K  # number of classes for each microservice
    mu = [0] * K  # number of inside calls
    sigma = [[0]*K for _ in range(K)]  # number of outside calls
    for class_index, class_name in enumerate(classes_info):
        class_microservices = microservices[class_index]
        if -1 in class_microservices:
            continue
        for microservice_i in class_microservices:
            m[microservice_i] += 1
        for call in classes_info[class_name]["method_calls"]:
            class_j = call["class_name"]
            if class_j in classes_info:
                j = list(classes_info).index(class_j)
                if class_index == j:
                    continue
                if -1 in microservices[j]:
                    continue
                for ms in set(class_microservices) - set(microservices[j]):
                    sigma[ms][choice(list(microservices[j]))] += 1
                for ms in set(class_microservices).intersection(set(microservices[j])):
                    mu[ms] += 1

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
    num_microservices = max(max(ms) for ms in microservices) + 1
    interfaces_per_microservice = [set() for _ in range(num_microservices)]

    for class_index, class_name in enumerate(classes_info):
        class_microservices = microservices[class_index]
        for call in classes_info[class_name]["method_calls"]:
            if call['class_name'] in classes_info:
                call_class_index = list(classes_info).index(call["class_name"])
                call_microservices = microservices[call_class_index]
                for call_microservice in call_microservices:
                    if call_microservice in class_microservices:
                        break
                else:
                    interfaces_per_microservice[call_microservice].add(call['class_name'])

    total_interfaces = sum([len(interfaces)
                           for interfaces in interfaces_per_microservice])
    try:
        interface_number = total_interfaces / num_microservices
    except ZeroDivisionError:
        interface_number = 0
    return interface_number


def NED(microservices):
    microservices = [ims for ms in microservices for ims in ms if ims != -1]
    non_extreme = 0
    for i in set(microservices):
        class_count = microservices.count(i)
        if 5 <= class_count <= 20:
            non_extreme += class_count
    try:
        return 1 - non_extreme/len(microservices)
    except:
        return 1


def _icp_num(classes_i, classes_j, classes_info):
    sum_calls = 0
    for class_i in classes_i:
        if class_i in classes_j:
            continue
        for class_j in classes_j:
            if class_j in classes_i:
                continue
            calls = len([1 for call in classes_info[class_i]["method_calls"]
                        if call["class_name"] == class_j])
            try:
                sum_calls += log(calls)+1
            except ValueError:
                pass  # log domain error is okay to pass
    return sum_calls


def _icp_denom(classes_i, classes_j, classes_info):
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
    class_names = list(classes_info)
    numerator, denominator = 0, 0

    n_microservices = max(max(m) for m in microservices) + 1
    for microservice_i in range(n_microservices):
        classes_i = {class_names[class_number] for class_number, microservices_list in enumerate(microservices)
                     if microservice_i in microservices_list}
        for microservice_j in range(n_microservices):
            classes_j = {class_names[class_number] for class_number, microservices_list in enumerate(microservices)
                        if microservice_j in microservices_list}

            if microservice_i != microservice_j:
                numerator += _icp_num(classes_i, classes_j, classes_info)
            denominator += _icp_denom(classes_i, classes_j, classes_info)

    if numerator == 0:
        return 0
    return numerator/denominator
