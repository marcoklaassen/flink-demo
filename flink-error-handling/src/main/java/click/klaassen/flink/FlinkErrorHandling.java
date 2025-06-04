package click.klaassen.flink;

// Flink APIs
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Counter;
import org.apache.flink.metrics.MetricGroup;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

// For PDF generation
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

// For Output to S3 or filesystem
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.OutputStream;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.core.fs.Path;

// For error handling and logging
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Objects;

// JSON
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

// Logging
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlinkErrorHandling {

    private static final Logger LOG = LoggerFactory.getLogger(FlinkErrorHandling.class);

    private static final OutputTag<String> errorOutputTag = new OutputTag<>("error-output", Types.STRING);

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        DataStream<String> input = env.fromElements("valid1", "valid2", "INVALID", "valid3")
            .name("Simple Data List")
            .setDescription("Data Source with three valid and one invalid entries");
        
        SingleOutputStreamOperator<String> processed = input.process(new ProcessFunction<String, String>() {

            private transient Counter successCounter;
            private transient Counter errorCounter;
            
            @Override            
            public void open(Configuration parameters) throws Exception {
                MetricGroup metricGroup = getRuntimeContext().getMetricGroup();
                successCounter = metricGroup.counter("success_counter");
                errorCounter = metricGroup.counter("error_counter");
            }

            @Override
            public void processElement(String value, Context ctx, Collector<String> out) {
                try {
                    if (value.equals("INVALID")) {
                        throw new RuntimeException("Invalid Value");
                    }
                    out.collect("Processed: " + value);
                    successCounter.inc();
                    LOG.info("Row '{}' processed successfully", value);
                } catch (Exception e) {
                    errorCounter.inc();
                    ctx.output(errorOutputTag, "Invalid Row: " + value);
                    LOG.error("Processing Row '{}' failed", value);
                }
            }
        });

        // Successfully processed (optionally write to S3 bucket with PDF Generation)
        FileSink<String> sink = FileSink
        .forRowFormat(new Path("s3a://flink-error-handling/data"), new SimpleStringEncoder<String>("UTF-8"))
        .build();
        processed.sinkTo(sink).name("Successful Data Bucket");
        
        // Write Errors to S3 bucket
        DataStream<String> errorStream = processed.getSideOutput(errorOutputTag);

        FileSink<String> errorSink = FileSink
        .forRowFormat(new Path("s3a://flink-error-handling/errors"), new SimpleStringEncoder<String>("UTF-8"))
        .build();
        
        errorStream.sinkTo(errorSink).name("Error Data Bucket");

        env.execute("Error handling with Prometheus and S3");
    }
}