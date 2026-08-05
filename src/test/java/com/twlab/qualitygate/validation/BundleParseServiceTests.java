package com.twlab.qualitygate.validation;

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twlab.qualitygate.config.FhirConfig;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class BundleParseServiceTests {

	private final FhirConfig fhirConfig = new FhirConfig();
	private final FhirContext fhirContext = FhirContext.forR4Cached();
	private final BundleParseService service = new BundleParseService(
			new ObjectMapper(),
			fhirContext,
			fhirConfig.fhirValidator(fhirContext),
			new TwCoreValidationService()
	);

	@Test
	void parsesBundleJson() {
		ValidationResult result = service.parse(fixture("valid-minimal-lab-bundle.json"));

		assertThat(result.jsonStatus()).isEqualTo(ParseStatus.PASSED);
		assertThat(result.fhirR4Status()).isEqualTo(ParseStatus.PASSED);
		assertThat(result.resourceTypeStatus()).isEqualTo(ParseStatus.PASSED);
		assertThat(result.fhirValidationStatus()).isEqualTo(ParseStatus.PASSED);
		assertThat(result.operationOutcomeIssues())
				.noneSatisfy(issue -> assertThat(issue.severity()).isIn("error", "fatal"));
		assertThat(result.resourceType()).isEqualTo("Bundle");
		assertThat(result.resourceCount()).isEqualTo(3);
		assertThat(result.resourceSummary().patientCount()).isEqualTo(1);
		assertThat(result.resourceSummary().observationCount()).isEqualTo(1);
		assertThat(result.resourceSummary().diagnosticReportCount()).isEqualTo(1);
		assertThat(result.resourceSummary().notEvaluatedCount()).isZero();
		assertThat(result.twCoreValidationResult().status()).isEqualTo(ParseStatus.NOT_EVALUATED);
		assertThat(result.twCoreValidationResult().packageId()).isEqualTo("tw.gov.mohw.twcore");
		assertThat(result.twCoreValidationResult().packageVersion()).isEqualTo("1.0.0");
		assertThat(result.twCoreValidationResult().message()).contains("不冒充 Profile 通過");
		assertThat(result.errorMessage()).isNull();
	}

	@Test
	void summarizesBundleEntriesForDay3ResourceInventory() {
		ValidationResult result = service.parse(fixture("valid-minimal-lab-bundle.json"));

		assertThat(result.bundleEntrySummaries())
				.extracting(BundleEntrySummary::resourceType)
				.containsExactly("Patient", "Observation", "DiagnosticReport");
		assertThat(result.bundleEntrySummaries())
				.extracting(BundleEntrySummary::id)
				.containsExactly("patient-1", "obs-1", "report-1");
		assertThat(result.bundleEntrySummaries())
				.extracting(BundleEntrySummary::fullUrl)
				.containsExactly(
						"urn:uuid:123e4567-e89b-12d3-a456-426614174000",
						"urn:uuid:223e4567-e89b-12d3-a456-426614174001",
						"urn:uuid:323e4567-e89b-12d3-a456-426614174002"
				);
		assertThat(result.bundleEntrySummaries())
				.extracting(BundleEntrySummary::evaluationStatus)
				.containsExactly("SUPPORTED", "SUPPORTED", "SUPPORTED");
	}

	@Test
	void marksUnsupportedBundleResourcesAsNotEvaluated() {
		ValidationResult result = service.parse(fixture("unsupported-resource-in-bundle.json"));

		assertThat(result.resourceSummary().patientCount()).isEqualTo(1);
		assertThat(result.resourceSummary().observationCount()).isZero();
		assertThat(result.resourceSummary().diagnosticReportCount()).isZero();
		assertThat(result.resourceSummary().notEvaluatedCount()).isEqualTo(1);
		assertThat(result.bundleEntrySummaries())
				.anySatisfy(entry -> {
					assertThat(entry.resourceType()).isEqualTo("Practitioner");
					assertThat(entry.id()).isEqualTo("practitioner-1");
					assertThat(entry.evaluationStatus()).isEqualTo("NOT_EVALUATED");
				});
	}

	@Test
	void reportsFhirValidationIssuesForInvalidBundle() {
		ValidationResult result = service.parse(fixture("missing-bundle-type.json"));

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
		assertThat(result.bundleEntrySummaries()).isEmpty();
		assertThat(result.resourceSummary()).isEqualTo(ResourceSummary.empty());
		assertThat(result.twCoreValidationResult().status()).isEqualTo(ParseStatus.NOT_EVALUATED);
		assertThat(result.twCoreValidationResult().message()).contains("Bundle gate 尚未通過");
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
		assertThat(result.bundleEntrySummaries()).isEmpty();
		assertThat(result.resourceSummary()).isEqualTo(ResourceSummary.empty());
		assertThat(result.twCoreValidationResult().status()).isEqualTo(ParseStatus.NOT_EVALUATED);
		assertThat(result.twCoreValidationResult().message()).contains("Bundle gate 尚未通過");
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
		assertThat(result.bundleEntrySummaries()).isEmpty();
		assertThat(result.resourceSummary()).isEqualTo(ResourceSummary.empty());
		assertThat(result.errorMessage()).contains("FHIR R4 parse failed");
	}

	private String fixture(String name) {
		try (var input = getClass().getResourceAsStream("/cases/" + name)) {
			if (input == null) {
				throw new IllegalArgumentException("Missing fixture: " + name);
			}
			return new String(input.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}
}
