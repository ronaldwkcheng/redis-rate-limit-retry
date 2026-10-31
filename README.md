# redis-rate-limit-retry

A Spring Boot sample that calls the [JSONPlaceholder](https://jsonplaceholder.typicode.com) `/users` API through a resilient `RestClient` with:

- **Automatic retry with exponential backoff** on transient gateway errors (`502`, `504`, `511`).
- **Client-side rate limiting** backed by [Bucket4j](https://github.com/bucket4j/bucket4j) and Redis, throttling calls to the external API's assumed limit of 5 requests/second instead of risking a `429`.

## Tech stack

- Java 25 (Gradle toolchain)
- Spring Boot 3.5.3 (`spring-boot-starter-web`, `spring-boot-starter-aop`, `spring-boot-starter-data-redis`)
- Spring Retry (`org.springframework.retry:spring-retry`)
- Bucket4j (`bucket4j_jdk17-core`, `bucket4j_jdk17-lettuce`) with a Redis-backed distributed bucket
- Lombok
- JUnit 5, Mockito, AssertJ

## Prerequisites

- JDK 25
- A local Redis instance on port `6379`. With Docker:

  ```bash
  docker run -d --name redis -p 6379:6379 redis:latest
  ```

## Running the app

```bash
./gradlew bootRun
```

The app starts on `http://localhost:8080`.

## API

| Method | Path              | Description                      |
|--------|-------------------|-----------------------------------|
| GET    | `/api/users`       | List all users                    |
| GET    | `/api/users/{id}`   | Get a user by id                  |
| POST   | `/api/users`        | Create a user                     |
| PUT    | `/api/users/{id}`   | Update a user                     |
| DELETE | `/api/users/{id}`   | Delete a user                     |

Each endpoint proxies to JSONPlaceholder via `UserClient`, which is wrapped with retry and rate-limiting behavior (see below).

## Retry behavior

`RestClientConfig` registers a status handler that converts `502` (Bad Gateway), `504` (Gateway Timeout), and `511` (Network Authentication Required) responses into a `RetryableServerException`. Every `UserClient` method is annotated with `@Retryable`, so a failing call is retried with exponential backoff before the exception is propagated to the caller. All other error statuses (e.g. `404`) behave as normal and are not retried.

Configurable via `application.yml`:

```yaml
client:
  retry:
    max-attempts: 3      # total attempts, including the first
    initial-delay-ms: 500
    multiplier: 2.0
```

## Rate limiting

`RateLimiterConfig` builds a Redis-backed `Bucket` (via Lettuce) sized to the external API's assumed limit. `ClientRateLimitAspect` wraps every method in the `com.ronald.rest.client` package and blocks on `bucket.asBlocking().consume(1)` before the call proceeds — so a call that would exceed the limit simply waits for the next refill window instead of being sent (and risking a `429`). Because the bucket lives in Redis, the limit is shared and enforced consistently even if the app is scaled across multiple instances.

Configurable via `application.yml`:

```yaml
client:
  rate-limiter:
    capacity: 5
    refill-tokens: 5
    refill-duration-seconds: 1
```

## Tests

```bash
./gradlew test
```

Requires the local Redis instance described above (the integration tests use it directly; there is no embedded/Testcontainers Redis).

- `UserClientRetryTest` — stubs a local HTTP server returning `502, 502, 200` and asserts `UserClient` retries with backoff until it succeeds.
- `UserClientRateLimitTest` — boots the full context against real Redis with a 1 token/sec bucket and asserts a call that exceeds the limit blocks for the refill window before proceeding.
- `ClientRateLimitAspectTest` — a pure unit test (mocked `Bucket`/`BlockingBucket`/`ProceedingJoinPoint`) verifying the aspect consumes a token before proceeding and never proceeds if the blocking wait is interrupted.
