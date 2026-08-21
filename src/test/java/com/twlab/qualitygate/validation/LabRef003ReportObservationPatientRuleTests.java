package com.twlab.qualitygate.validation;

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.context.FhirContext;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;

class LabRef003ReportObservationPatientRuleTests {

	private final FhirContext fhirContext = FhirContext.forR4Cached();
	private final LabRef003ReportObservationPatientRule rule = new LabRef003ReportObservationPatientRule();

	@Test
	void passesWhenReportAndReferencedObservationPointToSamePatientById() {
		List<RuleResult> results = rule.validate(bundle("valid-internal-reference.json"), TestExchangeContracts.DEMO_V11);

		assertThat(results).hasSize(1);
		assertThat(results.get(0).ruleCode()).isEqualTo("LAB-REF-003");
		assertThat(results.get(0).outcome()).isEqualTo(RuleOutcome.PASS);
		assertThat(results.get(0).path()).isEqualTo("DiagnosticReport/report-valid-ref.result[0].reference");
		assertThat(results.get(0).actual())
				.isEqualTo("DiagnosticReport.subject=Patient/patient-valid-ref; Observation.subject=Patient/patient-valid-ref");
	}

	@Test
	void passesWhenReportSubjectUsesPatientFullUrlAndObservationSubjectUsesPatientId() {
		List<RuleResult> results = rule.validate(bundle("report-subject-full-url-observation-subject-id.json"), TestExchangeContracts.DEMO_V11);

		assertThat(results).hasSize(1);
		assertThat(results.get(0).ruleCode()).isEqualTo("LAB-REF-003");
		assertThat(results.get(0).outcome()).isEqualTo(RuleOutcome.PASS);
		assertThat(results.get(0).path()).isEqualTo("DiagnosticReport/report-full-url-match.result[0].reference");
		assertThat(results.get(0).actual()).contains("urn:uuid:b23e4567-e89b-12d3-a456-426614174030");
		assertThat(results.get(0).actual()).contains("Patient/patient-full-url-match");
	}

	@Test
	void failsWhenReportAndReferencedObservationPointToDifferentPatients() {
		List<RuleResult> results = rule.validate(bundle("mismatched-report-observation-patient.json"), TestExchangeContracts.DEMO_V11);

		assertThat(results).hasSize(1);
		assertThat(results.get(0).ruleCode()).isEqualTo("LAB-REF-003");
		assertThat(results.get(0).outcome()).isEqualTo(RuleOutcome.FAIL);
		assertThat(results.get(0).severity()).isEqualTo("error");
		assertThat(results.get(0).path()).isEqualTo("DiagnosticReport/report-different-patient.result[0].reference");
		assertThat(results.get(0).actual())
				.isEqualTo("DiagnosticReport.subject=Patient/patient-report; Observation.subject=Patient/patient-observation");
		assertThat(results.get(0).evidence()).contains("different Patients");
	}

	@Test
	void failsWhenDiagnosticReportResultObservationIsMissingFromBundle() {
		List<RuleResult> results = rule.validate(bundle("missing-report-result-reference.json"), TestExchangeContracts.DEMO_V11);

		assertThat(results).hasSize(1);
		assertThat(results.get(0).ruleCode()).isEqualTo("LAB-REF-003");
		assertThat(results.get(0).outcome()).isEqualTo(RuleOutcome.FAIL);
		assertThat(results.get(0).severity()).isEqualTo("error");
		assertThat(results.get(0).path()).isEqualTo("DiagnosticReport/report-missing-result-ref.result[0].reference");
		assertThat(results.get(0).actual()).isEqualTo("Observation/obs-not-in-bundle");
	}

	@Test
	void failsWhenDiagnosticReportHasNoResultReference() {
		List<RuleResult> results = rule.validate(bundle("missing-report-result-field.json"), TestExchangeContracts.DEMO_V11);

		assertThat(results).hasSize(1);
		assertThat(results.get(0).ruleCode()).isEqualTo("LAB-REF-003");
		assertThat(results.get(0).outcome()).isEqualTo(RuleOutcome.FAIL);
		assertThat(results.get(0).severity()).isEqualTo("error");
		assertThat(results.get(0).path()).isEqualTo("DiagnosticReport/report-no-result.result");
		assertThat(results.get(0).actual()).isEqualTo("N/A");
	}

	@Test
	void doesNotEvaluateExternalDiagnosticReportResultReference() {
		List<RuleResult> results = rule.validate(bundle("external-report-result-reference.json"), TestExchangeContracts.DEMO_V11);

		assertThat(results).hasSize(1);
		assertThat(results.get(0).ruleCode()).isEqualTo("LAB-REF-003");
		assertThat(results.get(0).outcome()).isEqualTo(RuleOutcome.NOT_EVALUATED);
		assertThat(results.get(0).severity()).isEqualTo("warning");
		assertThat(results.get(0).path()).isEqualTo("DiagnosticReport/report-external-result.result[0].reference");
		assertThat(results.get(0).actual()).isEqualTo("https://example.org/fhir/Observation/external-observation");
		assertThat(results.get(0).evidence()).contains("outside the MVP reference resolver boundary");
	}

	@Test
	void doesNotEvaluateExternalObservationSubjectReference() {
		List<RuleResult> results = rule.validate(bundle("external-observation-subject-reference.json"), TestExchangeContracts.DEMO_V11);

		assertThat(results).hasSize(1);
		assertThat(results.get(0).ruleCode()).isEqualTo("LAB-REF-003");
		assertThat(results.get(0).outcome()).isEqualTo(RuleOutcome.NOT_EVALUATED);
		assertThat(results.get(0).severity()).isEqualTo("warning");
		assertThat(results.get(0).path()).isEqualTo("DiagnosticReport/report-external-observation-subject.result[0].reference");
		assertThat(results.get(0).actual()).isEqualTo("https://example.org/fhir/Patient/external-patient");
		assertThat(results.get(0).evidence()).contains("outside the MVP reference resolver boundary");
	}

	@Test
	void isNotApplicableWhenBundleHasNoDiagnosticReport() {
		List<RuleResult> results = rule.validate(bundle("unsupported-resource-in-bundle.json"), TestExchangeContracts.DEMO_V11);

		assertThat(results).hasSize(1);
		assertThat(results.get(0).ruleCode()).isEqualTo("LAB-REF-003");
		assertThat(results.get(0).outcome()).isEqualTo(RuleOutcome.NOT_APPLICABLE);
		assertThat(results.get(0).severity()).isEqualTo("information");
	}

	private Bundle bundle(String fixtureName) {
		return (Bundle) fhirContext.newJsonParser().parseResource(fixture(fixtureName));
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
