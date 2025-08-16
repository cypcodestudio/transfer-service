# TRANSFER MS SOLUTION
This microservice handles a payment transfer API trigger, it is the client facing Rest API exposed according to the below implementation:

* Namespace: 'com.cypcode.transfer-service'
* Spring: '3.5.4 POM'
* Java: '17'
* Database: 'In-Memory H2'
* Architecture: 'Microservices Architecture'
* Author: 'Nkululeko Gininda'

# Rest API Interfaces
* Base Url: http://localhost:8080
* POST Single Transfer: '/transfers'
* GET Transfer by ID: '/transfers/{id}'
* POST Batch Transfer: '/transfers/batch' 
* GET Service Health: '/actuator/health'


### Rest API Execution Steps
For detailed interface process, the flow of events:

* POST Single Transfer: '/transfers'
  * Client triggers an API call with the transfer payload below:
    * fromAccountId
    * toAccountId
    * amount
  * Client provides header Idempotency-Key
  * All transfer requests are handled as atomic transactions to guarantee data quality and accuracy
  * Transfer service automatically generates and attach a transferId 
  * Transfer service propagates the request to ledger microservice which fulfils the transfer request
    * ledger microservice returns the transfer status to the transfer service
    * if ledger service is unavailable, transfer service internally handles teh failure via a circuit breaker 
  * Transfer service handles the Idempotency of transfer requests to prevent duplication of transactions and ensure performance across the services
* GET Transfer by ID: '/transfers/{id}'
  * Retrieves the transfer status for the provided transferId
  * The status is retrieved as according to the idempotency implementation to prevent expensive database calls to internal tables 
* POST Batch Transfer: '/transfers/batch' 
  * Client triggers an API call with multiple (Max - 20) transfer payload below:
    * fromAccountId
    * toAccountId
    * amount
  * All transfer requests are handled as atomic transactions to guarantee data quality and accuracy
  * Transfer service automatically generates and attach a transferId
  * Transfer service propagates the request to ledger microservice which fulfils the transfer request
      * ledger microservice returns the transfer status to the transfer service
      * if ledger service is unavailable, transfer service internally handles teh failure via a circuit breaker
  * High performance mechanism using CompletableFuture concurrent execution of multiple transfer items
  * Transfer service handles the Idempotency of transfer requests to prevent duplication of transactions and ensure performance across the services



### DAO Implementation
* JPA Hibernate database interaction implementation
* All write operations are executed atomically inside a transaction
* Enabled liquibase for reliable database versioning and maintenance
* Utilising H2 In-Memory database that enables ease of service spin up locally or via a Docker Container

### Security Implementation
* Spring security with jwt authentication not a requirement for day 1 implementation
* Basic principal authentication (username and password) design
* A Token Generation API to be exposed
* Jwt token validated with a 2 Hours expiry window


### Transfer Service Packaging
* Attached Docker compose script to build the microservice deployment artifact
* Containers expose port 8080:8080 mapping to local port


### Transfer Service References
The service swagger: http://localhost:8080/api/docs