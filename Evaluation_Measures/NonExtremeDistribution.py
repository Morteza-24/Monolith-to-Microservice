

def count_unique_numbers_k(microservices):
    unique_numbers_k = set(microservices)
    k = len(unique_numbers_k)
    return k


def count_occurrences(microservices,k):
    mi_values = []
    for i in range(1, k + 1):
        count =microservices.count(i)
        mi_values.append(count)
    
    return mi_values

