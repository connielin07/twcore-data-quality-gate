package com.twlab.qualitygate.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.stereotype.Component;

@Component
public class LabRef002DiagnosticReportResultRule implements ContractRule {

	public static final String RULE_CODE = "LAB-REF-002";

	@Override
	public String ruleCode() {
		return RULE_CODE;
	}

	@Override
	public List<RuleResult> validate(Bundle bundle) {
		List<DiagnosticReport> reports = bundle.getEntry().stream()
				.map(Bundle.BundleEntryComponent::getResource)
				.filter(DiagnosticReport.class::isInstance)
				.map(DiagnosticReport.class::cast)
				.toList();
		if (reports.isEmpty()) {
			return List.of(notApplicable());
		}

		Set<String> observationReferences = BundleReferenceIndex.referencesTo(bundle, Observation.class, "Observation");
		List<RuleResult> results = new ArrayList<>();
		for (DiagnosticReport report : reports) {
			results.addAll(validateReport(report, observationReferences));
		}
		return results;
	}

	private List<RuleResult> validateReport(
			DiagnosticReport report,
			Set<String> observationReferences
	) {
		if (!report.hasResult()) {
			String path = "DiagnosticReport/" + idOrUnknown(report) + ".result";
			return List.of(fail(path, "N/A", "DiagnosticReport.result is required for LAB-REF-002."));
		}

		List<RuleResult> results = new ArrayList<>();
		List<Reference> reportResults = report.getResult();
		for (int i = 0; i < reportResults.size(); i++) {
			Reference reference = reportResults.get(i);
			String actual = reference.getReference();
			String path = "DiagnosticReport/" + idOrUnknown(report) + ".result[" + i + "].reference";
			results.add(validateReference(path, actual, observationReferences));
		}
		return results;
	}

	private RuleResult validateReference(String path, String actual, Set<String> observationReferences) {
		if (actual == null || actual.isBlank()) {
			return fail(path, "N/A", "DiagnosticReport.result.reference is required for LAB-REF-002.");
		}
		if (BundleReferenceIndex.isExternalReference(actual)) {
			return new RuleResult(
					RULE_CODE,
					RuleOutcome.NOT_EVALUATED,
					"warning",
					path,
					actual,
					"Bundle-local Observation reference using Observation/{id} or entry.fullUrl.",
					"External HTTP references are outside the MVP reference resolver boundary.",
					"Use a Bundle-local Observation reference, or add external FHIR Server lookup in a later version."
			);
		}
		if (observationReferences.contains(actual)) {
			return new RuleResult(
					RULE_CODE,
					RuleOutcome.PASS,
					"information",
					path,
					actual,
					"DiagnosticReport.result points to an Observation in the same Bundle.",
					"Matched Bundle Observation by logical reference or fullUrl.",
					"No fix needed."
			);
		}
		return fail(path, actual, "DiagnosticReport.result.reference does not match any Observation in this Bundle.");
	}

	private RuleResult fail(String path, String actual, String evidence) {
		return new RuleResult(
				RULE_CODE,
				RuleOutcome.FAIL,
				"error",
				path,
				actual,
				"DiagnosticReport.result must point to an Observation in the same Bundle.",
				evidence,
				"Make DiagnosticReport.result.reference point to an existing Observation in Bundle.entry."
		);
	}

	private RuleResult notApplicable() {
		return new RuleResult(
				RULE_CODE,
				RuleOutcome.NOT_APPLICABLE,
				"information",
				"Bundle.entry",
				"N/A",
				"At least one DiagnosticReport is required to evaluate LAB-REF-002.",
				"No DiagnosticReport resource found in this Bundle.",
				"No fix needed; this rule does not apply."
		);
	}

	private String idOrUnknown(DiagnosticReport report) {
		return BundleReferenceIndex.idOrUnknown(report);
	}
}
