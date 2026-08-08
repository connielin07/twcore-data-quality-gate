package com.twlab.qualitygate.validation;

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.context.FhirContext;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;

class LabRef001ObservationSubjectRuleTests {

	private final FhirContext fhirContext = FhirContext.forR4Cached();
	private final LabRef001ObservationSubjectRule rule = new LabRef001ObservationSubjectRule();

	@Test
	void passesWhenObservationSubjectPointsToBundlePatientById() {
		List<RuleResult> results = rule.validate(bundle("valid-internal-reference.json"));

		assertThat(results).hasSize(1);
		assertThat(results.get(0).ruleCode()).isEqualTo("LAB-REF-001");
		assertThat(results.get(0).outcome()).isEqualTo(RuleOutcome.PASS);
		assertThat(results.get(0).actual()).isEqualTo("Patient/patient-valid-ref");
	}

	@Test
	void failsWhenObservationSubjectPatientIsMissingFromBundle() {
		List<RuleResult> results = rule.validate(bundle("missing-internal-reference.json"));

		assertThat(results).hasSize(1);
		assertThat(results.get(0).outcome()).isEqualTo(RuleOutcome.FAIL);
		assertThat(results.get(0).severity()).isEqualTo("error");
		assertThat(results.get(0).actual()).isEqualTo("Patient/patient-not-in-bundle");
		assertThat(results.get(0).suggestion()).contains("Bundle.entry");
	}

	@Test
	void doesNotEvaluateExternalHttpReference() {
		List<RuleResult> results = rule.validate(bundle("external-http-reference.json"));

		assertThat(results).hasSize(1);
		assertThat(results.get(0).outcome()).isEqualTo(RuleOutcome.NOT_EVALUATED);
		assertThat(results.get(0).severity()).isEqualTo("warning");
		assertThat(results.get(0).actual()).isEqualTo("https://example.org/fhir/Patient/external-patient");
		assertThat(results.get(0).evidence()).contains("outside the MVP reference resolver boundary");
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
