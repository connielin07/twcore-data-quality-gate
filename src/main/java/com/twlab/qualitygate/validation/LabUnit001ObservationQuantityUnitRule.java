package com.twlab.qualitygate.validation;

import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Type;
import org.springframework.stereotype.Component;

@Component
public class LabUnit001ObservationQuantityUnitRule implements ContractRule {

	public static final String RULE_CODE = "LAB-UNIT-001";
	private static final String EXPECTED = "Observation.valueQuantity.unit must be present when value[x] is Quantity.";

	@Override
	public String ruleCode() {
		return RULE_CODE;
	}

	@Override
	public List<RuleResult> validate(Bundle bundle) {
		List<Observation> observations = bundle.getEntry().stream()
				.map(Bundle.BundleEntryComponent::getResource)
				.filter(Observation.class::isInstance)
				.map(Observation.class::cast)
				.toList();
		if (observations.isEmpty()) {
			return List.of(notApplicable("Bundle.entry", "N/A", "No Observation resource found in this Bundle."));
		}

		return observations.stream()
				.map(this::validateObservation)
				.toList();
	}

	private RuleResult validateObservation(Observation observation) {
		String observationId = BundleReferenceIndex.idOrUnknown(observation);
		if (!observation.hasValue()) {
			return notApplicable(
					"Observation/" + observationId + ".value[x]",
					"N/A",
					"Observation.value[x] is not present, so LAB-UNIT-001 cannot evaluate a Quantity unit."
			);
		}

		Type value = observation.getValue();
		if (!(value instanceof Quantity quantity)) {
			return notApplicable(
					"Observation/" + observationId + ".value[x]",
					value.fhirType(),
					"Observation.value[x] is not Quantity, so LAB-UNIT-001 does not apply."
			);
		}

		String path = "Observation/" + observationId + ".valueQuantity.unit";
		String actual = actualUnit(quantity);
		if (hasReadableUnit(quantity)) {
			return new RuleResult(
					RULE_CODE,
					RuleOutcome.PASS,
					"information",
					path,
					actual,
					EXPECTED,
					"Observation.valueQuantity.unit is present.",
					"No fix needed."
			);
		}
		return fail(path, actual);
	}

	private boolean hasReadableUnit(Quantity quantity) {
		return quantity.hasUnit() && !quantity.getUnit().isBlank();
	}

	private String actualUnit(Quantity quantity) {
		return hasReadableUnit(quantity) ? quantity.getUnit() : "N/A";
	}

	private RuleResult fail(String path, String actual) {
		return new RuleResult(
				RULE_CODE,
				RuleOutcome.FAIL,
				"error",
				path,
				actual,
				EXPECTED,
				"Observation.valueQuantity exists, but unit is missing or blank.",
				"Add a partner-readable lab unit such as mg/dL. UCUM system/code is checked separately by LAB-UNIT-002."
		);
	}

	private RuleResult notApplicable(String path, String actual, String evidence) {
		return new RuleResult(
				RULE_CODE,
				RuleOutcome.NOT_APPLICABLE,
				"information",
				path,
				actual,
				EXPECTED,
				evidence,
				"No fix needed; this rule does not apply."
		);
	}
}
