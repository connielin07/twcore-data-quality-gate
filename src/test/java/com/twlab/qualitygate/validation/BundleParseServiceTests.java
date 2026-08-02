package com.twlab.qualitygate.validation;

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class BundleParseServiceTests {

	private final BundleParseService service = new BundleParseService(
			new ObjectMapper(),
			FhirContext.forR4Cached()
	);

	@Test
	void parsesBundleJson() {
		ValidationResult result = service.parse("""
				{
				  "resourceType": "Bundle",
				  "type": "collection",
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
		assertThat(result.resourceType()).isEqualTo("Bundle");
		assertThat(result.resourceCount()).isEqualTo(1);
		assertThat(result.errorMessage()).isNull();
	}

	@Test
	void reportsInvalidJsonWithoutThrowing() {
		ValidationResult result = service.parse("{ not-json");

		assertThat(result.jsonStatus()).isEqualTo(ParseStatus.FAILED);
		assertThat(result.fhirR4Status()).isEqualTo(ParseStatus.FAILED);
		assertThat(result.resourceTypeStatus()).isEqualTo(ParseStatus.FAILED);
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
		assertThat(result.errorMessage()).contains("FHIR R4 parse failed");
	}
}
