package com.twlab.qualitygate.validation;

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.context.FhirContext;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;

class LabCode001ObservationLoincRuleTests {

	private final FhirContext fhirContext = FhirContext.forR4Cached();
	private final LabCode001ObservationLoincRule rule = new LabCode001ObservationLoincRule();

	@Test
	void passesWhenObservationHasAllowedLoincCode() {
		List<RuleResult> results = rule.validate(bundle("valid-loinc-code.json"));

		assertThat(results).hasSize(1);
		assertThat(results.get(0).ruleCode()).isEqualTo("LAB-CODE-001");
		assertThat(results.get(0).outcome()).isEqualTo(RuleOutcome.PASS);
		assertThat(results.get(0).severity()).isEqualTo("information");
		assertThat(results.get(0).path()).isEqualTo("Observation/obs-valid-loinc.code.coding");
		assertThat(results.get(0).actual()).contains("http://loinc.org|2345-7");
		assertThat(results.get(0).expected())
				.isEqualTo("Observation.code must include an allowed LOINC coding from the exchange contract.");
	}

	@Test
	void failsWhenObservationLoincCodeIsNotAllowedByContract() {
		List<RuleResult> results = rule.validate(bundle("loinc-code-not-allowed.json"));

		assertThat(results).hasSize(1);
		assertThat(results.get(0).ruleCode()).isEqualTo("LAB-CODE-001");
		assertThat(results.get(0).outcome()).isEqualTo(RuleOutcome.FAIL);
		assertThat(results.get(0).severity()).isEqualTo("error");
		assertThat(results.get(0).path()).isEqualTo("Observation/obs-disallowed-loinc.code.coding");
		assertThat(results.get(0).actual()).contains("http://loinc.org|9999-9");
		assertThat(results.get(0).suggestion()).contains("LOINC code allowed by the exchange contract");
	}

	@Test
	void failsWhenObservationCodeHasNoCoding() {
		List<RuleResult> results = rule.validate(bundle("observation-code-without-coding.json"));

		assertThat(results).hasSize(1);
		assertThat(results.get(0).ruleCode()).isEqualTo("LAB-CODE-001");
		assertThat(results.get(0).outcome()).isEqualTo(RuleOutcome.FAIL);
		assertThat(results.get(0).severity()).isEqualTo("error");
		assertThat(results.get(0).path()).isEqualTo("Observation/obs-no-coding.code.coding");
		assertThat(results.get(0).actual()).isEqualTo("N/A");
	}

	@Test
	void failsWhenObservationCodingHasNoCode() {
		List<RuleResult> results = rule.validate(bundle("observation-coding-without-code.json"));

		assertThat(results).hasSize(1);
		assertThat(results.get(0).ruleCode()).isEqualTo("LAB-CODE-001");
		assertThat(results.get(0).outcome()).isEqualTo(RuleOutcome.FAIL);
		assertThat(results.get(0).severity()).isEqualTo("error");
		assertThat(results.get(0).path()).isEqualTo("Observation/obs-coding-no-code.code.coding");
		assertThat(results.get(0).actual()).contains("http://loinc.org|N/A");
	}

	@Test
	void isNotApplicableWhenBundleHasNoObservation() {
		List<RuleResult> results = rule.validate(bundle("unsupported-resource-in-bundle.json"));

		assertThat(results).hasSize(1);
		assertThat(results.get(0).ruleCode()).isEqualTo("LAB-CODE-001");
		assertThat(results.get(0).outcome()).isEqualTo(RuleOutcome.NOT_APPLICABLE);
		assertThat(results.get(0).severity()).isEqualTo("information");
		assertThat(results.get(0).path()).isEqualTo("Bundle.entry");
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
