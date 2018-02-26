searchTerms = ["rpi", "gender pay gap", "cpi", "gdp", "inflation", "crime", "unemployment", 
              "population", "immigration", "mental health", "london", "london population", 
              "retail price index", "life expectancy", "obesity", "religion", "migration", 
              "poverty", "social media", "employment"]

def getSearchClient(esURL, timeout=1000):
    from elasticsearch import Elasticsearch
    return Elasticsearch(esURL, timeout=timeout)

def getMongoDBClient(mongoUrl, port):
    from pymongo import MongoClient
    return MongoClient(mongoUrl, port)

def getMostRecentJudgements(collection):
    from pymongo import DESCENDING

    doc = collection.find().sort([("timeStamp", DESCENDING)]).limit(1).next()
    return doc

def mergeDicts(x, y):
    z = x.copy()   # start with x's keys and values
    z.update(y)    # modifies z with y's keys and values & returns None
    return z

def termQuery(term):
    return {"term": term}

def timeQuery(dateTime):
    return {"timeStamp": dateTime}

def powerset(s):
    x = len(s)
    masks = [1 << i for i in range(x)]
    for i in range(1 << x):
        yield [ss for mask, ss in zip(masks, s) if i & mask]

def which(file):
    import os
    for path in os.environ["PATH"].split(os.pathsep):
        if os.path.exists(os.path.join(path, file)):
                return os.path.join(path, file)

    return None

class Model(object):
    def __init__(self, name, boost=1.0, queryBoost=0.5, rescoreBoost=1.0):
        self.name = name
        self.boost = boost
        self.queryBoost = queryBoost
        self.rescoreBoost = rescoreBoost

    def __str__(self):
        return self.name

    def __repr__(self):
        return self.name