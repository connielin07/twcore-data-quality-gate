package com.twlab.qualitygate.validation;

import java.util.List;
import java.util.Set;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Type;
import org.springframework.stereotype.Component;

@Component
public class LabUnit002ObservationUcumCodeRule implements ContractRule {

	public static final String RULE_CODE = "LAB-UNIT-002";
	private static final String UCUM_SYSTEM = "http://unitsofmeasure.org";
	private static final Set<String> ALLOWED_UCUM_CODES = Set.of("mg/dL", "mmol/L");
	private static final String EXPECTED = "Observation.valueQuantity.system must be http://unitsofmeasure.org and code must be allowed by the exchange contract.";

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
					"Observation.value[x] is not present, so LAB-UNIT-002 cannot evaluate UCUM system/code."
			);
		}

		Type value = observation.getValue();
		if (!(value instanceof Quantity quantity)) {
			return notApplicable(
					"Observation/" + observationId + ".value[x]",
					value.fhirType(),
					"Observation.value[x] is not Quantity, so LAB-UNIT-002 does not apply."
			);
		}

		String path = "Observation/" + observationId + ".valueQuantity.system/code";
		String actual = actualUcumSummary(quantity);
		if (hasAllowedUcumSystemAndCode(quantity)) {
			return new RuleResult(
					RULE_CODE,
					RuleOutcome.PASS,
					"information",
					path,
					actual,
					EXPECTED,
					"Observation.valueQuantity uses the UCUM system and an allowed unit code from the MVP exchange contract.",
					"No fix needed."
			);
		}
		return fail(path, actual);
	}

	private boolean hasAllowedUcumSystemAndCode(Quantity quantity) {
		return quantity.hasSystem()
				&& UCUM_SYSTEM.equals(quantity.getSystem())
				&& quantity.hasCode()
				&& !quantity.getCode().isBlank()
				&& ALLOWED_UCUM_CODES.contains(quantity.getCode());
	}

	private String actualUcumSummary(Quantity quantity) {
		String system = quantity.hasSystem() && !quantity.getSystem().isBlank() ? quantity.getSystem() : "N/A";
		String code = quantity.hasCode() && !quantity.getCode().isBlank() ? quantity.getCode() : "N/A";
		return system + "|" + code;
	}

	private RuleResult fail(String path, String actual) {
		return new RuleResult(
				RULE_CODE,
				RuleOutcome.FAIL,
				"error",
				path,
				actual,
				EXPECTED,
				"Observation.valueQuantity system/code does not match the UCUM policy in the exchange contract.",
				"Use UCUM conditions allowed by the exchange contract: system must be http://unitsofmeasure.org, and code currently allows mg/dL or mmol/L. This rule is not full UCUM validation and does not parse syntax, convert units, or judge clinical plausibility."
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
