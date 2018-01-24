import copy

_rescoreTemplate = {
    "window_size": 1000,
    "query": {
        "rescore_query": {
            "sltr": {
                "model": "",
                "featureset": "ons_features_prod",
                "store": "",
                "params": {
                    "keywords": ""
                },
                "boost": 0.0
            }
        },
        "query_weight": 0.0,
        "rescore_query_weight": 0.0,
        "score_mode": "max"
    }
}

def sortQuery(field, order):
    sortBy = {
        field : {
            "order" : order
        }
    }
    return sortBy

def getBaseQuery(searchTerm, rescoreQueries, fromParam, sizeParam, sort=None):

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

    if (sort is not None):
        baseQuery["sort"] = sort

    return baseQuery

def getRescoreQueryForModel(featureStore, keywords, model, boost, queryWeight, rescoreWeight):
    rescoreQuery = copy.deepcopy(_rescoreTemplate)

    rescoreQuery["query"]["rescore_query"]["sltr"]["store"] = featureStore
    rescoreQuery["query"]["rescore_query"]["sltr"]["boost"] = boost
    rescoreQuery["query"]["query_weight"] = queryWeight
    rescoreQuery["query"]["rescore_query_weight"] = rescoreWeight

    rescoreQuery["query"]["rescore_query"]["sltr"]["model"] = model
    rescoreQuery["query"]["rescore_query"]["sltr"]["params"]["keywords"] = keywords

    return rescoreQuery

models = []
for i in range(9):
    models.append("ons_model_%d" % (i+1))
models.append("all")
