'''
def preprocess(text) -> tokens list
'''

# Stemming words using nltk - could be useful for implementation
import nltk

# test
# words = ["stemming", "stemmed", "stemmer", "stem", "stems"]
# stemmer = nltk.stem.PorterStemmer()
# stemmed_words = [stemmer.stem(word) for word in words]
# print(stemmed_words)


# Perform preprocessing
def preprocess(text):
    stemmer = nltk.stem.PorterStemmer()
    stopwords = set(nltk.corpus.stopwords.words('english'))
    tokens = nltk.word_tokenize(text)
    tokens = [stemmer.stem(w) for w in tokens if w not in stopwords]
    return tokens
