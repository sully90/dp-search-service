import copy

_rescoreTemplate = {
    "window_size": 1000,
    "query": {
        "rescore_query": {
            "sltr": {
                "model": "",
                "featureset": "ons_features_prod",
                "store": "ons_featurestore_1516614381132",
                "params": {
                    "keywords": ""
                },
                "boost": 1.0
            }
        },
        "query_weight": 0.5,
        "rescore_query_weight": 0.0,
        "score_mode": "total"
    }
}

def getBaseQuery(searchTerm, rescoreQueries, fromParam, sizeParam):

    baseQuery = {
      "from": fromParam,
      "size": sizeParam,
      "query": {
        "function_score": {
          "query": {
            "dis_max": {
              "tie_breaker": 0.0,
              "queries": [
                {
                  "match": {
                    "description.title.title_no_dates": {
                      "query": searchTerm,
                      "operator": "OR",
                      "prefix_length": 0,
                      "max_expansions": 50,
                      "fuzzy_transpositions": True,
                      "lenient": False,
                      "zero_terms_query": "NONE",
                      "boost": 1.0
                    }
                  }
                },
                {
                  "match": {
                    "description.title.title_no_stem": {
                      "query": searchTerm,
                      "operator": "OR",
                      "prefix_length": 0,
                      "max_expansions": 50,
                      "fuzzy_transpositions": True,
                      "lenient": False,
                      "zero_terms_query": "NONE",
                      "boost": 1.0
                    }
                  }
                },
                {
                  "multi_match": {
                    "query": searchTerm,
                    "fields": [
                      "description.cdid^1.0",
                      "description.datasetId^1.0",
                      "description.edition^1.0",
                      "description.keywords^1.0",
                      "description.metaDescription^1.0",
                      "description.summary^1.0",
                      "description.title^1.0"
                    ],
                    "type": "best_fields",
                    "operator": "OR",
                    "slop": 0,
                    "prefix_length": 0,
                    "max_expansions": 50,
                    "lenient": False,
                    "zero_terms_query": "NONE",
                    "boost": 1.0
                  }
                },
                {
                  "multi_match": {
                    "query": searchTerm,
                    "fields": [
                      "entities.dates^100.0",
                      "entities.locations^100.0",
                      "entities.organizations^100.0",
                      "entities.persons^100.0"
                    ],
                    "type": "best_fields",
                    "operator": "OR",
                    "slop": 0,
                    "prefix_length": 0,
                    "max_expansions": 50,
                    "lenient": False,
                    "zero_terms_query": "NONE",
                    "boost": 1.0
                  }
                }
              ],
              "boost": 1.0
            }
          }
        }
      },
      "rescore": rescoreQueries
    }

    return baseQuery

def getRescoreQueriesForModels(keywords, models, rescoreWeights):
    rescoreQueries = []

    for model,weight in zip(models, rescoreWeights):
        rescoreQuery = copy.deepcopy(_rescoreTemplate)

        rescoreQuery["query"]["rescore_query_weight"] = weight
        rescoreQuery["query"]["rescore_query"]["sltr"]["model"] = model
        rescoreQuery["query"]["rescore_query"]["sltr"]["params"]["keywords"] = keywords
        rescoreQueries.append(rescoreQuery)

    return rescoreQueries

models = []
for i in range(9):
    models.append("ons_model_%d" % (i+1))
models.append("all")
