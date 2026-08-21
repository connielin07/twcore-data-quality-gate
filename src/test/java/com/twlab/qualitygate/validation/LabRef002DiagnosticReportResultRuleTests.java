package com.twlab.qualitygate.validation;

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.context.FhirContext;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;

class LabRef002DiagnosticReportResultRuleTests {

	private final FhirContext fhirContext = FhirContext.forR4Cached();
	private final LabRef002DiagnosticReportResultRule rule = new LabRef002DiagnosticReportResultRule();

	@Test
	void passesWhenDiagnosticReportResultPointsToBundleObservationById() {
		List<RuleResult> results = rule.validate(bundle("valid-internal-reference.json"), TestExchangeContracts.DEMO_V11);

		assertThat(results).hasSize(1);
		assertThat(results.get(0).ruleCode()).isEqualTo("LAB-REF-002");
		assertThat(results.get(0).outcome()).isEqualTo(RuleOutcome.PASS);
		assertThat(results.get(0).actual()).isEqualTo("Observation/obs-valid-ref");
	}

	@Test
	void passesWhenDiagnosticReportResultPointsToBundleObservationByFullUrl() {
		List<RuleResult> results = rule.validate(bundle("valid-report-result-full-url-reference.json"), TestExchangeContracts.DEMO_V11);

		assertThat(results).hasSize(1);
		assertThat(results.get(0).ruleCode()).isEqualTo("LAB-REF-002");
		assertThat(results.get(0).outcome()).isEqualTo(RuleOutcome.PASS);
		assertThat(results.get(0).actual()).isEqualTo("urn:uuid:223e4567-e89b-12d3-a456-426614174001");
	}

	@Test
	void failsWhenDiagnosticReportResultObservationIsMissingFromBundle() {
		List<RuleResult> results = rule.validate(bundle("missing-report-result-reference.json"), TestExchangeContracts.DEMO_V11);

		assertThat(results).hasSize(1);
		assertThat(results.get(0).ruleCode()).isEqualTo("LAB-REF-002");
		assertThat(results.get(0).outcome()).isEqualTo(RuleOutcome.FAIL);
		assertThat(results.get(0).severity()).isEqualTo("error");
		assertThat(results.get(0).actual()).isEqualTo("Observation/obs-not-in-bundle");
		assertThat(results.get(0).suggestion()).contains("DiagnosticReport.result.reference");
	}

	@Test
	void failsWhenDiagnosticReportHasNoResultReference() {
		List<RuleResult> results = rule.validate(bundle("missing-report-result-field.json"), TestExchangeContracts.DEMO_V11);

		assertThat(results).hasSize(1);
		assertThat(results.get(0).ruleCode()).isEqualTo("LAB-REF-002");
		assertThat(results.get(0).outcome()).isEqualTo(RuleOutcome.FAIL);
		assertThat(results.get(0).severity()).isEqualTo("error");
		assertThat(results.get(0).path()).isEqualTo("DiagnosticReport/report-no-result.result");
		assertThat(results.get(0).actual()).isEqualTo("N/A");
	}

	@Test
	void doesNotEvaluateExternalHttpReference() {
		List<RuleResult> results = rule.validate(bundle("external-report-result-reference.json"), TestExchangeContracts.DEMO_V11);

		assertThat(results).hasSize(1);
		assertThat(results.get(0).ruleCode()).isEqualTo("LAB-REF-002");
		assertThat(results.get(0).outcome()).isEqualTo(RuleOutcome.NOT_EVALUATED);
		assertThat(results.get(0).severity()).isEqualTo("warning");
		assertThat(results.get(0).actual()).isEqualTo("https://example.org/fhir/Observation/external-observation");
		assertThat(results.get(0).evidence()).contains("outside the MVP reference resolver boundary");
	}

	@Test
	void isNotApplicableWhenBundleHasNoDiagnosticReport() {
		List<RuleResult> results = rule.validate(bundle("unsupported-resource-in-bundle.json"), TestExchangeContracts.DEMO_V11);

		assertThat(results).hasSize(1);
		assertThat(results.get(0).ruleCode()).isEqualTo("LAB-REF-002");
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
