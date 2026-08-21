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
- Load bundled partner exchange contract files for rule activation and allowed LOINC/UCUM values.
- Optionally upload a partner contract JSON for the current validation run.
- Show `PASS`, `FAIL`, `NOT_APPLICABLE`, and `NOT_EVALUATED` rule evidence.
- Optionally compare contract v1.0 and v1.1 impact.
- Render an English-first `Quality Test Report` on the homepage.

Not included in the MVP:

- External FHIR Server reference lookup.
- Full terminology server validation.
- Unit conversion or clinical plausibility checks.
- Change Manifest or compatibility classification.
- Persistent history.

## Partner Exchange Contracts

The rule engine is implemented in Java, but partner-specific policy is loaded from bundled contract files:

- `src/main/resources/contracts/demo-lab-v1.0.json`
- `src/main/resources/contracts/demo-lab-v1.1.json`

These files define:

- contract id, name, and version
- enabled exchange rule codes
- allowed LOINC codes
- allowed UCUM codes

The default validation uses `demo-lab-hospital-a#1.1`.

Users may upload a contract JSON for the current validation run. The uploaded contract is checked for required metadata and known rule codes.

When `Compare contract versions` is selected, users must upload two or more contract version JSON files. The app runs the same Bundle against those uploaded contract versions in upload order; the first uploaded contract is the baseline for upgrade blocker evidence.

This is still an MVP demo contract workflow. It is not a TW Core official rule set, not a full terminology service, and not full contract schema validation.

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
Tests run: 65, Failures: 0, Errors: 0, Skipped: 0
```

## CI

GitHub Actions runs on push and pull request using Java 17:

- `./mvnw test`
- `./mvnw package`
- `docker build -t twcore-data-quality-gate:ci .`
