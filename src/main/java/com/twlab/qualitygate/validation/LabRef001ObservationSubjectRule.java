package com.twlab.qualitygate.validation;

import java.util.List;
import java.util.Set;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

@Component
public class LabRef001ObservationSubjectRule implements ContractRule {

	public static final String RULE_CODE = "LAB-REF-001";

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

		Set<String> patientReferences = BundleReferenceIndex.referencesTo(bundle, Patient.class, "Patient");
		return observations.stream()
				.map(observation -> validateObservation(observation, patientReferences))
				.toList();
	}

	private RuleResult validateObservation(Observation observation, Set<String> patientReferences) {
		String actual = observation.hasSubject() ? observation.getSubject().getReference() : null;
		String path = "Observation/" + idOrUnknown(observation) + ".subject.reference";
		if (actual == null || actual.isBlank()) {
			return fail(path, "N/A", "Observation.subject.reference is required for LAB-REF-001.");
		}
		if (BundleReferenceIndex.isExternalReference(actual)) {
			return new RuleResult(
					RULE_CODE,
					RuleOutcome.NOT_EVALUATED,
					"warning",
					path,
					actual,
					"Bundle-local Patient reference using Patient/{id} or entry.fullUrl.",
					"External HTTP references are outside the MVP reference resolver boundary.",
					"請改用 Bundle 內 Patient reference，或在後續版本接上外部 FHIR Server 查詢。"
			);
		}
		if (patientReferences.contains(actual)) {
			return new RuleResult(
					RULE_CODE,
					RuleOutcome.PASS,
					"information",
					path,
					actual,
					"Observation.subject points to a Patient in the same Bundle.",
					"Matched Bundle Patient by logical reference or fullUrl.",
					"無需修正。"
			);
		}
		return fail(path, actual, "Observation.subject.reference does not match any Patient in this Bundle.");
	}

	private RuleResult fail(String path, String actual, String evidence) {
		return new RuleResult(
				RULE_CODE,
				RuleOutcome.FAIL,
				"error",
				path,
				actual,
				"Observation.subject must point to a Patient in the same Bundle.",
				evidence,
				"請確認 Observation.subject.reference 指向 Bundle.entry 內存在的 Patient。"
		);
	}

	private RuleResult notApplicable() {
		return new RuleResult(
				RULE_CODE,
				RuleOutcome.NOT_APPLICABLE,
				"information",
				"Bundle.entry",
				"N/A",
				"At least one Observation is required to evaluate LAB-REF-001.",
				"No Observation resource found in this Bundle.",
				"無需修正；此規則不適用。"
		);
	}

	private String idOrUnknown(Observation observation) {
		return BundleReferenceIndex.idOrUnknown(observation);
	}
}
