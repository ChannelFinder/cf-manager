#!/bin/bash
#
# Provides rough PV count by simply counting all entries in the channel finder index.
#
# Pipeline then extracts just the count

curl -s http://localhost:9200/channelfinder/_count?pretty | fgrep count | sed -e 's/,//'
