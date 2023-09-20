package com.experiment1.consumer;

import java.util.Collections;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.elasticsearch.ElasticsearchIO;
import org.apache.beam.sdk.io.elasticsearch.ElasticsearchIO.ConnectionConfiguration;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.beam.sdk.values.PCollection;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.apache.beam.vendor.grpc.v1p48p1.com.google.gson.Gson;
import org.apache.beam.vendor.grpc.v1p48p1.com.google.gson.JsonObject;

public class ReadFromElasticsearch {
  private static Logger logger = LoggerFactory.getLogger(ReadFromElasticsearch.class);

  public static void buildPipeline(Pipeline pipeline) {
    ConnectionConfiguration connectionConfiguration = ElasticsearchIO.ConnectionConfiguration
        .create(new String[] { "http://localhost:9210" }, "xp-1");

    PCollection<String> elasticsearchData = pipeline.apply(
        ElasticsearchIO.read()
            .withConnectionConfiguration(connectionConfiguration)
            .withBatchSize(100)
            .withQuery(elasticsearchQuery()));

    // Apply transformations to access and process the data
    PCollection<String> processedData = elasticsearchData.apply(
        "Process Data", ParDo.of(new ProcessDataFn()));

    // Print the results
    processedData.apply(
        "Print Results", ParDo.of(new PrintResultsFn()));
  }

  // Define a DoFn to process the data
  static class ProcessDataFn extends DoFn<String, String> {
    @ProcessElement
    public void processElement(ProcessContext context) {
      String document = context.element();
      // String documentId = document.getString("document_id");
      context.output(document);
    }
  }

  // Define a DoFn to print the results
  static class PrintResultsFn extends DoFn<String, Void> {
    @ProcessElement
    public void processElement(ProcessContext context) {
      String result = context.element();
      logger.info("Result: " + result);
    }
  }

  private static String elasticsearchQuery() {
    // TermsQueryBuilder termsQuery = QueryBuilders.termsQuery("description", "product");
    // BoolQueryBuilder boolQuery = QueryBuilders.boolQuery().must(termsQuery);
    // String scriptSource = "def size = 0; for (entry in params._source.entrySet()) { if (entry.value != null) size += entry.value.toString().length(); } return size;";
    // Script script = new Script(ScriptType.INLINE, "painless", scriptSource, Collections.emptyMap());
    // SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
    // sourceBuilder.scriptField("document_size", script);
    // sourceBuilder.query(boolQuery);
    // sourceBuilder.fetchSource(false);
    // return sourceBuilder.toString();

    // return "{\"query\":{\"bool\":{\"must\":[{\"terms\":{\"description\":[\"product\"]}}]}},\"_source\":false,\"script_fields\":{\"document_size\":{\"script\":{\"lang\":\"painless\",\"source\":\"def size = 0; for (entry in params._source.entrySet()) { if (entry.value != null) size += entry.value.toString().length(); } return size;\"}}}}";
    return "{\"query\":{\"bool\":{\"must\":[{\"terms\":{\"description\":[\"product\"]}}]}}}";
  }

  public static void main(String[] args) {
    var pipeline = Pipeline.create();
    ReadFromElasticsearch.buildPipeline(pipeline);
    pipeline.run().waitUntilFinish();
  }
}
