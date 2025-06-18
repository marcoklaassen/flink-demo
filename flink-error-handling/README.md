# Flink Error Handling Example

This is a simple Flink application which shows how to handle errors. 
It uses static sample data with valid and invalid entries. 

The application shows that you can 
1. redirect invalid data to a separate sink - so you're able to identify the invalid data in an S3 storage
1. use a custom metrics to count errors - so your're able to analyse them in a prometheus instance or a grafana dashboard

This application also shows how to setup your local development machine to test the application. 

## Local Development & Setup

Install Flink CLI: 
```
curl https://dlcdn.apache.org/flink/flink-1.20.1/flink-1.20.1-bin-scala_2.12.tgz \
    -o flink/bin/flink-1.20.1-bin-scala_2.12.tgz
tar -xzf flink/bin/flink-1.20.1-bin-scala_2.12.tgz -C flink/bin
rm flink/bin/flink-1.20.1-bin-scala_2.12.tgz
```

Install s3 plugin: 
```
mkdir flink/bin/flink-1.20.1/plugins/flink-s3-fs-hadoop
curl https://repo1.maven.org/maven2/org/apache/flink/flink-s3-fs-hadoop/1.20.1/flink-s3-fs-hadoop-1.20.1.jar \
    -o flink/bin/flink-1.20.1/plugins/flink-s3-fs-hadoop/flink-s3-fs-hadoop-1.20.1.jar
```

Start minio locally:
```
podman run -d --name minio \
  -p 9000:9000 -p 9001:9001 \
  -e "MINIO_ACCESS_KEY=minioadmin" \
  -e "MINIO_SECRET_KEY=minioadmin" \
  quay.io/minio/minio server /data --console-address ":9001"
```

Install & Configure Minio CLI: 
```
brew install minio/stable/mc
mc alias set local http://127.0.0.1:9000 minioadmin minioadmin
```

Create bucket in minio:
```
mc mb local/flink-error-handling
```

Start flink local cluster: 
```
export FLINK_CONF_DIR=flink/conf
export FLINK_PLUGINS_DIR=flink/bin/flink-1.20.1/plugins
flink/bin/flink-1.20.1/bin/start-cluster.sh
```

Start flink job:
```
flink/bin/flink-1.20.1/bin/flink run -c click.klaassen.flink.FlinkErrorHandling target/flink-error-handling-1.0.jar
```

Have a look at the buckets with the CLI or the UI: 
```
mc ls --recursive local/flink-error-handling
# http://localhost:9001/browser/flink-error-handling


# Get the latest file's content: 
mc cat local/flink-error-handling/data/$(mc ls --recursive --json local/flink-error-handling/data | jq -r '. | select(.type=="file") | [.time, .key] | @tsv' | sort | tail -n 1 | cut -f2)
mc cat local/flink-error-handling/$(mc ls --recursive --json local/flink-error-handling/error | jq -r '. | select(.type=="file") | [.time, .key] | @tsv' | sort | tail -n 1 | cut -f2)
```

Stop cluster:
```
flink/bin/flink-1.20.1/bin/stop-cluster.sh
```