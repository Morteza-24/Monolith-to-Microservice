def _corresponding(microservice_classes, true_microservices):
    max_common_classes = 0
    for true_ms in set(true_microservices):
        true_ms_classes = {clss for clss, ms in enumerate(true_microservices) if ms == true_ms}
        if (m := len(true_ms_classes.intersection(microservice_classes))) > max_common_classes:
            max_common_classes = m
            corresponding_ms = true_ms_classes
    return corresponding_ms


def precision(microservices, true_microservices):
    summation = 0
    for microservice in set(microservices):
        ms_classes = {clss for clss, ms in enumerate(microservices) if ms == microservice}
        summation += len(microservice.intersection(_corresponding(ms_classes, true_microservices))) / len(ms_classes)
    return summation / len(set(microservices))
