# Simple Flink Demo

This is a simple Apache Flink Demo. It includes a strimzi KafkaBridge to produce kafka messages on a topic via HTTP. 
The Flink component aggregates the events on the topic and sends the result to an API endpoint. 


## Prerequisites

* Up and Running OpenShift Cluster
* Streams for Apache Kafka operator installed
* Flink Kubernetes Operator installed
* OpenJDK 17 installed and configured
* mvn installed and configured

## Installation

* create a new project `flink` with `oc new-project flink-demo`
* apply the manifests for minio (to provide the jar file), strimzi (kafka infrastructure) and the tekton pipeline to build the java application and the flink-container-image: 

```
oc apply -f minio
oc apply -f strimzi
oc apply -f tekton
```

* build the flink aggregator app locally (optional)
```
cd flink-aggregator-app
mvn clean package
```

* Login to minio web-ui: https://minio-flink-demo.apps.ocp4.klaassen.click/ with default credentials
* create two buckets `flink-data-checkpoints` & `flink-data-savepoints` via the minio web-ui

* apply the manifests for the flink deployment and it's infrastructure
```
oc apply -f flink
```

## Add new events to the topic

* execute the curl command to `POST` new events to the topic

```
curl -i -X POST https://kafka-bridge-flink-demo.apps.ocp4.klaassen.click/topics/flink \
  -H "Content-Type: application/vnd.kafka.json.v2+json" \
  -d '{
        "records": [
          { "value": { "user": "Marco", "counter": "2" } }
        ]
      }'
```

* have a look to the `flink-aggregator-taskmanager` pod's logs and you'll see the aggregation of the events

```
oc logs -f $(oc get pods -l app=flink-aggregator -l component=taskmanager -o name)
```

Example output: 

```
<date> INFO  click.klaassen.flink.HttpSink [] - Sending aggregated Request: {"user":"Marco","total":8}
```

## Build container image
```
oc create -f tekton/pipeline-run-error-handling.yaml
oc create -f tekton/pipeline-run-aggregator.yaml
```

## Monitoring

Access the Flink Dashboard (`https://flink-aggregator-flink-demo.apps.ocp4.klaassen.click`) to get an overview. 
![Apache Flink Dashboard](img/flink-dashboard.png)

## Links

* https://nightlies.apache.org/flink/flink-docs-master/
* https://nightlies.apache.org/flink/flink-docs-master/docs/deployment/resource-providers/native_kubernetes/
* https://min.io/docs/minio/kubernetes/upstream/index.html
* https://docs.redhat.com/en/documentation/openshift_container_platform/4.18