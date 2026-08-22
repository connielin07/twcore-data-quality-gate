# FHIR Validation and Partner Exchange Contracts

This project separates FHIR validation, TW Core validation, and partner-specific exchange contract checks. The separation is intentional: each layer answers a different interoperability question.

## Official validation layer

HL7 FHIR validation checks computable conformance aspects such as structure, cardinality, value domains, coding bindings, invariants, and profiles. The FHIR R4 validation page also calls out business rules as rules made outside the base specification, such as duplicate checks, reference resolution policy, authorization, and other workflow-specific requirements.

The standard FHIR `$validate` operation returns an `OperationOutcome` containing validation issues. That makes `OperationOutcome` the right evidence format for the project's FHIR R4 and TW Core validation sections.

Current official validator direction:

- HL7 FHIR Validator: https://validator.fhir.org/
- FHIR R4 validation documentation: https://hl7.org/fhir/R4/validation.html
- Inferno Resource Validator deprecation notice, July 13, 2026: https://inferno.healthit.gov/news/2026-07-planned-resource-validator-deprecation/

The Inferno notice matters because it says the Inferno Resource Validator service is planned for discontinuation as soon as August 2026 and points users to the HL7 FHIR Validator as an alternative. For this project, Inferno remains relevant for conformance test kits and API behavior testing, but not as the long-term reference for single-resource validation.

## TW Core layer

TW Core validation is the jurisdiction/profile layer for Taiwan. In this project, TW Core validation means validating selected R4 resources against the TW Core package and profiles when the validator support chain can load them reliably.

Current project baseline:

- FHIR version: R4 / 4.0.1
- TW Core package: `tw.gov.mohw.twcore#1.0.0`
- Official TW Core validation guide: https://twcore.mohw.gov.tw/ig/twcore/validates.html

TW Core validation is not treated as a partner policy rule. If TW Core validation runs and returns error or fatal issues, the Quality Gate blocks the exchange. If TW Core validation cannot be evaluated safely, the project reports `NOT_EVALUATED` instead of pretending the data passed.

## Partner exchange contract layer

The partner exchange contract layer answers a narrower operational question:

```text
For this partner and exchange scenario, which additional checks must pass before the Bundle is sent downstream?
```

Examples in this project:

- Which policy assertions from the partner guide are active for this exchange scenario.
- Which LOINC codes are accepted for the lab exchange scenario.
- Which UCUM unit codes are accepted for `Observation.valueQuantity`.
- Whether a policy assertion is a blocking `SHALL` requirement or a non-blocking `SHOULD` / `MAY` warning.
- Which contract lifecycle metadata and terminology expansion snapshot were used.

This layer is not a replacement for HL7 FHIR validation or TW Core validation. It is an MVP companion contract used by a pre-exchange quality gate.

## Real-world contract formats

There is no single FHIR resource named "TradingPartnerAgreement" that replaces all implementation agreements. In real projects, the computable and human-readable pieces are usually split across several artifacts:

- `ImplementationGuide`: a set of rules about how FHIR resources are used for a problem or workflow. Validators can use IG resources and packages to validate content against the guide. Reference: https://www.hl7.org/fhir/implementationguide.html
- `CapabilityStatement`: a statement of server or client capabilities for a FHIR version, including supported formats, resources, profiles, operations, and implementation guides. Reference: https://hl7.org/fhir/capabilitystatement.html
- Companion guide, interface specification, trading partner agreement, or implementation workbook: organization-specific policy that may include required scenarios, local code sets, cutover dates, allowed value subsets, and operational rules.

Public examples of exchange-focused IGs include:

- Da Vinci Payer Data Exchange (PDex) v2.2.0 current published version: https://hl7.org/fhir/us/davinci-pdex/
- CMS Recommended Implementation Guides and Standards page: https://www.cms.gov/initiatives/burden-reduction/overview/interoperability/implementation-guides-standards/standards-igs-index-resources

CMS describes recommended IGs as implementation information developed through industry-led, consensus-based public processes that can be used for API regulatory requirements. PDex is a useful comparison point because it is a real FHIR exchange guide, not a single JSON upload validator.

## Project mapping

| Real-world artifact | Project representation |
|---|---|
| HL7 FHIR Validator / `$validate` | FHIR R4 validation result and `OperationOutcome` issues |
| TW Core IG package | TW Core Profile validation section |
| ImplementationGuide / companion guide policy | `ExchangeContract.policyAssertions` JSON plus Java rule implementations |
| CapabilityStatement / API behavior requirements | Out of scope for the current Bundle-only MVP |
| Inferno / Touchstone conformance tests | Out of scope until the project validates a live FHIR server or client |

The bundled `demo-lab-v1.0.json` and `demo-lab-v1.1.json` files are intentionally small. They are not official TW Core rule sets. They are local partner contract examples that let the same lab Bundle be evaluated under different partner policy versions.

## Remaining production gaps

The current implementation is closer to a real companion contract than the first demo, but it is still not a production exchange agreement implementation.

| Gap | Current state |
|---|---|
| FHIR-native contract packaging | The JSON is project-specific. It is not an IG package, StructureDefinition, ValueSet, or CapabilityStatement. |
| Terminology validation | ValueSet canonical URLs and versions are documented, but runtime still checks local allowed code snapshots. |
| Lifecycle enforcement | Status and effective dates are captured, but validation does not yet reject retired, future, or superseded contracts. |
| Live API behavior | The app validates pasted/uploaded Bundles, not REST endpoints, search interactions, OAuth scopes, or server capability. |
| External references | Bundle-local references are resolved; external FHIR server lookup is still outside the MVP. |
| Audit and privacy | Provenance, AuditEvent, Consent, operator identity, PHI masking, and retention are not implemented. |
| Lab domain breadth | The rule pack still focuses on Patient, Observation, DiagnosticReport, LOINC, UCUM, and local references. |

The correct production path is to keep this quality gate as the pre-exchange evidence layer, then add IG/ValueSet/CapabilityStatement/terminology-server integration when validating real partner endpoints.
