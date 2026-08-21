package com.twlab.qualitygate.validation;

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.context.FhirContext;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;

class LabUnit001ObservationQuantityUnitRuleTests {

	private final FhirContext fhirContext = FhirContext.forR4Cached();
	private final LabUnit001ObservationQuantityUnitRule rule = new LabUnit001ObservationQuantityUnitRule();

	@Test
	void passesWhenQuantityHasReadableUnit() {
		List<RuleResult> results = rule.validate(bundle("valid-minimal-lab-bundle.json"), TestExchangeContracts.DEMO_V11);

		assertThat(results).hasSize(1);
		assertThat(results.get(0).ruleCode()).isEqualTo("LAB-UNIT-001");
		assertThat(results.get(0).outcome()).isEqualTo(RuleOutcome.PASS);
		assertThat(results.get(0).severity()).isEqualTo("information");
		assertThat(results.get(0).path()).isEqualTo("Observation/obs-1.valueQuantity.unit");
		assertThat(results.get(0).actual()).isEqualTo("mg/dL");
		assertThat(results.get(0).expected())
				.isEqualTo("Observation.valueQuantity.unit must be present when value[x] is Quantity.");
	}

	@Test
	void failsWhenQuantityHasNoUnit() {
		List<RuleResult> results = rule.validate(bundle("observation-quantity-without-unit.json"), TestExchangeContracts.DEMO_V11);

		assertThat(results).hasSize(1);
		assertThat(results.get(0).ruleCode()).isEqualTo("LAB-UNIT-001");
		assertThat(results.get(0).outcome()).isEqualTo(RuleOutcome.FAIL);
		assertThat(results.get(0).severity()).isEqualTo("error");
		assertThat(results.get(0).path()).isEqualTo("Observation/obs-quantity-no-unit.valueQuantity.unit");
		assertThat(results.get(0).actual()).isEqualTo("N/A");
		assertThat(results.get(0).suggestion()).contains("partner-readable lab unit");
	}

	@Test
	void isNotApplicableWhenObservationValueIsNotQuantity() {
		List<RuleResult> results = rule.validate(bundle("observation-value-string.json"), TestExchangeContracts.DEMO_V11);

		assertThat(results).hasSize(1);
		assertThat(results.get(0).ruleCode()).isEqualTo("LAB-UNIT-001");
		assertThat(results.get(0).outcome()).isEqualTo(RuleOutcome.NOT_APPLICABLE);
		assertThat(results.get(0).severity()).isEqualTo("information");
		assertThat(results.get(0).path()).isEqualTo("Observation/obs-value-string.value[x]");
		assertThat(results.get(0).actual()).isEqualTo("string");
	}

	@Test
	void isNotApplicableWhenBundleHasNoObservation() {
		List<RuleResult> results = rule.validate(bundle("unsupported-resource-in-bundle.json"), TestExchangeContracts.DEMO_V11);

		assertThat(results).hasSize(1);
		assertThat(results.get(0).ruleCode()).isEqualTo("LAB-UNIT-001");
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
