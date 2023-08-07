# K is the number of the extracted microservices
# 𝐼 is the set of interface classes within microservice i

# An interface class in this case is a class that has
# been called by a class in an external microservice


def IFN(microservices, classes_info):
    num_microservices = max(microservices) + 1

    interfaces_per_microservice = [[] for i in range(num_microservices)]

    for class_index, class_name in enumerate(classes_info):
        ms_index = microservices[class_index]
        for call in classes_info[class_name]["method_calls"]:
            if call['class_name'] in list(classes_info):
                if microservices[list(classes_info).index(
                        call["class_name"])] != ms_index:
                    interfaces_per_microservice[ms_index].append(
                        call['class_name'])
            # for class calls that are not listed
            # else:
            #   interfaces_per_microservice[ms_index].append(call['class_name'])
    print(interfaces_per_microservice)
    total_interfaces = sum(
        [len(interfaces) for interfaces in interfaces_per_microservice])

    interface_number = total_interfaces / num_microservices
    return interface_number


# test
a = {
    "Class_1": {
        "methods": ["method_1", "method_2", "method_3", "method_4"],
        "method_calls": [{
            "method_name": "method_5",
            "class_name": "Class_2"
        }, {
            "method_name": "method_6",
            "class_name": "Class_2"
        }],
        "words": ["word_1", "word_2", "word_3", "word_4"]
    },
    "Class_2": {
        "methods": ["method_5", "method_6", "method_7"],
        "method_calls": [{
            "method_name": "method_1",
            "class_name": "Class_1"
        }, {
            "method_name": "pop",
            "class_name": "numbers"
        }],
        "words": ["word_5", "word_6", "word_7", "word_8", "word_9"]
    }
}
print((IFN([0, 1], a)))
