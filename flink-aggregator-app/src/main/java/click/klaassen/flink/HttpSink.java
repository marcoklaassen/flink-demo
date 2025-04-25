package click.klaassen.flink;

import java.io.IOException;

import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class HttpSink extends RichSinkFunction<AggregatedEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(HttpSink.class);

    private final String endpoint;
    private static final ObjectMapper mapper = new ObjectMapper();


    public HttpSink(String endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public void invoke(AggregatedEvent event, Context context) throws IOException {
        try {
            String json = mapper.writeValueAsString(event);
            LOG.info("Sending aggregated Request: {}", json);
        } catch (IOException e) {
            System.err.println("Failed to serialize event for HTTP sink: " + e.getMessage());
        }
    }
}