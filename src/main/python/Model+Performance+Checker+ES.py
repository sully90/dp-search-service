
# coding: utf-8

# In[38]:


from modules.ONS import queries
reload(queries)

index = "ons1516722910092"
store = "ons_featurestore_1516779508509"

# models = []
# for i in range(9):
#     modelName = "ons_model_%d" % (i+1)
#     models.append(modelName)
models = ["ons_model_9"]

boosts = [1.0]*len(models)
queryWeights = [0.5]*len(models)
rescoreWeights = [1.0]*len(models)

size = 10

searchTerms = ["rpi", "gender pay gap", "cpi", "gdp", "inflation", "crime", "unemployment", 
              "population", "immigration", "mental health", "london", "london population", 
              "retail price index", "life expectancy", "obesity", "religion", "migration", 
              "poverty", "social media", "employment"]

qidDict = {}
for i in range(len(searchTerms)):
    qidDict[i+1] = searchTerms[i]


# In[39]:


from elasticsearch import Elasticsearch

esUrl = "http://localhost:9200"
esClient = Elasticsearch(esUrl, timeout=1000)

from pymongo import MongoClient, ASCENDING, DESCENDING
mongoClient = MongoClient('localhost', 27017)

db = mongoClient.local
collection = db.judgements

def mergeDicts(x, y):
    z = x.copy()   # start with x's keys and values
    z.update(y)    # modifies z with y's keys and values & returns None
    return z

def termQuery(term):
    return {"term": term}

def timeQuery(dateTime):
    return {"timeStamp": dateTime}

# Get date of most recent entries
doc = collection.find().sort([("timeStamp", DESCENDING)]).limit(1).next()
timeStamp = doc["timeStamp"]
    
timeStampQuery = timeQuery(timeStamp)


# In[40]:


import numpy as np

MAX_SCORE = 4.0

def idealJudgement(num):
    i = 0
    incremenet = (1.0 / (float(num) - 1.0)) * num
    
    iJ = np.zeros(num)
    val = len(iJ)
    while (val > 0):
        iJ[i] = (val / float(num)) * MAX_SCORE
        i += 1
        val -= incremenet
        
    return iJ

def idealDiscountedCumulativeGain(num):
    idealGain = idealJudgement(num)
    iDCG = np.zeros(num)
    
    total = 0.0
    for i in range(num):
        total += idealGain[i] / float(i+1)
        iDCG[i] = total
    return iDCG

class Judgements(object):
    def __init__(self, judgements):
        self.judgements = judgements
        
    def dcg(self):
        total = 0.0
        
        dcg = []
        
        for i in range(len(self.judgements)):
            judgement = self.judgements[i]
            total += judgement["judgement"] / float(judgement["rank"])
            dcg.append(total)
            
        return np.array(dcg)
    
    def ndcg(self):
        
        dcg = self.dcg()
        idcg = idealDiscountedCumulativeGain(len(dcg))
        
        ndcg = np.zeros(len(dcg))
        
        for i in range(len(ndcg)):
            ndcg[i] = min(1.0, dcg[i] / idcg[i])
        return ndcg
    
    def __iter__(self):
        return self.judgements.__iter__()
    
    def __getitem__(self, i):
        return self.judgements[i]
    
    def __len__(self):
        return len(self.judgements)
    
    def remove(self, item):
        self.judgements.remove(item)


# In[41]:


import copy, json

def processTerm(searchTerm, boosts, queryWeights, rescoreWeights, pages=10):
    mongoQuery = mergeDicts(termQuery(searchTerm), timeStampQuery)
    cursor = collection.find(mongoQuery)
    judgementCount = cursor.count()
    
#     print judgementCount
    
    if (judgementCount > 0):
        judgements = cursor.next()
        modelJudgements = copy.deepcopy(judgements)

        keep = []
        judgementList = modelJudgements["judgements"]["judgementList"]
        
        # Reset ranks
        for judgement in judgementList:
            judgement["rank"] = -1
        
        rescoreQueries = queries.getRescoreQueriesForModels(store, searchTerm, models,
                                        boosts, queryWeights, rescoreWeights)
        for page in range(1, pages+1):
            fromParam = (page - 1) * size
            esQuery = queries.getBaseQuery(searchTerm, rescoreQueries, fromParam, size)
#             print json.dumps(esQuery)

            hits = esClient.search(index=index, body=esQuery)
            
#             print "Got %d hits for page %d" % (len(hits["hits"]["hits"]), page)
            
            searchResults = []
            for hit in hits["hits"]["hits"]:
                searchResults.append( hit["_id"] )
            
            # Check for matches
            for judgement in judgementList:
                url = judgement["attrs"]["uri"]
                if (url in searchResults):
                    newRank = int(((page - 1) * 10.0) + searchResults.index(url) + 1)
                    judgement["rank"] = newRank
                    j = copy.deepcopy(judgement)
                    j["rank"] = newRank
                    keep.append(j)
                    
        if (len(keep) > 1):
            judgementList = Judgements(keep)
            ndcg = judgementList.ndcg()
#             print searchTerm, ndcg.mean()
            return ndcg.mean()
    else:
        return 0.0

# for searchTerm in searchTerms:
#     ndcg = processTerm(searchTerm, boosts, queryWeights, rescoreWeights)
#     print searchTerm, ndcg


def fn(searchTerm, X):
#     ndcg = processTerm(term, boosts, queryWeights, rescoreWeights)
    ndcg = processTerm(searchTerm, boosts, queryWeights, X)
    if (ndcg is None):
        return 0.0
    return ndcg

def optFn(searchTerm, X):
    ndcg = fn(searchTerm, X)
    if (ndcg is None):
        return 1.0
    return 1.0 - ndcg


# In[ ]:


import random
from scipy import optimize

x0 = [random.random() for i in range(len(models))]
bounds = ((0.0, 1.0), (0.0, 1.0))

resDict = {}

options = {"maxiter": 100, "disp": True}

print "Optimizing..."
searchTerm = "rpi"
termFn = lambda x: optFn(searchTerm, x)
res = optimize.brute(termFn, bounds)
resDict[searchTerm] = res

# for searchTerm in searchTerms:
#     termFn = lambda X: optFn(searchTerm, X)
#     res = minimize(termFn, X, method='Nelder-Mead', tol=1e-6)
#     resDict[searchTerm] = res


# In[ ]:


res = resDict[searchTerm]
print res

