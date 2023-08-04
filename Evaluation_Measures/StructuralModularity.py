def SM(microservices, classes_info):

    K = len(set(microservices)) 
    SM = 0 
    mu = [0] * K 
    m = [0] * K 
    sigma = [[0] * K for _ in range(K)]


    for i, class_name in enumerate(classes_info):
        microservice_i = microservices[i] 
        m[microservice_i] += 1 
        for call in classes_info[class_name]["method_calls"]:
            class_j = call["class_name"] 
            if class_j in classes_info:
                j = list(classes_info).index(class_j) 
                microservice_j = microservices[j] 
                if microservice_i == microservice_j:
                    mu[microservice_i] += 1
                else:
                    sigma[microservice_i][microservice_j] += 1 
    
    
    for i in range(K):
        SM += mu[i] / (m[i] ** 2) 
    SM /= K
    for i in range(K):
        for j in range(K):
            if i != j:
                SM -= sigma[i][j] / (2 * m[i] * m[j])
    SM /= (K * (K - 1)) / 2 

    return SM 