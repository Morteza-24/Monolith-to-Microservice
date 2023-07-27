# Stemming words using nltk - could be useful for implementation
from nltk.stem import PorterStemmer

# Define a list of words - example
words = ["stemming", "stemmed", "stemmer", "stem", "stems"]

# Create an instance of PorterStemmer
stemmer = PorterStemmer()

# Stem each word in the list
stemmed_words = [stemmer.stem(word) for word in words]

# Print the stemmed words
print(stemmed_words)