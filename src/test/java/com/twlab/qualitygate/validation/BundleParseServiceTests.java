package com.twlab.qualitygate.validation;

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.context.FhirContext;
import com.twlab.qualitygate.config.FhirConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class BundleParseServiceTests {

	private final FhirConfig fhirConfig = new FhirConfig();
	private final FhirContext fhirContext = FhirContext.forR4Cached();
	private final BundleParseService service = new BundleParseService(
			new ObjectMapper(),
			fhirContext,
			fhirConfig.fhirValidator(fhirContext)
	);

	@Test
	void parsesBundleJson() {
		ValidationResult result = service.parse("""
				{
				  "resourceType": "Bundle",
				  "type": "collection",
				  "entry": [
				    {
				      "fullUrl": "urn:uuid:123e4567-e89b-12d3-a456-426614174000",
				      "resource": {
				        "resourceType": "Patient",
				        "id": "patient-1"
				      }
				    }
				  ]
				}
				""");

		assertThat(result.jsonStatus()).isEqualTo(ParseStatus.PASSED);
		assertThat(result.fhirR4Status()).isEqualTo(ParseStatus.PASSED);
		assertThat(result.resourceTypeStatus()).isEqualTo(ParseStatus.PASSED);
		assertThat(result.fhirValidationStatus()).isEqualTo(ParseStatus.PASSED);
		assertThat(result.operationOutcomeIssues())
				.noneSatisfy(issue -> assertThat(issue.severity()).isIn("error", "fatal"));
		assertThat(result.resourceType()).isEqualTo("Bundle");
		assertThat(result.resourceCount()).isEqualTo(1);
		assertThat(result.errorMessage()).isNull();
	}

	@Test
	void reportsFhirValidationIssuesForInvalidBundle() {
		ValidationResult result = service.parse("""
				{
				  "resourceType": "Bundle",
				  "entry": [
				    {
				      "resource": {
				        "resourceType": "Patient",
				        "id": "patient-1"
				      }
				    }
				  ]
				}
				""");

		assertThat(result.jsonStatus()).isEqualTo(ParseStatus.PASSED);
		assertThat(result.fhirR4Status()).isEqualTo(ParseStatus.PASSED);
		assertThat(result.resourceTypeStatus()).isEqualTo(ParseStatus.PASSED);
		assertThat(result.fhirValidationStatus()).isEqualTo(ParseStatus.FAILED);
		assertThat(result.operationOutcomeIssues())
				.anySatisfy(issue -> {
					assertThat(issue.severity()).isIn("error", "fatal");
					assertThat(issue.diagnostics()).containsIgnoringCase("Bundle.type");
				});
	}

	@Test
	void reportsInvalidJsonWithoutThrowing() {
		ValidationResult result = service.parse("{ not-json");

		assertThat(result.jsonStatus()).isEqualTo(ParseStatus.FAILED);
		assertThat(result.fhirR4Status()).isEqualTo(ParseStatus.FAILED);
		assertThat(result.resourceTypeStatus()).isEqualTo(ParseStatus.FAILED);
		assertThat(result.fhirValidationStatus()).isEqualTo(ParseStatus.NOT_EVALUATED);
		assertThat(result.operationOutcomeIssues()).isEmpty();
		assertThat(result.errorMessage()).contains("JSON parse failed");
	}

	@Test
	void reportsNonBundleFhirResource() {
		ValidationResult result = service.parse("""
				{
				  "resourceType": "Patient",
				  "id": "patient-1"
				}
				""");

		assertThat(result.jsonStatus()).isEqualTo(ParseStatus.PASSED);
		assertThat(result.fhirR4Status()).isEqualTo(ParseStatus.PASSED);
		assertThat(result.resourceTypeStatus()).isEqualTo(ParseStatus.FAILED);
		assertThat(result.fhirValidationStatus()).isEqualTo(ParseStatus.NOT_EVALUATED);
		assertThat(result.operationOutcomeIssues()).isEmpty();
		assertThat(result.resourceType()).isEqualTo("Patient");
		assertThat(result.errorMessage()).contains("resourceType is not Bundle");
	}

	@Test
	void reportsPlainJsonAsFhirFailure() {
		ValidationResult result = service.parse("""
				{
				  "hello": "world"
				}
				""");

		assertThat(result.jsonStatus()).isEqualTo(ParseStatus.PASSED);
		assertThat(result.fhirR4Status()).isEqualTo(ParseStatus.FAILED);
		assertThat(result.resourceTypeStatus()).isEqualTo(ParseStatus.FAILED);
		assertThat(result.fhirValidationStatus()).isEqualTo(ParseStatus.NOT_EVALUATED);
		assertThat(result.operationOutcomeIssues()).isEmpty();
		assertThat(result.errorMessage()).contains("FHIR R4 parse failed");
	}
}
