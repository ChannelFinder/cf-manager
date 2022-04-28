#!/bin/bash
#
# Lists all IOCs
#
# Basic query of channelfinder index data looks like this:
#
#   "name" : "SomeChannelName",
#   "properties" : [
#    {
#       "name" : "time",
#       "value" : "2022-..."
#    },
#    {
#       "name" : "iocName",
#       "value" : "TheIOCName"
#    },
#
# The query must thus be "nested" to locate all entries with an "iocName",
# and the aggregation to list all IOCs needs to be nested as well.
#
# https://www.elastic.co/guide/en/elasticsearch/reference/8.1/query-dsl-nested-query.html
# https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-bucket-nested-aggregation.html
#
# Remaining pipe extracts just the IOC names and prints them with "line" number

curl -s -XGET "http://localhost:9200/channelfinder/_search?pretty" -H 'Content-Type: application/json' -d'
{
  "query":
  {
    "nested":
    {
      "path": "properties",
      "query": { "match": { "properties.name": "iocName" } }
    }
  },
  "size": 1,
  "aggs":
  {
    "IOCs":
    {
      "nested": { "path": "properties" },
      "aggs":
      {
        "filter_ioc":
        {
          "filter": { "bool": { "filter": [ { "term": { "properties.name": "iocName" } } ] } },
          "aggs": { "ioc": { "terms": { "field": "properties.value", "size": 500 } } }
        }
      }
    }      
  }
}' | fgrep key | cut -d: -f 2 | sort | cat -n | sed -e 's/[",]//g'

