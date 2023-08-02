'''
def camel_case_split(identifier) -> camelCase separated words
def preprocess(text) -> processed text
'''


from nltk import word_tokenize
from nltk.corpus import stopwords
from nltk.stem.snowball import SnowballStemmer
from re import finditer


def camel_case_split(identifier):
    matches = finditer(
        '.+?(?:(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])|$)', identifier)
    return [m.group(0) for m in matches]


def preprocess(text):
    tokens = word_tokenize(text)
    tokens = [
        word for w in tokens for word in camel_case_split(w)
    ]
    stemmer = SnowballStemmer("english")
    stop_words = stopwords.words('english')
    tokens = [stemmer.stem(w) for w in tokens if w not in stop_words and any(c.isalpha() for c in w)]
    return ' '.join(tokens)
