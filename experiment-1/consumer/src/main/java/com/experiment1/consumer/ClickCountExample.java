package com.experiment1.consumer;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.GenerateSequence;
import org.apache.beam.sdk.transforms.Count;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.Values;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.PCollection;
import org.joda.time.Duration;

public class ClickCountExample {

    public static void main(String[] args) {
        Pipeline pipeline = Pipeline.create();

        PCollection<Long> clickEvents = pipeline.apply(GenerateSequence.from(0).to(100))
                .apply(Window.into(FixedWindows.of(Duration.standardMinutes(1))))
                .apply(Count.perElement())
                .apply(Values.create());

        clickEvents.apply(ParDo.of(new LogClickCountFn()));

        pipeline.run();
    }

    public static class LogClickCountFn extends DoFn<Long, Void> {
        @ProcessElement
        public void processElement(ProcessContext c) {
            long clickCount = c.element();
            System.out.println("Número de cliques no minuto: " + clickCount);
        }
    }
}
