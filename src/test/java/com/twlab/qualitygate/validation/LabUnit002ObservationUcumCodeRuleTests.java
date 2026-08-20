package com.twlab.qualitygate.validation;

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.context.FhirContext;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;

class LabUnit002ObservationUcumCodeRuleTests {

	private final FhirContext fhirContext = FhirContext.forR4Cached();
	private final LabUnit002ObservationUcumCodeRule rule = new LabUnit002ObservationUcumCodeRule();

	@Test
	void passesWhenQuantityHasAllowedUcumSystemAndCode() {
		List<RuleResult> results = rule.validate(bundle("valid-ucum-code.json"));

		assertThat(results).hasSize(1);
		assertThat(results.get(0).ruleCode()).isEqualTo("LAB-UNIT-002");
		assertThat(results.get(0).outcome()).isEqualTo(RuleOutcome.PASS);
		assertThat(results.get(0).severity()).isEqualTo("information");
		assertThat(results.get(0).path()).isEqualTo("Observation/obs-valid-ucum.valueQuantity.system/code");
		assertThat(results.get(0).actual()).isEqualTo("http://unitsofmeasure.org|mg/dL");
		assertThat(results.get(0).expected())
				.isEqualTo("Observation.valueQuantity.system must be http://unitsofmeasure.org and code must be allowed by the exchange contract.");
	}

	@Test
	void failsWhenQuantitySystemIsNotUcum() {
		List<RuleResult> results = rule.validate(bundle("observation-quantity-wrong-ucum-system.json"));

		assertThat(results).hasSize(1);
		assertThat(results.get(0).ruleCode()).isEqualTo("LAB-UNIT-002");
		assertThat(results.get(0).outcome()).isEqualTo(RuleOutcome.FAIL);
		assertThat(results.get(0).severity()).isEqualTo("error");
		assertThat(results.get(0).path()).isEqualTo("Observation/obs-wrong-ucum-system.valueQuantity.system/code");
		assertThat(results.get(0).actual()).isEqualTo("http://example.org/local-units|mg/dL");
		assertThat(results.get(0).suggestion()).contains("system must be http://unitsofmeasure.org");
	}

	@Test
	void failsWhenQuantityCodeIsNotAllowedByContract() {
		List<RuleResult> results = rule.validate(bundle("observation-quantity-ucum-code-not-allowed.json"));

		assertThat(results).hasSize(1);
		assertThat(results.get(0).ruleCode()).isEqualTo("LAB-UNIT-002");
		assertThat(results.get(0).outcome()).isEqualTo(RuleOutcome.FAIL);
		assertThat(results.get(0).severity()).isEqualTo("error");
		assertThat(results.get(0).path()).isEqualTo("Observation/obs-ucum-code-not-allowed.valueQuantity.system/code");
		assertThat(results.get(0).actual()).isEqualTo("http://unitsofmeasure.org|g/L");
		assertThat(results.get(0).suggestion()).contains("code currently allows mg/dL or mmol/L");
	}

	@Test
	void failsWhenQuantityCodeIsMissing() {
		List<RuleResult> results = rule.validate(bundle("observation-quantity-without-ucum-code.json"));

		assertThat(results).hasSize(1);
		assertThat(results.get(0).ruleCode()).isEqualTo("LAB-UNIT-002");
		assertThat(results.get(0).outcome()).isEqualTo(RuleOutcome.FAIL);
		assertThat(results.get(0).severity()).isEqualTo("error");
		assertThat(results.get(0).path()).isEqualTo("Observation/obs-no-ucum-code.valueQuantity.system/code");
		assertThat(results.get(0).actual()).isEqualTo("http://unitsofmeasure.org|N/A");
	}

	@Test
	void isNotApplicableWhenObservationValueIsNotQuantity() {
		List<RuleResult> results = rule.validate(bundle("observation-value-string.json"));

		assertThat(results).hasSize(1);
		assertThat(results.get(0).ruleCode()).isEqualTo("LAB-UNIT-002");
		assertThat(results.get(0).outcome()).isEqualTo(RuleOutcome.NOT_APPLICABLE);
		assertThat(results.get(0).severity()).isEqualTo("information");
		assertThat(results.get(0).path()).isEqualTo("Observation/obs-value-string.value[x]");
		assertThat(results.get(0).actual()).isEqualTo("string");
	}

	@Test
	void isNotApplicableWhenBundleHasNoObservation() {
		List<RuleResult> results = rule.validate(bundle("unsupported-resource-in-bundle.json"));

		assertThat(results).hasSize(1);
		assertThat(results.get(0).ruleCode()).isEqualTo("LAB-UNIT-002");
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
