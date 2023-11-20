import pickle
with open("apps/"+input("file: ")+"/embeddings_membership.pkl", "rb") as f:
    print(str(pickle.load(f)).replace(" ",","))

