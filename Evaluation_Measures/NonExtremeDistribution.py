

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

def calculate_NED(k, mi_list):

    count = 0

    sum_mi = 0

    for i in range(1, k+1):

        mi = mi_list[i-1]

        if 5 < abs(mi) < 20:

            count += 1

            sum_mi += mi

    

    ned = 1 - abs(sum_mi) / k

    

    return ned