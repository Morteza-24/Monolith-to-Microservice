'''
def structural_similarity(ci, cj, classes_info) -> sim_str(ci, cj)
def semantic_similarity(ci, cj, classes_info) -> sim_sem(ci, cj)
def class_similarity(alpha, classes_info) -> class_similarity_matrix
'''


def calls(ci, cj, classes_info):
    return len([_ for _ in classes_info[ci]["method_calls"] if _ in classes_info[cj]["methods"]])


def calls_in(ci, classes_info):
    return sum([calls(cj, ci, classes_info) for cj in classes_info if cj != ci])


def structural_similarity(ci, cj, classes_info):
    if calls_in(ci, classes_info) != 0 and calls_in(cj, classes_info) != 0:
        return (1/2) * (calls(ci, cj, classes_info)/calls_in(cj, classes_info) + calls(cj, ci, classes_info)/calls_in(ci, classes_info))
    elif calls_in(ci, classes_info) == 0 and calls_in(cj, classes_info) != 0:
        return calls(ci, cj, classes_info) / calls_in(cj, classes_info)
    elif calls_in(ci, classes_info) != 0 and calls_in(cj, classes_info) == 0:
        return calls(cj, ci, classes_info) / calls_in(ci, classes_info)
    else:
        return 0
