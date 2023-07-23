package com.experiment1.consumer;

import java.util.Random;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.GenerateSequence;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.windowing.BoundedWindow;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.TimestampCombiner;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.joda.time.Duration;

public class WatermarkLagExample {

  public static void main(String[] args) {
    Pipeline pipeline = Pipeline.create();

    PCollection<KV<Long, String>> events = pipeline.apply(GenerateSequence.from(0).to(10))
        .apply(ParDo.of(new GenerateEventsFn()));

    PCollection<KV<Long, String>> windowedEvents = events.apply(
        Window.<KV<Long, String>>into(FixedWindows.of(Duration.standardSeconds(2)))
            .withTimestampCombiner(TimestampCombiner.END_OF_WINDOW));

    windowedEvents.apply(ParDo.of(new MonitorWatermarkLagFn()));

    pipeline.run();
  }

  public static class GenerateEventsFn extends DoFn<Long, KV<Long, String>> {
    @ProcessElement
    public void processElement(ProcessContext c) {
      long eventTime = c.element();
      Random random = new Random();
      long delay = random.nextInt(3000);
      eventTime += delay;
      c.output(KV.of(eventTime, "Evento_" + c.element()));
    }
  }

  public static class MonitorWatermarkLagFn extends DoFn<KV<Long, String>, Void> {
    @ProcessElement
    public void processElement(ProcessContext c, BoundedWindow window) {
      long currentTimestamp = c.timestamp().getMillis();
      long watermarkTimestamp = window.maxTimestamp().getMillis();

      long watermarkLag = currentTimestamp - watermarkTimestamp;
      System.out.println("Watermark Lag: " + watermarkLag + "ms for Event: " + c.element().getValue());
    }
  }
}
