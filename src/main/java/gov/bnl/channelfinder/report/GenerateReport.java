package gov.bnl.channelfinder.report;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.bnl.channelfinder.XmlChannel;
import gov.bnl.channelfinder.XmlProperty;
import gov.bnl.channelfinder.XmlTag;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.ClearScrollResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

/**
 * A management tools for producing reports and analysis on the channels in channefinder.
 */
public class GenerateReport {

    private static RestHighLevelClient searchClient;

    public static void createReport(String es_host, int es_port) throws IOException {
        try {

            searchClient =  new RestHighLevelClient(RestClient.builder(new HttpHost(es_host, es_port, "http")));

            QueryBuilder qb = matchAllQuery();

            final Scroll scroll = new Scroll(TimeValue.timeValueMinutes(30L));


            SearchRequest searchRequest = new SearchRequest("channelfinder");
            searchRequest.scroll(scroll);

            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(qb);
            searchSourceBuilder.size(1000);
            searchRequest.source(searchSourceBuilder);
            SearchResponse searchResponse = searchClient.search(searchRequest, RequestOptions.DEFAULT);

            String scrollId = searchResponse.getScrollId();
            SearchHit[] searchHits = searchResponse.getHits().getHits();


            final Set<String> pvs = new HashSet<String>();
            // { propertyName : { propertyValue : [channels]},,,} ,,,}
            final Map<String, Map<String, Set<String>>> propertyValues = new HashMap<String, Map<String, Set<String>>>();

            final ObjectMapper mapper = new ObjectMapper();
            mapper.addMixIn(XmlProperty.class, OnlyXmlProperty.class);
            mapper.addMixIn(XmlTag.class, OnlyXmlTag.class);

            while (searchHits != null && searchHits.length > 0) {

                for (SearchHit hit : searchResponse.getHits().getHits()) {
                    XmlChannel ch = mapper.readValue(hit.getSourceAsString(), XmlChannel.class);
                    pvs.add(ch.getName());
                    // Process the channels based on the names

                    // Process the channels based on properties
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

                SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
                scrollRequest.scroll(scroll);
                searchResponse = searchClient.scroll(scrollRequest, RequestOptions.DEFAULT);
                scrollId = searchResponse.getScrollId();
                searchHits = searchResponse.getHits().getHits();

            }

            ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
            clearScrollRequest.addScrollId(scrollId);
            ClearScrollResponse clearScrollResponse = searchClient.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
            boolean succeeded = clearScrollResponse.isSucceeded();

            // System.out.println(qbResult.getHits().getHits().length);
            // Break condition: No hits are returned
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
