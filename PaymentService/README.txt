***** Usage *****
Main: PaymentServiceMain

Usage: [Port]
Default port number: 8080

Data is shared among server instances.
Deadlock is avoided by locking accounts in a particular order.


***** Endpoints *****
** GET: http://127.0.0.1:8080/account/:accountId
Test Accounts:
0
1000
2000
3000
...
10000

** POST: http://127.0.0.1:8080/transfer
{
  "fromAccountId" : "2000",
  "toAccountId" : "0",
  "currency": "USD",
  "quantity": "100"
}


***** Tests *****
Integration Test: PaymentServiceIntegrationTest
Concurrency Unit Test: HazelcastPaymentServiceTest