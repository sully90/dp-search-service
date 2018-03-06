#! /bin/bash

export LEXVEX_ROOT="/Users/sullid/golang/src/github.com/alexandres/lexvec"
MIN_FREQ=15

lexvec -corpus ~/ONS/dp-search-service/src/main/python/ons_corpus.txt -output models/ons_lv.vec -dim 300 -window 5 -subsample 1e-5 -negative 5 -iterations 5 -minfreq ${MIN_FREQ} -matrix ppmi -model 0

echo "Merging..."
python ${LEXVEX_ROOT}/merge_context_vectors.py models/ons_lv.vec
