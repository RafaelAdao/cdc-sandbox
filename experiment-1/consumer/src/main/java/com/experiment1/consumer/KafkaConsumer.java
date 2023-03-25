package com.experiment1.consumer;

import java.util.HashMap;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.kafka.KafkaIO;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.vendor.grpc.v1p48p1.com.google.gson.Gson;
import org.apache.beam.vendor.grpc.v1p48p1.com.google.gson.JsonObject;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;

import avro.shaded.com.google.common.collect.ImmutableMap;

public class KafkaConsumer {

  public static void buildPipeline(Pipeline pipeline) {
    pipeline
        .apply(KafkaIO.<String, String>read()
            .withBootstrapServers("localhost:9092")
            .withTopic("xp-1.public.my_table")
            .withKeyDeserializer(StringDeserializer.class)
            .withValueDeserializer(StringDeserializer.class)
            .withConsumerConfigUpdates(ImmutableMap.of(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"))
            .withoutMetadata())
        .apply(ParDo.of(new DoFn<KV<String, String>, Void>() {
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
            System.out.printf("Table: %s, Op: %s, Change: %s\n", table, operation, change);
          }
        }));
  }

  public static void main(String[] args) {
    var pipeline = Pipeline.create();
    KafkaConsumer.buildPipeline(pipeline);
    pipeline.run().waitUntilFinish();
  }
}
