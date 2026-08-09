package com.twlab.qualitygate.validation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;

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

		Set<String> observationReferences = collectObservationReferences(bundle);
		List<RuleResult> results = new ArrayList<>();
		for (DiagnosticReport report : reports) {
			results.addAll(validateReport(report, observationReferences));
		}
		return results;
	}

	private Set<String> collectObservationReferences(Bundle bundle) {
		Set<String> observationReferences = new HashSet<>();
		for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
			Resource resource = entry.getResource();
			if (resource instanceof Observation observation) {
				String id = observation.getIdElement().getIdPart();
				if (id != null && !id.isBlank()) {
					observationReferences.add("Observation/" + id);
				}
				String fullUrl = entry.getFullUrl();
				if (fullUrl != null && !fullUrl.isBlank()) {
					observationReferences.add(fullUrl);
				}
			}
		}
		return observationReferences;
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
		if (isExternalReference(actual)) {
			return new RuleResult(
					RULE_CODE,
					RuleOutcome.NOT_EVALUATED,
					"warning",
					path,
					actual,
					"Bundle-local Observation reference using Observation/{id} or entry.fullUrl.",
					"External HTTP references are outside the MVP reference resolver boundary.",
					"請改用 Bundle 內 Observation reference，或在後續版本接上外部 FHIR Server 查詢。"
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
					"無需修正。"
			);
		}
		return fail(path, actual, "DiagnosticReport.result.reference does not match any Observation in this Bundle.");
	}

	private boolean isExternalReference(String reference) {
		return reference.startsWith("http://") || reference.startsWith("https://");
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
				"請確認 DiagnosticReport.result.reference 指向 Bundle.entry 內存在的 Observation。"
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
				"無需修正；此規則不適用。"
		);
	}

	private String idOrUnknown(DiagnosticReport report) {
		String id = report.getIdElement().getIdPart();
		return id == null || id.isBlank() ? "UNKNOWN" : id;
	}
}
