package click.klaassen.flink;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaFlinkAggregator {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaFlinkAggregator.class);

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        
        String kafkaBootstrap = System.getenv("KAFKA_BOOTSTRAP");
        String topic = System.getenv("TOPIC");
        String groupId = System.getenv("GROUP_ID");
        
        String securityProtocol = System.getenv("SECURITY_PROTOCOL");
        String storeType = System.getenv("STORE_TYPE");

        String trustStoreLocation = System.getenv("TRUST_STORE_LOCATION");
        String trustStorePassword = System.getenv("TRUST_STRORE_PASSWORD");

        String keyStoreLocation = System.getenv("KEY_STORE_LOCATION");
        String keyStorePassword = System.getenv("KEY_STORE_PASSWORD");

        String sinkEndpoint = System.getenv("SINK_ENDPOINT");

        LOG.debug("Envirionment variables initialized.");

        KafkaSource<String> kafkaSource = KafkaSource.<String>builder()
        .setBootstrapServers(kafkaBootstrap)
        .setTopics(topic)
        .setGroupId(groupId)
        .setStartingOffsets(OffsetsInitializer.latest())
        .setValueOnlyDeserializer(new SimpleStringSchema())
        .setProperty("security.protocol", securityProtocol)
        
        .setProperty("ssl.truststore.type", storeType)
        .setProperty("ssl.truststore.location", trustStoreLocation)
        .setProperty("ssl.truststore.password", trustStorePassword)

        .setProperty("ssl.keystore.type", storeType)
        .setProperty("ssl.keystore.location", keyStoreLocation)
        .setProperty("ssl.keystore.password", keyStorePassword)
        .build();        

        LOG.debug("Finished KafkaSource build.");

        DataStream<String> rawStream = env.fromSource(
                kafkaSource,
                WatermarkStrategy.noWatermarks(),
                "Kafka Source"
        );

        DataStream<MyEvent> validEvents = rawStream                
                .map(new SafeJsonDeserializer())
                .filter(e -> e != null);

        DataStream<MyEvent> reduced = validEvents                
                .keyBy(MyEvent::getUser)
                .reduce((e1, e2) -> new MyEvent(e1.getUser(), e1.getCounter() + e2.getCounter()));

        
        DataStream<AggregatedEvent> aggregated = reduced
            .map(e -> new AggregatedEvent(e.getUser(), e.getCounter()));

        aggregated.addSink(new HttpSink(sinkEndpoint));

        env.execute("Kafka Aggregator with Safe Deserialization");
    } 
}