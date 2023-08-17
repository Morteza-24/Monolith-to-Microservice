'''
def DBSCAN(minimum_number_of_sample, epsilon, class_similarity_matrix) -> clusters
def visualize_cluster(cluster)
'''


from sklearn.cluster import DBSCAN


def dbscan(minimum_number_of_sample, epsilon, class_similarity_matrix):
    clustering = DBSCAN(eps=epsilon, min_samples=minimum_number_of_sample, metric='precomputed').fit(
        class_similarity_matrix)
    return clustering.labels_
