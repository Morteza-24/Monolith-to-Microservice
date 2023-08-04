def corresponding(microservice, true_microservices):
    max_common_classes = 0
    for i in set(true_microservices):
        true_microservice = {clss for clss, ms in enumerate(true_microservices) if ms == i}
        if (m := len(true_microservice.intersection(microservice))) > max_common_classes:
            max_common_classes = m
            true_correspondence = true_microservice
    return true_correspondence


def precision(microservices, true_microservices):
    summation = 0
    for i in set(microservices):
        microservice = {clss for clss, ms in enumerate(microservices) if ms == i}
        summation += len(microservice.intersection(corresponding(microservice, true_microservices))) / len(microservice)
    return summation / len(set(microservices))
