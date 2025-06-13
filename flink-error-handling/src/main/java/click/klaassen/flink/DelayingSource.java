package click.klaassen.flink;

import org.apache.flink.streaming.api.functions.source.RichSourceFunction;

public class DelayingSource extends RichSourceFunction<String> {
    private volatile boolean running = true;

    @Override
    public void run(SourceContext<String> ctx) throws Exception {
        long start = System.currentTimeMillis();
        long delay = 60_000L; // wait 60 seconds
        while (running && System.currentTimeMillis() - start < delay) {
            Thread.sleep(1000);
        }
    }

    @Override
    public void cancel() {
        running = false;
    }
}
