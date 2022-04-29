#!/bin/env python
#
# Example for using IOC info from ES/recsync.
#
# Loops over all channels and extracts some info,
# in the specific example counting PVs per IOC.
# Handles about 100k channels per second.
#
# Dependencies:
#   pip install elasticsearch

import pprint
pp = pprint.PrettyPrinter(indent=2)

from elasticsearch import Elasticsearch
es = Elasticsearch("http://localhost:9200")

def getProperty(source, propname):
    """Fetch a property from ES/recsync data
       source   - '_source' from ES result
       propname - name of one of the properties
       Returns value of that property or None if not found
    """
    if 'properties' in source:
        for prop in source['properties']:
            if prop['name'] == propname:
                return prop['value']
    return None

# result = es.count(index="channelfinder")
# print("Channel count estimate: %d" % result['count'])

# PV counts per IOC
iocs = dict()

# Hosts of IOCs
hosts = set()

# PV count
pvs = 0

# Restrict PV names to anything?
# Assuming that each IOC has a "Load" PV,
# just looking for those would speed up listing of IOCs
# (but could not count how many PVs per IOC)
# pv_search = {"wildcard": { "name": "*:IOC*:Load"}}
pv_search = None

# Helper for batching more than 10000 results
batch = None

while True:
    # Default result 'size' is 10, maximum 10000.
    # To get all results, keep fetching the next batch
    # via 'search_after', which is only supported when
    # using 'sort'. The pseudo-sort option "_doc" uses
    # the natural index order.
    result = es.search(index="channelfinder",
                       size=10000,
                       query=pv_search,
                       sort="_doc",
                       search_after=batch)
    # 'total' will always report the total channel count
    # print("Channel count: %d" % result['hits']['total']['value'])
    # pp.pprint(result)
    batch = None   
    for hit in result['hits']['hits']:
        data = hit['_source']
        # pp.pprint(data)
        name = data['name']
        ioc = getProperty(data, 'iocName')
        if ioc:
            # print("%6d: %s on %s" % (pvs, name, ioc))
            if ioc in iocs:
                iocs[ioc] = iocs[ioc] + 1
            else:
                iocs[ioc] = 1
            pvs += 1
        host = getProperty(data, 'hostName')
        if host:
            hosts.add(host)
        # Does result include a token for 'search_after'?
        if "sort" in hit:
            batch = hit['sort']
    # Continue with the last batch token, or quit
    if batch is None:
        break
    # print("++++++++++> Continue after " + str(batch))

sorted_iocs = list()
for name in iocs:
    sorted_iocs.append({ 'name': name, 'pvs': iocs[name]})

print("#   IOC                          PV Count")
print("-----------------------------------------")
i = 1
for ioc in sorted(sorted_iocs, key=lambda item: item['pvs'], reverse=True):
    print("%3d %-30s %6d" % (i, ioc['name'], ioc['pvs']))
    i += 1

print("\nPV Count: %d" % pvs)

print("\nHosts")
print("-----------------------------------------")
for host in sorted(hosts):
    print(host)

es.close()
