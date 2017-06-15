# APM-287 Reactive programming

## Starting

The app consists of two parts: backend containing different reactive and non-reactive service implementations and mobile frontend for using the exposed rest services.

### Backend

Backend is a Spring Boot application and can be started by running:

```apm287-stock-backend$ ./gradle bootRun```

The properties of the application is defined in the ```application.properties``` and there it's possible to:

* select type of service implementation (akka, java-sync, java-concurrent, etc)
* define properties of the ```ApprovalService``` that can serve as bottle-neck in the app

Available REST points are:

* http://localhost:8080/balance
* http://localhost:8080/stocks
* http://localhost:8080/buy?c=code&s=size
* http://localhost:8080/sell?c=code&s=size

When started application can be remotely debugged on port 9080.

### Mobile

For mobile / web front end to  start you need to install NodeJS on your machine. Then use ```npm``` command to install the following:

```
npm install ionic cordova -g
npm install chart.js --save
```

Then the app can be served with:

```apm287-stock-mobile$ ionic serve```

and accessible on http://localhost:8100.

## Implement new service

If you wish to implement a new stock service and try out new library please implement the IStockService interface. Interface methods return Java ```Object``` as result so you can return anything you wish if this is allowed as result by Spring Rest. Thus, you can return DTO objects, but also ```Future```, ```CompletableFuture```, etc.

The general requirements are:
* There are at least 5 stocks
* Stocks are updated every second (by an external thread, executor, message)
* Stocks can be bought and sold with the current price
* Starting balance is 10,000 and can be used for buying stocks
* When buying more than 5 stocks, ```ApprovalService``` must be called to authorize the transaction
* You can only buy stocks if enough balance to pay them
* You can only sell stocks if you own them

To better understand the general requirements check ```de.novatec.apm287.java.sync.StockManager``` which implements requirements in plan Java using synchronized methods.

Once new service is implemented, please add the option to use in the ```de.novatec.apm287.Apm287Application``` and change the ```application.properties``` to use the new implementation.

## Load testing

Gatling load test is located under ```gatling/``` folder. It can be used to generate considerable load on the backend. Please note that used service implementation and ```ApprovalService``` settings heavily affect the load test results.


