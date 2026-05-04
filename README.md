# Chat Backend (Spring Boot)

Small WebSocket chat backend using Java, Spring Boot and Maven. The project processes incoming JSON chat messages off the WebSocket thread (asynchronously) and broadcasts to connected sessions. Tests cover async behavior and use Mockito.

## Requirements
- Java 25+ (or the version configured in `pom.xml`)
- Maven 3.6+

## Build
Build the project:
```bash
mvn package
```

## Run
Build the project without running tests:
```bash
mvn spring-boot:run
```

## Port
Default port in 8080

## Frontend 
This app work with an Angular frontend which runs on port 4200 by default, you can find it here https://github.com/zwakelem/chatfrontend
