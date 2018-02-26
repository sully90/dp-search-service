from gensim.utils import lemmatize
from nltk.corpus import stopwords

def get_stopwords():
    return set(stopwords.words('english'))  # nltk stopwords list

def get_bigram(train_texts):
    import gensim
    bigram = gensim.models.Phrases(train_texts)  # for bigram collocation detection
    return bigram

def build_texts_from_file(fname):
    import gensim
    """
    Function to build tokenized texts from file
    
    Parameters:
    ----------
    fname: File to be read
    
    Returns:
    -------
    yields preprocessed line
    """
    with open(fname) as f:
        for line in f:
            yield gensim.utils.simple_preprocess(line, deacc=True, min_len=3)

def build_texts_as_list_from_file(fname):
    return list(build_texts_from_file(fname))

def build_texts(texts):
    import gensim
    """
    Function to build tokenized texts from file
    
    Parameters:
    ----------
    fname: File to be read
    
    Returns:
    -------
    yields preprocessed line
    """
    for line in texts:
        yield gensim.utils.simple_preprocess(line, deacc=True, min_len=3)

def build_texts_as_list(texts):
    return list(build_texts(texts))

def process_texts(texts, stops=get_stopwords()):
    """
    Function to process texts. Following are the steps we take:
    
    1. Stopword Removal.
    2. Collocation detection.
    3. Lemmatization (not stem since stemming can reduce the interpretability).
    
    Parameters:
    ----------
    texts: Tokenized texts.
    
    Returns:
    -------
    texts: Pre-processed tokenized texts.
    """
    bigram = get_bigram(texts)

    texts = [[word for word in line if word not in stops] for line in texts]
    texts = [bigram[line] for line in texts]
    
    from nltk.stem import WordNetLemmatizer
    lemmatizer = WordNetLemmatizer()

    texts = [[word for word in lemmatizer.lemmatize(' '.join(line), pos='v').split()] for line in texts]
    return texts