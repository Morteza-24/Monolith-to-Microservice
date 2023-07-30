'''
def camel_case_split(identifier) -> camelCase separated words
def preprocess(text) -> tokens list
'''

import nltk
from nltk.corpus import stopwords
from nltk.stem import PorterStemmer
from nltk.util import ngrams

# Downloading nltk data
# nltk.download('punkt')
# nltk.download('stopwords')

# test---
# words = ["stemming", "stemmed", "stemmer", "stem", "stems"]
# stemmer = nltk.stem.PorterStemmer()
# stemmed_words = [stemmer.stem(word) for word in words]
# print(stemmed_words)

stemmer = nltk.stem.PorterStemmer()
stopwords = set(nltk.corpus.stopwords.words('english'))


def dummy(doc):
    return doc


def camel_case_split(identifier):
    matches = re.findall(
        '.+?(?:(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])|$)', identifier)
    return [m.lower() for m in matches]


def preprocess(text):
    tokens = nltk.word_tokenize(text)
    tokens = [
        camel_case_split(w) if '_' not in w else w.lower() for w in tokens
    ]
    tokens = [stemmer.stem(w) for w in tokens if w not in stopwords]
    return tokens
