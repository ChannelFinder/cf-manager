package gov.bnl.channelfinder.report;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.disMaxQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.nestedQuery;
import static org.elasticsearch.index.query.QueryBuilders.wildcardQuery;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

import org.apache.http.HttpHost;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.ClearScrollResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.DisMaxQueryBuilder;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.bnl.channelfinder.XmlChannel;
import gov.bnl.channelfinder.XmlProperty;
import gov.bnl.channelfinder.XmlTag;

/**
 * A management tools for producing reports and analysis on the channels in channefinder.
 */
public class GenerateReport {

    private static ExecutorService executorService = Executors.newFixedThreadPool(16);
    private static RestHighLevelClient searchClient;

    private static ServiceLoader<PVNamesProcessor> processPVNamesServiceLoader = ServiceLoader.load(PVNamesProcessor.class);

    public static void createReport(String es_host, int es_port) throws IOException {
        createReport(es_host, es_port, false, null, null);
    }

    public static void createReport(String es_host, int es_port, boolean excludeInactive, String nameQueryPattern, String timeQueryPattern) throws IOException {
        try {
            StringBuilder reportFileName = new StringBuilder("cf_report");

            searchClient =  new RestHighLevelClient(RestClient.builder(new HttpHost(es_host, es_port, "http")));

            BoolQueryBuilder qb = boolQuery();
            if (nameQueryPattern != null && !nameQueryPattern.isBlank()) {
                DisMaxQueryBuilder nameQuery = disMaxQuery();
                nameQuery.add(wildcardQuery("name", nameQueryPattern.trim()));
                qb.must(nameQuery);
                reportFileName.append("-"+nameQueryPattern.replaceAll("[:\\*\\%]", "").trim());
            }

            if (timeQueryPattern != null && !timeQueryPattern.isBlank()) {
                DisMaxQueryBuilder timeQuery = disMaxQuery();
                timeQuery
                    .add(nestedQuery("properties",
                        boolQuery().must(matchQuery("properties.name", "time"))
                                   .must(wildcardQuery("properties.value", timeQueryPattern.trim())),
                        ScoreMode.None));
                qb.must(timeQuery);
                reportFileName.append("-"+timeQueryPattern.replaceAll("[:\\*\\%]", "").trim());
            } else {
                LocalDate localDate = LocalDate.ofInstant(Instant.now(), ZoneId.systemDefault());
                reportFileName.append("-"+localDate.toString().trim());
            }

            if (excludeInactive) {
                DisMaxQueryBuilder propertyQuery = disMaxQuery();
                propertyQuery
                        .add(nestedQuery("properties",
                                boolQuery().must(matchQuery("properties.name", "pvStatus"))
                                           .must(wildcardQuery("properties.value", "Active")),
                                ScoreMode.None));
                qb.must(propertyQuery);
            }

            if (!excludeInactive || nameQueryPattern == null || nameQueryPattern.isBlank()) {
                qb.must(matchAllQuery());
            }

            final Scroll scroll = new Scroll(TimeValue.timeValueMinutes(30L));


            SearchRequest searchRequest = new SearchRequest("channelfinder");
            searchRequest.scroll(scroll);

            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(qb);
            searchSourceBuilder.size(10000);
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

            int count = 0;
            while (searchHits != null && searchHits.length > 0) {

                System.out.println("processing hits : " + count + " - " + (count + searchHits.length));
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

                count = count + searchHits.length;
            }

            ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
            clearScrollRequest.addScrollId(scrollId);
            ClearScrollResponse clearScrollResponse = searchClient.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
            boolean succeeded = clearScrollResponse.isSucceeded();

            File file = Files.createFile(Paths.get(reportFileName.toString()+".txt")).toFile();

            try (FileOutputStream fileOutputStream = new FileOutputStream(file);
                 DataOutputStream outStream = new DataOutputStream(new BufferedOutputStream(fileOutputStream));)
            {
                outStream.writeUTF("Unique Channels:" + pvs.size());
                outStream.writeUTF(System.lineSeparator());
                
                if(!pvs.isEmpty())
                {

                    outStream.writeUTF("Unique Hosts:" + propertyValues.get("hostName").keySet().size());
                    outStream.writeUTF(System.lineSeparator());
                    outStream.writeUTF(propertyValues.get("hostName").entrySet().stream().map(entry -> {
                        return entry.getKey() + ":" + entry.getValue().size();
                    }).collect(Collectors.joining(",")));

                    if (propertyValues.containsKey("iocid")) {
                        outStream.writeUTF("Unique iocid:" + propertyValues.get("iocid").size());
                        outStream.writeUTF(System.lineSeparator());
                        outStream.writeUTF(propertyValues.get("iocid").entrySet().stream().map(entry -> {
                            return entry.getKey() + ":" + entry.getValue().size();
                        }).collect(Collectors.joining(",")));
                        outStream.writeUTF(System.lineSeparator());
                    }

                    if (propertyValues.containsKey("iocName")) {
                        outStream.writeUTF("Unique iocName:" + propertyValues.get("iocName").size());
                        outStream.writeUTF(System.lineSeparator());
                        outStream.writeUTF(propertyValues.get("iocName").entrySet().stream().map(entry -> {
                            return entry.getKey() + ":" + entry.getValue().size();
                        }).collect(Collectors.joining(",")));
                        outStream.writeUTF(System.lineSeparator());
                    }

                    // check
                    final Set<String> pvNames = Collections.unmodifiableSet(pvs);
                    final List<FutureTask<String>> tasks = new ArrayList<>();

                    for (PVNamesProcessor pvNamesProcessor : processPVNamesServiceLoader) {
                        pvNamesProcessor.setPVNames(pvNames);
                        FutureTask<String> task = new FutureTask<>(pvNamesProcessor);
                        executorService.submit(task);
                        tasks.add(task);
                    }

                    tasks.stream().forEach(t -> {
                        try {
                            // outStream.writeChars(t.get());
                            String s = t.get();

                            if (s == null) {
                                outStream.writeByte(0);
                            } else {
                                outStream.writeByte(1);
                                if (s.length() < 16*1024) {
                                    outStream.writeUTF(s);
                                } else {
                                    // here comes the simple workaround
                                    byte[] b = s.getBytes("utf-8");
                                    outStream.writeInt(b.length);
                                    outStream.write(b);
                                }
                            }

                        } catch (IOException | InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                    });
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("closing the search client.");
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
