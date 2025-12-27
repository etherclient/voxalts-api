# voxalts-api

> A library for interacting with the [VoxAlts](https://voxalts.store/) API.

## Example

```java
CommunicationHandler communicationHandler = new CommunicationHandler(YOUR_API_KEY);
CommunicationHandler.BalanceResponse balanceResponse = communicationHandler.getBalance();
System.out.println(balanceResponse);
```

## Features

- Retrieve account balance
- Fetch available plans
- Create new orders
- Extract mctokens/access tokens (and other data) from session accounts
