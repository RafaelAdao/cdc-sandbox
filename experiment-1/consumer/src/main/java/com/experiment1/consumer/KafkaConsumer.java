package com.experiment1.consumer;

import java.util.HashMap;
import java.util.Map;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.elasticsearch.ElasticsearchIO;
import org.apache.beam.sdk.io.kafka.KafkaIO;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.vendor.grpc.v1p48p1.com.google.gson.Gson;
import org.apache.beam.vendor.grpc.v1p48p1.com.google.gson.JsonObject;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import avro.shaded.com.google.common.collect.ImmutableMap;

public class KafkaConsumer {
  private static Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);

  public static void buildPipeline(Pipeline pipeline) {

    var consumer = pipeline
        .apply(KafkaIO.<String, String>read()
            .withBootstrapServers("localhost:9092")
            .withTopic("xp-1.public.my_table")
            .withKeyDeserializer(StringDeserializer.class)
            .withValueDeserializer(StringDeserializer.class)
            .withConsumerConfigUpdates(ImmutableMap.of(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"))
            .withoutMetadata());
    var parsed = consumer.apply(ParDo.of(parseMyTable()));
    var doc = parsed.apply(ParDo.of(new DoFn<Map<String, String>, String>() {
      @ProcessElement
      public void processElement(ProcessContext c) {
        var element = c.element();
        var change = new Gson().fromJson(element.get("change"), JsonObject.class);
        var output = new JsonObject();

        output.add("id", change.get("id"));
        output.add("description", change.get("description"));
        output.add("value", change.get("value"));
        output.addProperty("timestamp",
            new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .format(new java.util.Date(change.get("timestamp").getAsLong() / 1000)));
        output.addProperty("derived", change.get("value").getAsDouble() * 2);

        c.output(output.toString());
      }
    }));
    var output = doc.apply(
        ElasticsearchIO.write()
            .withConnectionConfiguration(
                ElasticsearchIO.ConnectionConfiguration.create(
                    new String[] { "http://localhost:9210" }, "xp-1"))
            .withUsePartialUpdate(true)
            .withIdFn(new ElasticsearchIO.Write.FieldValueExtractFn() {
              @Override
              public String apply(JsonNode input) {
                return input.get("id").asText();
              }
            }));
    output
        .get(ElasticsearchIO.Write.SUCCESSFUL_WRITES)
        .apply(ParDo.of(new DoFn<ElasticsearchIO.Document, Void>() {
          @ProcessElement
          public void processElement(ProcessContext c) {
            var element = c.element();
            System.out.printf("Successfully wrote document: %s\n", element);
          }
        }));
  }

  private static DoFn<KV<String, String>, Map<String, String>> parseMyTable() {
    return new DoFn<KV<String, String>, Map<String, String>>() {
      @ProcessElement
      public void processElement(ProcessContext c) {
        KV<String, String> record = c.element();
        JsonObject value = new Gson().fromJson(record.getValue(), JsonObject.class);
        var payload = value.getAsJsonObject("payload");
        String table = payload.getAsJsonObject("source").get("table").getAsString();
        String operation = new HashMap<String, String>() {
          {
            put("c", "create");
            put("u", "update");
            put("d", "delete");
            put("r", "read (applies to only snapshots)");
            put("t", "truncate");
            put("m", "message");
          }
        }.get(payload.get("op").getAsString());
        var change = payload.getAsJsonObject("after");

        var output = new HashMap<String, String>();
        output.put("operation", operation);
        output.put("table", table);
        output.put("change", change.toString());

        c.output(output);
      }
    };
  }

  public static void main(String[] args) {
    var pipeline = Pipeline.create();
    KafkaConsumer.buildPipeline(pipeline);
    pipeline.run().waitUntilFinish();
  }
}
