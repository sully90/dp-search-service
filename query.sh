#!/bin/bash

QUERY_FILE=$1

curl -H "Content-Type: application/json" -X POST -d @${QUERY_FILE} http://localhost:8080/SearchEngine/api/ltr/sltr/bulletin/page_features/
