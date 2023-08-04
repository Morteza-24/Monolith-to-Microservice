from math import log

def _calls(classes_i, classes_j, classes_info):
    sum_calls = 0
    for class_i in classes_i:
        sum_calls += len([1 for call in classes_info[class_i]["method_calls"]
                          if call["class_name"] in classes_j])


def ICP(microservices, classes_info):
    class_names = list(classes_info.keys())
    numerator, denominator = 0, 0

    for microservice_i in set(microservices):
        classes_i = {class_names[class_number] for class_number, microservice_number in enumerate(microservices)
                     if microservice_number == microservice_i}
        for microservice_j in set(microservices):
            classes_j = {class_names[class_number] for class_number, microservice_number in enumerate(microservices)
                            if microservice_number == microservice_j}

            if microservice_i != microservice_j:
                numerator += log(_calls(classes_i, classes_j, classes_info))+1

            denominator += log(_calls(classes_i, classes_i, classes_info))+1

    return numerator/denominator
