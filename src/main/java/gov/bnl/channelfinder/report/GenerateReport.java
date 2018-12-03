package gov.bnl.channelfinder.report;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.bnl.channelfinder.XmlChannel;
import gov.bnl.channelfinder.XmlProperty;
import gov.bnl.channelfinder.XmlTag;

public class GenerateReport {

    private static TransportClient searchClient;

    public static void createReport(String es_host, int es_port) {
        try {
            searchClient = new TransportClient();

            searchClient.addTransportAddress(new InetSocketTransportAddress(es_host, es_port));

            QueryBuilder qb = matchAllQuery();

            SearchResponse qbResult = searchClient.prepareSearch("channelfinder").setScroll(new TimeValue(60000))
                    .setQuery(qb).setSize(100).execute().actionGet();

            final Set<String> pvs = new HashSet<String>();
            // { propertyName : { propertyValue : [channels]},,,} ,,,}
            final Map<String, Map<String, Set<String>>> propertyValues = new HashMap<String, Map<String, Set<String>>>();

            final ObjectMapper mapper = new ObjectMapper();
            mapper.addMixIn(XmlProperty.class, OnlyXmlProperty.class);
            mapper.addMixIn(XmlTag.class, OnlyXmlTag.class);

            while (true) {

                for (SearchHit hit : qbResult.getHits().getHits()) {
                    // Handle the hit...
                    XmlChannel ch = mapper.readValue(hit.source(), XmlChannel.class);
                    // System.out.println(ch);
                    pvs.add(ch.getName());
                    ch.getProperties().stream().forEach((property) -> {
                        if (propertyValues.containsKey(property.getName())) {
                            if (propertyValues.get(property.getName()).containsKey(property.getValue())) {
                                propertyValues.get(property.getName()).get(property.getValue()).add(ch.getName());
                            } else {
                                Set<String> list = new HashSet<String>();
                                list.add(ch.getName());
                                propertyValues.get(property.getName()).put(property.getValue(), list);
                            }
                        } else {
                            Map<String, Set<String>> map = new HashMap<String, Set<String>>();
                            Set<String> list = new HashSet<String>();
                            list.add(ch.getName());
                            map.put(property.getValue(), list);
                            propertyValues.put(property.getName(), map);
                        }
                    });
                }
                qbResult = searchClient.prepareSearchScroll(qbResult.getScrollId()).setScroll(new TimeValue(600000))
                        .execute().actionGet();

                // System.out.println(qbResult.getHits().getHits().length);
                // Break condition: No hits are returned
                if (qbResult.getHits().getHits().length == 0) {
                    System.out.println("Unique Channels:" + pvs.size());
//                    pvs.forEach(System.out::println);
                    System.out.println("Unique Hosts:" + propertyValues.get("hostName").keySet().size());
                    System.out.println(propertyValues.get("hostName").entrySet().stream().map(entry -> {
                        return entry.getKey() + ":" + entry.getValue().size();
                    }).collect(Collectors.joining(",")));
                    System.out.println("Unique iocid:" + propertyValues.get("iocid").size());
                    System.out.println(propertyValues.get("iocid").entrySet().stream().map(entry -> {
                        return entry.getKey() + ":" + entry.getValue().size();
                    }).collect(Collectors.joining(",")));
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            searchClient.close();
        }
    }

    abstract class OnlyXmlProperty {
        @JsonIgnore
        private List<XmlChannel> channels;
    }

    abstract class OnlyXmlTag {
        @JsonIgnore
        private List<XmlChannel> channels;
    }
}
