'''
@author David Sullivan
Simple python script to generate a (parsed) ONS text corpus from indexed pages and train both fastText and
word2vec models.
'''
import os, re

def parse_corpus(content, stop=None):
    # Convert 'content' into properly formatted text corpus with stop word removal
    import text_processing

    print "Parsing text corpus..."

    stop = text_processing.get_stopwords()
    texts = text_processing.build_texts_as_list(content)
    texts = text_processing.process_texts(texts, stops=stop)

    print "Done"
    return texts


def generate_corpus(pages):
    # Gather some human written text into 'content'
    import markdown
    from string import punctuation
    from bs4 import BeautifulSoup
    from nltk.tokenize import sent_tokenize

    print "Generating text corpus..."

    pattern = "[, \-!?:]+"

    fix_encoding = lambda s: s.decode('utf8', 'ignore')

    def strip_punctuation(s):
        return ''.join(c for c in s if c not in punctuation)

    def markdown_to_text(md):
        extensions = ['extra', 'smarty']
        html = markdown.markdown(md, extensions=extensions, output_format='html5')
        soup = BeautifulSoup(html, "lxml")
        return soup.text

    content = []
    for page in pages:
        if ("description" in page):
            desc = page["description"]
            text = ""
            if ("summary" in desc):
                text += desc["summary"].strip()
        if ("sections" in page):
            # Convert markdown -> html -> text
            for section in page["sections"]:
                if ("markdown" in section):
                    text += markdown_to_text(section["markdown"])
        
        if (len(text) > 0):
            content.append(text)

            sent_tokenize_list = sent_tokenize(text)
            for sentence in sent_tokenize_list:
                if (len(filter(None, re.split(pattern, sentence))) > 10):
                    content.append(fix_encoding(sentence.encode("utf-8").strip()))

    print "Done"
    return content


def write_corpus(corpus, output_fname):
    # Writes the corpus to a txt file
    print "Writing text corpus..."
    with open(output_fname, "w") as f:
        for item in corpus:
            if (len(item) > 0):
                line = " ".join(i for i in item)
                # Replace multiple whitespace with single and encode to ascii
                s = re.sub( '\s+', ' ', line.encode("ascii", "ignore") ).strip()
                f.write("%s\n" % s)
    print "Done"


def train_models(corpus_file, output_name, models_dir):
    from utils import which

    # fastText training params
    lr = 0.05
    dim = 300
    ws = 5
    epoch = 5
    minCount = 5
    neg = 5
    loss = 'ns'
    t = 1e-4

    from gensim.models import Word2Vec, KeyedVectors
    from gensim.models.word2vec import Text8Corpus

    # Same values as used for fastText training above
    params = {
        'alpha': lr,
        'size': dim,
        'window': ws,
        'iter': epoch,
        'min_count': minCount,
        'sample': t,
        'sg': 1,
        'hs': 0,
        'negative': neg
    }

    # Check for fasttext binary in path
    fasttext = which("fasttext")
    if (fasttext is None):
        raise Exception("Unable to locate fasttext binary in $PATH")

    # Generate the models

    # fastText with ngrams
    output_file = '{:s}_ft'.format(output_name)
    print('Training fasttext on {:s} corpus..'.format(corpus_file))
    exe = "{fasttext} skipgram -input {corpus_file} -output {output}  -lr {lr} -dim {dim} -ws {ws} -epoch {epoch} -minCount {minCount} -neg {neg} -loss {loss} -t {t}"
    exe = exe.format(fasttext=fasttext, corpus_file=corpus_file, output=models_dir+output_file, lr=lr, dim=dim, ws=ws, epoch=epoch, minCount=minCount, neg=neg, loss=loss, t=t)
    os.system(exe)
        
    # fastText with NO ngrams
    output_file = '{:s}_ft_no_ng'.format(output_name)
    print('\nTraining fasttext on {:s} corpus (without char n-grams)..'.format(corpus_file))
    exe = "{fasttext} skipgram -input {corpus_file} -output {output}  -lr {lr} -dim {dim} -ws {ws} -epoch {epoch} -minCount {minCount} -neg {neg} -loss {loss} -t {t} -maxn 0"
    exe = exe.format(fasttext=fasttext, corpus_file=corpus_file, output=models_dir+output_file, lr=lr, dim=dim, ws=ws, epoch=epoch, minCount=minCount, neg=neg, loss=loss, t=t)
    os.system(exe)
        
    # Word2Vec
    output_file = '{:s}_gs'.format(output_name)
    print('\nTraining word2vec on {:s} corpus..'.format(corpus_file))
    
    # Text8Corpus class for reading space-separated words file
    gs_model = Word2Vec(Text8Corpus(corpus_file), **params)
    # Direct local variable lookup doesn't work properly with magic statements (%time)
    gs_model.wv.save_word2vec_format(os.path.join(models_dir, '{:s}.vec'.format(output_file)))
    print('\nSaved gensim model as {:s}.vec'.format(output_file))


def load_pages(use_mongo=True):
    # Load pages from disk/mongo
    pages = None
    print "Loading pages from " + "mongoDB" if use_mongo else "filesystem"
    if (use_mongo):
        import utils

        mongoClient = utils.getMongoDBClient("localhost", 27017)
        collection = mongoClient.local.pages

        query = {
            "sections": {
                "$exists": True
            }
        }
        cursor = collection.find(query)

        pages = []
        for doc in cursor:
            pages.append(doc)
    else:
        # Read from filesystem
        from file_scanner import FileScanner
        scanner = FileScanner()
        pages = scanner.load_pages()
    print "Done"
    return pages


def main(output_fname, models_dir, use_mongo=True):
    # Load the pages
    pages = load_pages(use_mongo=use_mongo)

    # Generate the corpus
    corpus = generate_corpus(pages)

    # Parse the corpus to prepare for model training
    texts = parse_corpus(corpus)

    # Write to disk
    write_corpus(texts, output_fname)

    # Train the models
    train_models(output_fname, 'ons', models_dir)


if __name__ == "__main__":
    import sys
    if (len(sys.argv) == 1):
        print "Usage: python %s <input_corpus_fname> <output_model_fname>" % sys.argv[0]
        sys.exit(1)
    output_fname = sys.argv[1]
    models_dir = sys.argv[2]

    main(output_fname, models_dir)