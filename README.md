## Local Build and Setup - CRITICAL TO READ !!!

This project requires both maven and sbt to build.

First you have to do a `mvn install` in the futures-extra directory.  I had to modify the futures-extra to add the ability to pass in an executor.  This allows the future to run in a fork join pool instead of with the default direct executor.

## Run Reactive Fauna

```
  cd reactive-fauna
  sbt run
```

Then you should be able to add a test user with a post command:

Save the following json to a json file:

`{"clientId":0,"counter":7,"type":"DEPOSIT","description":"NEW DEPOSIT", "amount":"42.11"}`

For example with [HTTPie](https://httpie.org/doc):

```
  http localhost:9000/add < ledger-entry.json
```

## Run Load Harness


```
  export EVENT_SERVER_HOST=localhost
  export EVENT_SERVER_PORT=9090
  export TEST_TIME=5
  export USER_LOAD=5

  sbt run
```  

## Deploy to Kubernetes

gcloud container clusters create raptor \
      --cluster-version=1.9.7 \
      --num-nodes 3 \
      --machine-type n1-standard-2

sbt docker:publishLocal

gcloud auth configure-docker
docker tag reactive-fauna:1.1 gcr.io/precise-window-210817/reactive-fauna:1.1
docker push gcr.io/precise-window-210817/reactive-fauna:1.1

kubectl create -f react-fauna.yaml
kubectl get pods
kubectl logs -f reactive-fauna-v1-7d95896c94-6k522
kubectl port-forward reactive-fauna-v1-75549546d7-ttz8n 9000
