def SM(microservices, classes_info):

    K = len(set(microservices)) 
    SM = 0 
    mu = [0] * K 
    m = [0] * K 
    sigma = [[0] * K for _ in range(K)]


    # loop through the classes and count the calls (TBD)


    # calculate the SM according to the formula
    
    for i in range(K):
        SM += mu[i] / (m[i] ** 2) 
    SM /= K
    for i in range(K):
        for j in range(K):
            if i != j:
                SM -= sigma[i][j] / (2 * m[i] * m[j])
    SM /= (K * (K - 1)) / 2 

    return SM 