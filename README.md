# Library API

A small Spring Boot project for managing **authors**, **books**, and **loans** in a library system.

## Tech stack
- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
- H2 Database
- Lombok
- Spring Validation
- SpringDoc OpenAPI / Swagger UI
- Maven

## Features
- CRUD-style API structure for authors, books, and loans
- Versioned REST API with **v1** and **v2** endpoints
- JPA relations between `Author`, `Book`, and `Loan`
- Validation and centralized error handling
- Simple caching for book-related reads
- Concurrency handling for loan creation

## API versions
- `api/v1/...` = original version
- `api/v2/...` = newer version with extended book support such as `genre`

## Run the project
```powershell
./mvnw.cmd spring-boot:run
```

## Useful URLs
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI docs: `http://localhost:8080/v3/api-docs`
- H2 Console: `http://localhost:8080/h2-console`

## Run tests
```powershell
./mvnw.cmd test
```

## Project purpose
This project appears to be built as a backend learning project for practicing:
- REST API design
- JPA entity relationships
- validation and exception handling
- API versioning
- concurrency and scalability concepts

