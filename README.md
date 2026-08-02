# TW Lab Contract Gate

Day 1 minimal Spring Boot + HAPI FHIR R4 validation skeleton.

## Scope

- Paste or upload Bundle JSON.
- Parse JSON safely.
- Parse FHIR R4 Bundle with HAPI FHIR.
- Show JSON parse status, FHIR R4 parse status, resource type, resource count, and error message.

Not included in Day 1:

- TW Core package loading.
- Profile validation.
- OperationOutcome formatting.
- Reference rules.
- Contract version comparison.

## Run

```bash
./mvnw spring-boot:run
```

Open:

```text
http://localhost:8080
```

## Test

```bash
./mvnw test
```
