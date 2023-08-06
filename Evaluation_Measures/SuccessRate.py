import numpy as np
def SR (matching_matrix):
    M = matching_matrix.shape[0]  # Get the number of rows in the matching matrix

    Sigma = np.sum(matching_matrix)  # Calculate the sum of all elements in the matching matrix

    SR = 1 / M * Sigma  # Calculate the SR value

    return SR
    import numpy as np

    def create_matching_matrix(matrix):
        size = len(matrix)

    matching_matrix = np.zeros((size, size))

    for i in range(size):
        for j in range(size):
            if matrix[i][j] > 0:
            matching_matrix[i][j] = 1

    return matching_matrix

    def create_corresponding_matrix(matrix):
        size = len(matrix)

    corresponding_matrix = np.zeros((size, size))

    for i in range(size):
        for j in range(size):
            if matrix[i][j] > 0:
            corresponding_matrix[i][j] = matrix[i][j]

    return corresponding_matrix



