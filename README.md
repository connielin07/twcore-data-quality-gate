# TW Lab Contract Gate

Spring Boot + HAPI FHIR R4 quality gate for TW Core lab Bundle exchange evidence.

## Scope

- Paste or upload Bundle JSON.
- Parse JSON safely.
- Parse FHIR R4 resources with HAPI FHIR.
- Gate input to collection Bundles.
- Run FHIR R4 validation and show OperationOutcome issues.
- Run TW Core Profile validation when the package can be loaded safely.
- Run six MVP exchange contract rules.
- Show `PASS`, `FAIL`, `NOT_APPLICABLE`, and `NOT_EVALUATED` rule evidence.
- Compare contract v1.0 and v1.1 impact.
- Render an English-first `Quality Test Report` on the homepage.

Not included in the MVP:

- External FHIR Server reference lookup.
- Full terminology server validation.
- Unit conversion or clinical plausibility checks.
- Change Manifest or compatibility classification.
- Persistent history.

## Run Locally

```bash
./mvnw spring-boot:run
```

Open:

```text
http://localhost:8080
```

## Docker

Build the image:

```bash
docker build -t twcore-data-quality-gate:local .
```

The Docker image uses a Java 17 multi-stage build. The build stage runs Maven package from source, so the image does not depend on a local `target` directory.

Run the container:

```bash
docker run --rm -p 8080:8080 twcore-data-quality-gate:local
```

Open:

```text
http://localhost:8080
```

## Docker Compose

```bash
docker compose up --build
```

Open:

```text
http://localhost:8080
```

Stop:

```bash
docker compose down
```

The Compose service publishes port `8080`, uses image name `twcore-data-quality-gate:local`, and allows JVM tuning through `JAVA_OPTS`.

If Docker reports a local Buildx permission error under `~/.docker/buildx`, retry with an isolated Docker config directory:

```bash
mkdir -p /tmp/twcore-docker-config
DOCKER_CONFIG=/tmp/twcore-docker-config docker build -t twcore-data-quality-gate:local .
```

## Test

```bash
./mvnw test
```

Current expected result:

```text
Tests run: 60, Failures: 0, Errors: 0, Skipped: 0
```

## CI

GitHub Actions runs on push and pull request using Java 17:

- `./mvnw test`
- `./mvnw package`
- `docker build -t twcore-data-quality-gate:ci .`
