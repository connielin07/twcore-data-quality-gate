package com.twlab.qualitygate.validation;

import java.util.List;
import java.util.Set;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;

public class LabCode001ObservationLoincRule implements ContractRule {

	public static final String RULE_CODE = "LAB-CODE-001";
	private static final String LOINC_SYSTEM = "http://loinc.org";
	private static final Set<String> ALLOWED_LOINC_CODES = Set.of("2345-7", "718-7");

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
			return List.of(notApplicable());
		}

		return observations.stream()
				.map(this::validateObservation)
				.toList();
	}

	private RuleResult validateObservation(Observation observation) {
		String path = "Observation/" + BundleReferenceIndex.idOrUnknown(observation) + ".code.coding";
		String actual = actualCodingSummary(observation);
		if (hasAllowedLoincCoding(observation)) {
			return new RuleResult(
					RULE_CODE,
					RuleOutcome.PASS,
					"information",
					path,
					actual,
					"Observation.code must include an allowed LOINC coding from the exchange contract.",
					"Matched allowed LOINC code from the MVP exchange contract.",
					"無需修正。"
			);
		}
		return fail(path, actual);
	}

	private boolean hasAllowedLoincCoding(Observation observation) {
		if (!observation.hasCode() || !observation.getCode().hasCoding()) {
			return false;
		}
		return observation.getCode().getCoding().stream()
				.anyMatch(coding -> LOINC_SYSTEM.equals(coding.getSystem())
						&& coding.hasCode()
						&& ALLOWED_LOINC_CODES.contains(coding.getCode()));
	}

	private String actualCodingSummary(Observation observation) {
		if (!observation.hasCode() || !observation.getCode().hasCoding()) {
			return "N/A";
		}
		List<Coding> codings = observation.getCode().getCoding();
		String summary = codings.stream()
				.map(this::codingSummary)
				.toList()
				.toString();
		return summary.equals("[]") ? "N/A" : summary;
	}

	private String codingSummary(Coding coding) {
		String system = coding.hasSystem() ? coding.getSystem() : "N/A";
		String code = coding.hasCode() ? coding.getCode() : "N/A";
		return system + "|" + code;
	}

	private RuleResult fail(String path, String actual) {
		return new RuleResult(
				RULE_CODE,
				RuleOutcome.FAIL,
				"error",
				path,
				actual,
				"Observation.code must include an allowed LOINC coding from the exchange contract.",
				"Observation.code does not include http://loinc.org with one of the allowed codes: 2345-7, 718-7.",
				"請改用合作契約允許的 LOINC code；目前 MVP 允許 2345-7 與 718-7。"
		);
	}

	private RuleResult notApplicable() {
		return new RuleResult(
				RULE_CODE,
				RuleOutcome.NOT_APPLICABLE,
				"information",
				"Bundle.entry",
				"N/A",
				"At least one Observation is required to evaluate LAB-CODE-001.",
				"No Observation resource found in this Bundle.",
				"無需修正；此規則不適用。"
		);
	}
}
