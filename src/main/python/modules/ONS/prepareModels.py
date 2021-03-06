'''
Analyses the performance for all possible combinations of ONS models and stores them in mongoDB to be used by Babbage
'''
import numpy as np
import utils, queries, copy
from utils import Model
from judgements import Judgements

_DEFAULT_BOOST = 1.0
_DEFAULT_QUERY_BOOST = 0.5
_DEFAULT_RESCORE_BOOST = 1.0

_N_CALLS = 50

_NDCG_COST_FUNC = lambda ndcg: 1.0 - ndcg

_WORD_2_VEC_MODEL = word2vec("../../../resources/vectorModels/GoogleNews-vectors-negative300-SLIM.bin", True)

def computeNdcg(searchTerm, models, judgementList, index, elasticClient, size, pages):
    keep = []
    for page in pages:
        fromParam = (page - 1) * size

        # Generate the query
        searchQuery = getSearchQuery(searchTerm, models, fromParam, size)
        # Do the search
        searchResults = elasticClient.search(index=index, body=searchQuery)

        # Get the page urls from the hits
        urls = []
        for hit in searchResults["hits"]["hits"]:
            urls.append( hit["_id"] )

        # Check for matches
        for judgement in judgementList:
            url = judgement["attrs"]["uri"]
            if (url in urls):
                newRank = int(((page - 1) * 10.0) + urls.index(url) + 1)
                judgement["rank"] = newRank
                j = copy.deepcopy(judgement)
                j["rank"] = newRank
                keep.append(j)

    # Compute NDCG
    if (len(keep) > 1):
        judgementList = Judgements(keep)
        ndcg = judgementList.ndcg()
        return ndcg.mean()
    return 0.0

def _process(x0, searchTerm, inputModels, judgementList, index, elasticClient, size, pages):
    models = []
    if (len(inputModels) > 1):
        for model,param in zip(inputModels, np.split(np.array(x0), len(inputModels))):
            b,q,r = param
            models.append(Model(model.name, b, q, r))
    else:
        model = inputModels[0]
        b,q,r = x0
        models.append(Model(model.name, b, q, r))
    return computeNdcg(searchTerm, models, judgementList, index, elasticClient, size, pages)

def optimise(searchTerm, models, judgementList, index, elasticClient, size, pages, **kwargs):
    '''
    Minimises the NDCG cost function to optimise performance
    '''
    from skopt import gp_minimize

    if (kwargs.get("verbose", False)): print "Optimizing over models/term: %s/%s" % (models, searchTerm)

    bounds = [(0.0, 10.0), (0.0, 1.0), (0.0, 100.0)]*len(models)
    minFunc = lambda x: _NDCG_COST_FUNC(_process(x, searchTerm, models, judgementList, index, elasticClient, size, pages))

    n_calls = kwargs.pop("n_calls", _N_CALLS)
    res = gp_minimize(minFunc, bounds, n_calls=n_calls, **kwargs)
    return res

def getSearchQuery(searchTerm, models, fromParam, size):
    # import json
    rescoreQueries = []
    for model in models:
        name = model.name
        boost = model.boost
        queryBoost = model.queryBoost
        rescoreBoost = model.rescoreBoost

        rescoreQuery = queries.getRescoreQueryForModel(store, searchTerm, name,
                boost, queryBoost, rescoreBoost)
        rescoreQueries.append(rescoreQuery)

    similarTerms = []
    tokens = searchTerm.split(" ")
    for token in tokens:
        results = _WORD_2_VEC_MODEL.
        similarTerms.extend()
    similarTerms = _WORD_2_VEC_MODEL.similar_by_word

    # esQuery = queries.getBaseQuery(searchTerm, rescoreQueries, fromParam, size)
    # esQuery = queries.getSltrBaseQuery(searchTerm, rescoreQueries, fromParam, size)
    esQuery = queries.getSimpleBaseQuery(searchTerm, rescoreQueries, fromParam, size)
    # print json.dumps(esQuery)
    return esQuery

def word2vec(fileName, binary=True):
    import gensim
    model = gensim.models.KeyedVectors.load_word2vec_format(fname, binary=binary)
    return model

def main(store, index="ons*"):
    searchTerms = utils.searchTerms
    pages = range(1, 11)  # 10 pages
    size = 10

    # Load the elasticsearch and mongodb clients
    elasticClient = utils.getSearchClient("http://localhost:9200")
    mongoClient = utils.getMongoDBClient("localhost", 27017)
    collection = mongoClient.local.judgements

    # Get timestamp for judgements
    doc = utils.getMostRecentJudgements(collection)
    timeStamp = doc["timeStamp"]
    timeStampQuery = utils.timeQuery(timeStamp)

    enabledModels = [1,3,9]
    models = [Model("ons_model_%d" % i) for i in enabledModels]
    modelPowerset = list(utils.powerset(models))

    # Load all judgements
    judgementsDict = {}
    for searchTerm in searchTerms:
        termQuery = utils.termQuery(searchTerm)
        mongoQuery = utils.mergeDicts(termQuery, timeStampQuery)
        cursor = collection.find(mongoQuery)
        if (cursor.count() > 0):
            judgements = cursor.next()
            judgementsDict[searchTerm] = judgements["judgements"]["judgementList"]

    # Compute the performance of all combinations
    # enabledModels = [1,2,3,7,8,9]
    # modelPowerset = [[Model("ons_model_%d" % i) for i in enabledModels]]
    results = {}
    for powerset in modelPowerset:
        if len(powerset) == 0:
            continue
        sumNdcgBefore = 0.0
        sumNdcgAfter = 0.0
        count = 0.0
        for searchTerm in judgementsDict:
            judgements = judgementsDict[searchTerm]
            # ndcg = computeNdcg(searchTerm, powerset, judgements, index, elasticClient, size, pages)
            # print powerset, searchTerm, ndcg
            res = optimise(searchTerm, powerset, judgements, index, elasticClient, size, pages)
            params = res.x
            defaultNdcgParams = [1.0, 0.5, 1.0]*len(powerset)

            defaultNdcg = _process(defaultNdcgParams, searchTerm, powerset, judgements, index, elasticClient, size, pages)
            optimisedNdcg = _process(params, searchTerm, powerset, judgements, index, elasticClient, size, pages)
            print powerset, searchTerm, defaultNdcg, optimisedNdcg

            sumNdcgBefore += defaultNdcg
            sumNdcgAfter += optimisedNdcg
            count += 1.0

            powerSetKey = str(powerset)

            if (searchTerm not in results):
                results[searchTerm] = {}
            results[searchTerm][powerSetKey] = params
        print powerset, "Mean NDCG=", sumNdcgBefore/count, sumNdcgAfter/count
        print

    return results

if __name__ == "__main__":
    import sys

    results = None
    if (len(sys.argv) == 1):
        usage(sys)
    store = sys.argv[1]
    if (len(sys.argv) == 3):
        index = sys.argv[2]
        results = main(store)
    else:
        results = main(store)

def usage(sys):
    print "Usage: python %s <featureStore> <(optional) ons_index>" % sys.argv[0]
    sys.exit(1)
