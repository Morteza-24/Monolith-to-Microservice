# Stemming words using wordnet - could be useful for implementation
import nltk
from nltk.corpus import wordnet

# Define a list of words - example
words = ["stemming", "stemmed", "stemmer", "stem", "stems"]

# Download the WordNet corpus (only need to do this once)
nltk.download('wordnet')

# Create an instance of WordNetLemmatizer
lemmatizer = nltk.WordNetLemmatizer()

# Find the base form (lemma) of each word in the list
lemmatized_words = [lemmatizer.lemmatize(word, wordnet.VERB) for word in words]

# Print the lemmatized words
print(lemmatized_words)