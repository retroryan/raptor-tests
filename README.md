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
