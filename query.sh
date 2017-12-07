#!/bin/bash

KEYWORDS=$1
QUERY_FILE=$2

curl -H "Content-Type: application/json" -X POST -d @${QUERY_FILE} http://localhost:8080/SearchEngine/api/ltr/sltr/bulletin/bulletin_features/${KEYWORDS}
