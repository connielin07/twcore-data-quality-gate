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
- Load bundled partner exchange contract files for policy assertions and allowed LOINC/UCUM values.
- Optionally upload a partner contract JSON for the current validation run.
- Apply `SHALL` policy failures as blocking issues and `SHOULD` / `MAY` policy failures as warnings.
- Show `PASS`, `FAIL`, `NOT_APPLICABLE`, and `NOT_EVALUATED` rule evidence.
- Optionally compare contract v1.0 and v1.1 impact.
- Render an English-first `Quality Test Report` on the homepage.

Not included in the MVP:

- External FHIR Server reference lookup.
- Full terminology server validation.
- Unit conversion or clinical plausibility checks.
- Change Manifest or compatibility classification.
- CapabilityStatement validation for a live FHIR API.
- Authentication, authorization, Consent, Provenance, AuditEvent, and PHI handling.
- Persistent history.

## Partner Exchange Contracts

The rule engine is implemented in Java, but partner-specific policy is loaded from bundled contract files:

- `src/main/resources/contracts/demo-lab-v1.0.json`
- `src/main/resources/contracts/demo-lab-v1.1.json`

A reference-only specimen also documents how this MVP contract shape maps to real exchange artifacts:

- `src/main/resources/contracts/demo-lab-hospital-a-v1.2-reference.json`

These files define:

- contract id, name, and version
- lifecycle metadata such as status, publisher, jurisdiction, effective date, retire date, and FHIR version
- policy assertions mapped to executable exchange rules
- obligation and severity for each assertion
- terminology policy metadata, including ValueSet canonical URLs and local expansion timestamp
- local allowed LOINC and UCUM expansion snapshots

The default validation uses `demo-lab-hospital-a#1.1`.

Users may upload a contract JSON for the current validation run. The uploaded contract is checked for required metadata and known rule codes.

When `Compare contract versions` is selected, users must upload two or more contract version JSON files. The app runs the same Bundle against those uploaded contract versions in upload order; the first uploaded contract is the baseline for upgrade blocker evidence.

This is still an MVP demo contract workflow. The project contract JSON is not a FHIR official resource, not a TW Core official rule set, not a full terminology service, and not full contract schema validation.

In real FHIR exchange projects, partner requirements are usually split across artifacts such as:

- FHIR `ImplementationGuide` packages for computable profiles, value sets, examples, and workflow guidance.
- FHIR `CapabilityStatement` resources for server or client API capabilities, supported formats, profiles, operations, and implementation guides.
- Companion guides, interface specifications, trading partner agreements, or implementation workbooks for organization-specific policies.

This project's contract JSON is a small companion-contract representation for a pre-exchange quality gate. `policyAssertions` are companion-guide-style statements mapped to executable rule implementations; they are not FHIR-native fields. `SHALL` assertions with `error` or `fatal` severity block exchange, while `SHOULD` / `MAY` failures are reported as warnings. The JSON does not replace HL7 FHIR Validator, FHIR `$validate`, TW Core validation, or a terminology server. It adds partner policy evidence after the Bundle has been parsed and evaluated against the standard validation layers.

The terminology fields are still local expansion snapshots. A production implementation should resolve the declared ValueSet canonical URLs through a terminology server and record the expansion version used for each validation run.

For the current reference notes, see `docs/references/fhir-validation-and-exchange-contracts.md`.

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
docker build \
  --build-arg APP_VERSION=0.0.1-SNAPSHOT \
  --build-arg VCS_REF="$(git rev-parse --short HEAD)" \
  --build-arg BUILD_DATE="$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
  -t twcore-data-quality-gate:local .
```

The Docker image uses a Java 17 multi-stage build. The build stage runs Maven package from source, so the image does not depend on a local `target` directory. `.dockerignore` keeps Git metadata, local IDE files, build output, and documentation-only files out of the image build context.

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

The Compose service publishes port `8080` by default, uses image name `twcore-data-quality-gate:local`, and allows JVM tuning through `JAVA_OPTS`.

Useful overrides:

```bash
APP_PORT=18080 docker compose up --build
JAVA_OPTS="-XX:MaxRAMPercentage=70" docker compose up --build
APP_IMAGE=twcore-data-quality-gate:dev docker compose build
```

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
Tests run: 66, Failures: 0, Errors: 0, Skipped: 0
```

## CI

GitHub Actions runs on push and pull request using Java 17:

- `./mvnw test`
- `./mvnw package`
- `docker build -t twcore-data-quality-gate:ci .`
