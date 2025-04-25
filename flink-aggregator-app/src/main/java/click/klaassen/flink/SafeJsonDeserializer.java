package click.klaassen.flink;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;

public class SafeJsonDeserializer implements MapFunction<String, MyEvent> {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public MyEvent map(String value) {
        try {
            return mapper.readValue(value, MyEvent.class);
        } catch (Exception e) {
            // Log and skip
            System.err.println("Skipping invalid event: " + e.getMessage());
            return null;
        }
    }
}