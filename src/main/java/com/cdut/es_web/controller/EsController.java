package com.cdut.es_web.controller;

import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by Horizon
 * Time: 上午9:19 2018/3/10
 * Description:
 */
@RestController
public class EsController {

    @Autowired
    private TransportClient client;

    @GetMapping("/get/book/novel")
    public ResponseEntity get(
            @RequestParam(name = "id", required = false) String id) {
        if (id == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        GetResponse result = client.prepareGet("book", "novel", id).get();
        if (!result.isExists()) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity(result.getSource(), HttpStatus.OK);
    }

    @PostMapping("/add/book/novel")
    public ResponseEntity add(
            @RequestParam(name = "title") String title,
            @RequestParam(name = "author") String author,
            @RequestParam(name = "word_count") Integer wordCount,
            @RequestParam(name = "publish_date")
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                    Date publishDate
    ) {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder().startObject()
                    .field("title", title)
                    .field("author", author)
                    .field("word_count", wordCount)
                    .field("publish_date", publishDate.getTime()).endObject();
            IndexResponse indexResponse = client.prepareIndex("book", "novel").setSource(builder).get();
            return new ResponseEntity(indexResponse.getId(), HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/delete/book/novel")
    public ResponseEntity delete(@RequestParam(name = "id", defaultValue = "") String id) {
        if (id.isEmpty()) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        DeleteResponse result = client.prepareDelete("book", "novel", id).get();
        return new ResponseEntity(result.getResult().toString()
                , HttpStatus.OK);
    }

    @PutMapping("/update/book/novel")
    public ResponseEntity update(
            @RequestParam(name = "id") String id,
            @RequestParam(name = "title", required = false) String title,
            @RequestParam(name = "author", required = false) String author
    ) {
        UpdateRequest update = new UpdateRequest("book", "novel", id);
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder().startObject();
            if (title != null) {
                builder.field("title", title);
            }
            if (author != null) {
                builder.field("author", author);
            }
            builder.endObject();
            update.doc(builder);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        try {
            UpdateResponse result = client.update(update).get();
            return new ResponseEntity(result.getResult().toString(), HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/query/book/novel")
    public ResponseEntity query(
            @RequestParam(name = "author", required = false) String author,
            @RequestParam(name = "title", required = false) String title,
            @RequestParam(name = "gt_word_count", defaultValue = "0") int gtWordCount,
            @RequestParam(name = "lt_word_count", required = false) Integer ltWordCount
    ) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        if (author != null) {
            boolQuery.must(QueryBuilders.matchQuery("author", author));
        }
        if (title != null) {
            boolQuery.must(QueryBuilders.matchQuery("title", title));
        }
        RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("word_count").from(gtWordCount);
        if (ltWordCount != null && ltWordCount > 0) {
            rangeQuery.to(ltWordCount);
        }
        boolQuery.filter(rangeQuery);
        SearchRequestBuilder builder = client.prepareSearch("book")
                .setTypes("novel")
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(boolQuery)
                .setFrom(0)
                .setSize(10);
        System.out.println(builder);

        SearchResponse searchResponse = builder.get();
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (SearchHit hit : searchResponse.getHits()) {
            list.add(hit.getSource());
        }
        return new ResponseEntity(list, HttpStatus.OK);
    }

}
