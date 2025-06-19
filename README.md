# Flink Demo

This is an Apache Flink Demo which shows how to configure and operate Apache Flink on an OpenShift cluster and [how to handle data streaming](flink-aggregator-app/README.md) and [batch processing with error handling and observability](flink-error-handling/README.md). 

It covers use cases like: 
1. continues streaming of data from a kafka topic
1. sink based error handling 
1. metrics based error handling
1. local development with flink applications
1. setup and configure an OpenShift namespace
1. setup OpenShift user workload observability and let flink apps produce metrics
1. setup a grafana instance and implement a grafana dashboard for error handling of flink apps
1. setup a Minio instance to store the checkpoints, savepoints and the data which was processed by the flink app
1. setup a nexus instance to manage the flink apps 
1. configure tekton piplines to implement a fully automated CI/CD pipeline for the entire flink application and infrastructure
1. setup and configure a strimzi kafka instance and a kafka bridge to produce events to the kafka topic via HTTP requests 


## Table of content

- [Demo Architecture](#demo-architecture)
- [Prerequisites](#prerequisites)
- [Installation and Configuration](#installation-and-configuration)
   - [Installation of supporting components on OpenShift](#installation-of-supporting-components-on-openshift)
   - [Configure Minio](#configure-minio)
   - [Install Flink components on OpenShift](#install-flink-components-on-openshift)
   - [Configure Nexus server](#configure-nexus-server)
   - [Build and Deploy Flink applications](#build-and-deploy-flink-applications)
- [Play around with the demo](#play-around-with-the-demo)
   - [Flink Kafka Consumer Demo ](#flink-kafka-consumer-demo)
   - [Flink Error Handling Demo](#flink-error-handling-demo)
- [Links & Resources](#links--resources)

## Demo Architecture

![Demo Architecture](img/demo-architecture.png)


## Prerequisites

* Up and Running OpenShift Cluster
* Streams for Apache Kafka operator installed
* Flink Kubernetes Operator installed
* OpenJDK 17 installed and configured
* mvn installed and configured
* Tekton CLI installed (`brew install tektoncd-cli`)
* Nexus Repository Operator operator installed
* Grafana Operator installed 

## Installation and Configuration

### Installation of supporting components on OpenShift

* create a new project `flink` with `oc new-project flink-demo`
* apply the manifests for 
  * minio (to manage s3 buckets for success / error data sinks and the flink state)
  * strimzi for kafka instance and the kafka bridge to be able to produce new messages via http
  * tekton pipeline to build the java application and the custom flink base image
  * nexus server to manage the jar artifact
  * grafana to setup the grafana instance, the prometheus datasource and the dashboard

```
oc apply -f minio
oc apply -f strimzi
oc apply -f tekton/pipeline
oc apply -f nexus
oc apply -f grafana
```

> For the grafana deployment you have to replace the `datasource.yaml.example`. There is a `token` available in the created `grafana-serviceaccount-token` secret. For this demo there is no solution to inject the secret in a more secret way. Keep in mind that this is not a good solution when it comes to production deployments. 

### Configure Minio

#### UI based
There are some buckets which are expected by the flink deployments. 
To create this buckets:

* Login to minio web-ui: `https://minio-flink-demo.apps.<your-host>/` with the default credentials
* create three buckets `flink-data-checkpoints`, `flink-data-savepoints` & `flink-error-handling` via the minio web-ui


#### CLI based
If you are more familiar with the minio cli tool, you can of course use this one. 

Install & Configure Minio CLI: 

```
brew install minio/stable/mc
mc alias set local https://minio-flink-demo.apps.<your-host>/ <user> <password>
```

Create bucket in minio:

```
mc mb local/flink-error-handling
```

### Install Flink components on OpenShift

Apply the manifests for the flink deployment and it's infrastructure

```
oc apply -f flink/infrastructure
oc apply -f flink/custom-resources
oc apply -f flink/metrics
```

In the infrastructure directory there is everything included you need to start a flink deployment and access the flink dashboard. 

In the custom resources directory there are the the flink deployment manifests which deploy two flink job managers to your cluster. 

The metrics part is responsible to configure user workload monitoring on OpenShift and the service monitor to get the metrics. 

### Configure Nexus server


* create `flink` repository in nexus instance (`https://nexus-flink-demo.apps.<your-host>/`)
  * create new repository from type maven (hosted)
  * name should be `flink`
  * version policy: `release`
  * deployment policy: `Allow redeploy`
* configure anonymous access to flink repo in nexus to avoid more complexity in the pipeline for this demo. In production environments it's highly recommended to implement proper security concepts.
  * in this demo use case I simply add `nx-admin` role to user `anonymous` (not recommended for production environments)


### Build and Deploy Flink applications

In this demo there are two Flink applications: 

* [Flink Error Handling](flink-error-handling/README.md)
* [Flink Aggregator App](flink-aggregator-app/README.md)

To use this demo it's mandatory to build and deploy this applications. 
You can use the already deployed tekton pipelines to build and deploy both applications. 

More information on how to build and deploy the applications can be found in the READMEs of the applications. 

## Play around with the demo

### Flink Kafka Consumer Demo 

In this part of the demo a new message will be produced on a kafka topic. The flink aggregator app consumes the message and aggregates the `total` property of the message.

* execute the curl command to `POST` new events to the topic

```
curl -i -X POST https://kafka-bridge-flink-demo.apps.<your-host>/topics/flink \
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

Access the Flink Dashboard (`https://flink-aggregator-flink-demo.apps.<your-host>`) to get an overview. 
![Apache Flink Dashboard](img/flink-dashboard.png)

### Flink Error Handling Demo

This demo handles errors in data processing and represents a cron job. 
 
 TODO: Error Handling Demo instructions

## Links & Resources

* https://nightlies.apache.org/flink/flink-docs-master/
* https://nightlies.apache.org/flink/flink-docs-master/docs/deployment/resource-providers/native_kubernetes/
* https://min.io/docs/minio/kubernetes/upstream/index.html
* https://docs.redhat.com/en/documentation/openshift_container_platform/4.18
* https://developers.redhat.com/articles/2023/08/08/how-monitor-workloads-using-openshift-monitoring-stack#how_to_monitor_a_sample_application