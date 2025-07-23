# ARCHITECTURE.md

## Overview

This repository implements a suite of database consistency and transaction performance tests, with a focus on load testing for event server endpoints. The core load test logic is implemented in Scala using Gatling, a powerful open-source load testing framework. Supporting utilities and tests are written in Java and Scala.

## Load Test Architecture

### Technologies Used

- **Scala**: Main language for the load harness and simulation scripting (version 2.12.6).
- **Gatling**: Used for defining and running HTTP-based load scenarios and collecting performance metrics (version 2.3.0).
- **Java**: Utility libraries and tests, especially around asynchronous programming (Guava Futures, custom FuturesExtra).
- **SBT (Scala Build Tool)**: For building and running tests.
- **Play Framework**: Powers the reactive-fauna event server application.
- **FaunaDB**: Database backend for the event server (using faunadb-java 2.2.0).
- **Circe**: JSON parsing library for Scala (version 0.9.0-M1).
- **Maven**: Build tool for the futures-extra Java utilities.
- **Docker & Kubernetes**: For containerizing and deploying the tested service and harnesses.
- **Google Cloud**: Example scripts for Kubernetes deployment are provided.

### EventSourceLoad Simulation

- **Class**: `EventSourceLoad` in `loadharness/src/main/scala/fauna/EventSourceLoad.scala`
- **Purpose**: Simulates load against an event server, configurable via environment variables.
- **Configurable Parameters**:
  - `EVENT_SERVER_HOST`: Target hostname.
  - `EVENT_SERVER_PORT`: Target port.
  - `TEST_TIME`: Duration of the test (in minutes).
  - `USER_LOAD`: Number of concurrent simulated users.

Example usage:
```
export EVENT_SERVER_HOST=localhost
export EVENT_SERVER_PORT=9090
export TEST_TIME=5
export USER_LOAD=5
sbt run
```

- **Ramp-Up**: Users ramp up over a configurable period (`TIME_FOR_RAMP` = 60 seconds).
- **Load Pattern**: Uses `rampUsersPerSec(1).to(USER_LOAD)` followed by `constantUsersPerSec(USER_LOAD)` for sustained load.
- **Test Scenarios**: 
  - Executes POST requests to `/add` endpoint with JSON payloads containing ledger entries
  - Validates response parsing and event application success
  - Performs GET requests to `/all/${memberId}` to verify version consistency
  - Uses repeat loops (10 iterations) with exponential accrual calculations
- **Data Generation**: Utilizes `LoadGenerator.rangeGenerator` for creating realistic test data with client profiles.
- **Response Validation**: Implements custom response parsing using Circe JSON library to validate WriteResult responses.

### Load Test Data Models & Scenarios

#### Data Models
- **LedgerEntry**: JSON structure with fields: `clientId`, `counter`, `type`, `description`, `amount`
- **WriteResult**: Response model with Success/Failure variants containing version information
- **LedgerEvent**: Event model tracking `memberId`, `transactionId`, `version`, and `event` data

#### Test Scenarios
1. **Accumulate and Sum Scenario**: 
   - Creates client profiles with unique IDs
   - Executes 10 iterations per user with exponential accrual calculations
   - Alternates between "clear" and "accumulate" operations
   - Validates version consistency across operations
2. **Load Generators**:
   - `LoadGenerator.rangeGenerator`: Creates infinite stream of client profiles
   - `LoadProfile.clientsGenerator`: Provides client-specific configuration
   - Dynamic data generation with randomized parameters

### Supporting Infrastructure & Workflow

#### Event Server (reactive-fauna)
- **Framework**: Play Framework Java application serving as the target for load testing.
- **Endpoints**: 
  - `POST /add`: Accepts ledger entries in JSON format
  - `GET /all/{memberId}`: Retrieves all events for a specific member
- **Database**: Integrates with FaunaDB for persistent event storage.
- **Configuration**: Uses environment variables for FaunaDB connection and application secrets.

#### Build & Deployment Pipeline
- **Local Development**: 
  1. Build futures-extra: `mvn install` in the futures-extra directory
  2. Run reactive-fauna: `sbt run` in reactive-fauna directory (starts on port 9000)
  3. Execute load tests: `sbt run` in loadharness directory with environment variables set
- **Containerization**: Docker support with `sbt docker:publishLocal` for reactive-fauna
- **Kubernetes Deployment**: 
  - Manifest file: `reactive-fauna/kubernetes/react-fauna.yaml`
  - Google Cloud Container Registry integration
  - Configurable replicas and resource allocation
- **Monitoring**: Gatling produces detailed HTML reports on throughput, response time, and errors.
- **Integration**: The harness can be used locally or in CI/CD environments.

### Asynchronous Programming & Utility Tests

- **futures-extra Library**: A comprehensive Java library extending Guava's `ListenableFuture` with additional utilities:
  - **FuturesExtra**: Core utilities for future composition, transformation, and error handling
  - **AsyncRetrier**: Configurable retry mechanisms for async operations
  - **ConcurrencyLimiter**: Controls concurrent execution of operations
  - **TimeoutFuture**: Adds timeout capabilities to futures
  - **JoinedResults**: Utilities for combining multiple future results
- **Key Features**:
  - Custom executor support for fork-join pools instead of direct executors
  - Comprehensive error handling and propagation
  - Dataflow graph execution capabilities
  - Delayed execution and scheduling
- **Test Coverage**: Extensive unit test suite covering:
  - Synchronous and asynchronous transformations
  - DAG (Directed Acyclic Graph) execution patterns
  - Exception handling and propagation
  - Timeout and retry scenarios
  - Concurrency limiting and control flow

## Summary

This system provides a flexible, cloud-deployable load harness for event-driven database systems, built on Gatling and Scala, validated with strong Java-based asynchronous utilities. The architecture enables:

- **Scalable Load Testing**: Configurable user loads with realistic ramp-up patterns
- **Event Server Validation**: Comprehensive testing of ledger entry processing and retrieval
- **Robust Async Operations**: Battle-tested utilities for concurrent programming patterns
- **Cloud-Ready Deployment**: Kubernetes manifests and Docker containerization for production use
- **Comprehensive Monitoring**: Detailed performance metrics and response validation

The system can simulate configurable loads, produce actionable metrics, and is ready for deployment in containerized environments, making it suitable for both development testing and production performance validation.