# ptalts-api

> A library for interacting with the [PTAlts](https://ptalts.best//) API.

## Installation

### Gradle

```kotlin
repositories {
    maven {
        name = "darraghsRepositoryReleases"
        url = uri("https://repo.darragh.website/releases")
    }
}

dependencies {
    implementation("me.darragh:ptalts-api:{version}")
}
```

_This project is also available via. Jitpack. View more information [here](https://jitpack.io/#etherclient/ptalts-api)._

## Features

- Retrieve account balance
- Fetch available plans
- Create new orders
- Extract mctokens/access tokens (and other data) from session accounts

## Usage

```java
CommunicationHandler communicationHandler = new CommunicationHandler(YOUR_API_KEY);
CommunicationHandler.BalanceResponse balanceResponse = communicationHandler.getBalance();
System.out.println(balanceResponse);
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
