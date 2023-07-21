'''
def DBSCAN(minimum_number_of_sample, epsilon, class_similarity_matrix) -> cluster
def visualize_cluster(cluster)
'''

from sklearn.cluster import DBSCAN
import numpy as np

minimum_number_of_sample = 2
epsilon = 0.2 # added for testing 
class_similarity_matrix = np.array([[0]]) # TBD

def dbscan(minimum_number_of_sample, epsilon, class_similarity_matrix):
    clustering = DBSCAN(eps=epsilon, min_samples=minimum_number_of_sample).fit(class_similarity_matrix)
    return clustering.labels_

# Call the dbscan function
labels = dbscan(minimum_number_of_sample, epsilon, class_similarity_matrix)

# Print the cluster labels
print(labels)