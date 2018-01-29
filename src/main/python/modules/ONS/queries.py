import copy

true=True
false=False

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

def getSimpleBaseQuery(searchTerm, similarTerms, rescoreQueries, fromParam, sizeParam, sort=None):

    baseQuery = {
      "from": fromParam,
      "size": sizeParam,
      "query": {
        "function_score": {
          "query": {
            "bool": {
              "should": [
                {
                  "match": {
                    "description.title": {
                      "query": searchTerm,
                      "operator": "OR",
                      "prefix_length": 0,
                      "max_expansions": 50,
                      "fuzzy_transpositions": true,
                      "lenient": false,
                      "zero_terms_query": "NONE",
                      "boost": 10.0
                    }
                  }
                },
                {
                  "terms": {
                    "description.title": similarTerms,
                    "boost": 5.0
                  }
                },
                {
                  "match": {
                    "_all": {
                      "query": searchTerm,
                      "operator": "OR",
                      "prefix_length": 0,
                      "max_expansions": 50,
                      "fuzzy_transpositions": true,
                      "lenient": false,
                      "zero_terms_query": "NONE",
                      "boost": 1.0
                    }
                  }
                }
              ],
              "disable_coord": false,
              "adjust_pure_negative": true,
              "boost": 1.0
            }
          },
          "functions": [
            {
              "filter": {
                "match_all": {
                  "boost": 1.0
                }
              },
              "weight": 0.5,
              "exp": {
                "description.releaseDate": {
                  "origin": "now",
                  "scale": "17472h",
                  "decay": 0.5
                },
                "multi_value_mode": "AVG"
              }
            }
          ],
          "score_mode": "multiply",
          "boost_mode": "avg",
          "max_boost": 100.0,
          "min_score": 1.0,
          "boost": 1.0
        }
      },
      "rescore": rescoreQueries
    }

    if (sort is not None):
        baseQuery["sort"] = sort

    return baseQuery

def getSltrBaseQuery(searchTerm, rescoreQueries, fromParam, sizeParam, sort=None):

    baseQuery = {
          "from": fromParam,
          "size": sizeParam,
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
                      "fuzzy_transpositions": true,
                      "lenient": false,
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
                      "description.summary^1.0"
                    ],
                    "type": "best_fields",
                    "operator": "OR",
                    "slop": 0,
                    "prefix_length": 0,
                    "max_expansions": 50,
                    "lenient": false,
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
                    "lenient": false,
                    "zero_terms_query": "NONE",
                    "boost": 1.0
                  }
                }
              ],
              "boost": 1.0
            }
          },
          "rescore": rescoreQueries
        }

    if (sort is not None):
        baseQuery["sort"] = sort

    return baseQuery

def getBaseQuery(searchTerm, rescoreQueries, fromParam, sizeParam, sort=None):

    baseQuery = {
      "from": fromParam,
      "size": sizeParam,
      "query": {
        "dis_max": {
          "tie_breaker": 0.0,
          "queries": [
            {
              "bool": {
                "should": [
                  {
                    "match": {
                      "description.title.title_no_dates": {
                        "query": searchTerm,
                        "operator": "OR",
                        "prefix_length": 0,
                        "max_expansions": 50,
                        "minimum_should_match": "1<-2 3<80% 5<60%",
                        "fuzzy_transpositions": true,
                        "lenient": false,
                        "zero_terms_query": "NONE",
                        "boost": 10.0
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
                        "minimum_should_match": "1<-2 3<80% 5<60%",
                        "fuzzy_transpositions": true,
                        "lenient": false,
                        "zero_terms_query": "NONE",
                        "boost": 10.0
                      }
                    }
                  },
                  {
                    "multi_match": {
                      "query": searchTerm,
                      "fields": [
                        "description.edition^1.0",
                        "description.title^10.0"
                      ],
                      "type": "cross_fields",
                      "operator": "OR",
                      "slop": 0,
                      "prefix_length": 0,
                      "max_expansions": 50,
                      "minimum_should_match": "3<80% 5<60%",
                      "lenient": false,
                      "zero_terms_query": "NONE",
                      "boost": 1.0
                    }
                  }
                ],
                "disable_coord": false,
                "adjust_pure_negative": true,
                "boost": 1.0
              }
            },
            {
              "multi_match": {
                "query": searchTerm,
                "fields": [
                  "description.metaDescription^1.0",
                  "description.summary^1.0"
                ],
                "type": "best_fields",
                "operator": "OR",
                "slop": 0,
                "prefix_length": 0,
                "max_expansions": 50,
                "minimum_should_match": "75%",
                "lenient": false,
                "zero_terms_query": "NONE",
                "boost": 1.0
              }
            },
            {
              "match": {
                "description.keywords": {
                  "query": searchTerm,
                  "operator": "AND",
                  "prefix_length": 0,
                  "max_expansions": 50,
                  "fuzzy_transpositions": true,
                  "lenient": false,
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
                  "description.datasetId^1.0"
                ],
                "type": "best_fields",
                "operator": "OR",
                "slop": 0,
                "prefix_length": 0,
                "max_expansions": 50,
                "lenient": false,
                "zero_terms_query": "NONE",
                "boost": 1.0
              }
            },
            {
              "match": {
                "searchBoost": {
                  "query": searchTerm,
                  "operator": "AND",
                  "prefix_length": 0,
                  "max_expansions": 50,
                  "fuzzy_transpositions": true,
                  "lenient": false,
                  "zero_terms_query": "NONE",
                  "boost": 100.0
                }
              }
            }
          ],
          "boost": 1.0
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
