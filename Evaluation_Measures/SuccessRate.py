def _corresponding(microservice_classes, true_microservices):
    max_common_classes = 0
    for i in set(true_microservices):
        true_microservice_classes = {clss for clss, ms in enumerate(true_microservices) if ms == i}
        if (m := len(true_microservice_classes.intersection(microservice_classes))) > max_common_classes:
            max_common_classes = m
            true_correspondence = true_microservice_classes
    return true_correspondence


def SR(microservices, true_microservices, k):
    threshold = k/10
    matches = 0
    for microservice in set(microservices):
        microservice_classes = {class_number for class_number, microservice_number in enumerate(microservices)
                                if microservice_number == microservice}
        matching = len(microservice_classes.intersection(_corresponding(microservice_classes, true_microservices))) / len(microservice_classes)
        if matching >= threshold:
            matches += 1
    return matches / len(set(microservices))

