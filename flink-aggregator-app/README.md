# Flink Aggregator App

This is a flink app which consumes messages from a kafka topic. 
After aggregating the messages it provides just log entries. 

It's part of [this demo](../README.md)


## Build the flink aggregator app locally (optional)

```
cd flink-aggregator-app
mvn clean package
```

## Trigger Build process on OpenShift

This triggers a tekton pipeline which was already created by [this part of the demo](../README.md). The pipeline builds the java application via maven and put it in a container image. Finally it pushed the container image to the local OpenShift container repository. 

```
oc create -f tekton/run/run-aggregator-image-build.yaml
```