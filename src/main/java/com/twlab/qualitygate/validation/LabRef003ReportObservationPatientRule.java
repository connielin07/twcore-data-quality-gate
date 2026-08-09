package com.twlab.qualitygate.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;

public class LabRef003ReportObservationPatientRule implements ContractRule {

	public static final String RULE_CODE = "LAB-REF-003";

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

		Map<String, Observation> observationsByReference = collectObservationsByReference(bundle);
		Map<String, String> patientReferenceAliases = collectPatientReferenceAliases(bundle);
		List<RuleResult> results = new ArrayList<>();
		for (DiagnosticReport report : reports) {
			results.addAll(validateReport(report, observationsByReference, patientReferenceAliases));
		}
		return results;
	}

	private Map<String, Observation> collectObservationsByReference(Bundle bundle) {
		Map<String, Observation> observationsByReference = new HashMap<>();
		for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
			Resource resource = entry.getResource();
			if (resource instanceof Observation observation) {
				String id = observation.getIdElement().getIdPart();
				if (id != null && !id.isBlank()) {
					observationsByReference.put("Observation/" + id, observation);
				}
				String fullUrl = entry.getFullUrl();
				if (fullUrl != null && !fullUrl.isBlank()) {
					observationsByReference.put(fullUrl, observation);
				}
			}
		}
		return observationsByReference;
	}

	private Map<String, String> collectPatientReferenceAliases(Bundle bundle) {
		Map<String, String> patientReferenceAliases = new HashMap<>();
		for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
			Resource resource = entry.getResource();
			if (resource != null && "Patient".equals(resource.fhirType())) {
				String id = resource.getIdElement().getIdPart();
				String canonical = id == null || id.isBlank() ? entry.getFullUrl() : "Patient/" + id;
				if (canonical == null || canonical.isBlank()) {
					continue;
				}
				patientReferenceAliases.put(canonical, canonical);
				String fullUrl = entry.getFullUrl();
				if (fullUrl != null && !fullUrl.isBlank()) {
					patientReferenceAliases.put(fullUrl, canonical);
				}
			}
		}
		return patientReferenceAliases;
	}

	private List<RuleResult> validateReport(
			DiagnosticReport report,
			Map<String, Observation> observationsByReference,
			Map<String, String> patientReferenceAliases
	) {
		String reportSubject = report.hasSubject() ? report.getSubject().getReference() : null;
		String reportSubjectPath = "DiagnosticReport/" + idOrUnknown(report) + ".subject.reference";
		if (reportSubject == null || reportSubject.isBlank()) {
			return List.of(fail(
					reportSubjectPath,
					"N/A",
					"DiagnosticReport.subject.reference is required for LAB-REF-003."
			));
		}
		if (isExternalReference(reportSubject)) {
			return List.of(notEvaluated(
					reportSubjectPath,
					reportSubject,
					"External HTTP DiagnosticReport.subject references are outside the MVP reference resolver boundary."
			));
		}
		if (!report.hasResult()) {
			String path = "DiagnosticReport/" + idOrUnknown(report) + ".result";
			return List.of(fail(path, "N/A", "DiagnosticReport.result is required for LAB-REF-003."));
		}

		List<RuleResult> results = new ArrayList<>();
		List<Reference> reportResults = report.getResult();
		for (int i = 0; i < reportResults.size(); i++) {
			String path = "DiagnosticReport/" + idOrUnknown(report) + ".result[" + i + "].reference";
			String observationReference = reportResults.get(i).getReference();
			results.add(validateResultReference(
					path,
					reportSubject,
					observationReference,
					observationsByReference,
					patientReferenceAliases
			));
		}
		return results;
	}

	private RuleResult validateResultReference(
			String path,
			String reportSubject,
			String observationReference,
			Map<String, Observation> observationsByReference,
			Map<String, String> patientReferenceAliases
	) {
		if (observationReference == null || observationReference.isBlank()) {
			return fail(path, "N/A", "DiagnosticReport.result.reference is required for LAB-REF-003.");
		}
		if (isExternalReference(observationReference)) {
			return notEvaluated(
					path,
					observationReference,
					"External HTTP DiagnosticReport.result references are outside the MVP reference resolver boundary."
			);
		}

		Observation observation = observationsByReference.get(observationReference);
		if (observation == null) {
			return fail(path, observationReference, "DiagnosticReport.result.reference does not match any Observation in this Bundle.");
		}

		String observationSubject = observation.hasSubject() ? observation.getSubject().getReference() : null;
		if (observationSubject == null || observationSubject.isBlank()) {
			return fail(path, actual(reportSubject, "N/A"), "Referenced Observation.subject.reference is required for LAB-REF-003.");
		}
		if (isExternalReference(observationSubject)) {
			return notEvaluated(
					path,
					observationSubject,
					"External HTTP Observation.subject references are outside the MVP reference resolver boundary."
			);
		}

		String canonicalReportSubject = patientReferenceAliases.getOrDefault(reportSubject, reportSubject);
		String canonicalObservationSubject = patientReferenceAliases.getOrDefault(observationSubject, observationSubject);
		String actual = actual(reportSubject, observationSubject);
		if (canonicalReportSubject.equals(canonicalObservationSubject)) {
			return new RuleResult(
					RULE_CODE,
					RuleOutcome.PASS,
					"information",
					path,
					actual,
					"DiagnosticReport.subject and referenced Observation.subject point to the same Patient.",
					"Matched Patient reference by logical reference or fullUrl.",
					"無需修正。"
			);
		}

		return fail(
				path,
				actual,
				"DiagnosticReport.subject and referenced Observation.subject point to different Patients."
		);
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
				"DiagnosticReport.subject and referenced Observation.subject must identify the same Patient.",
				evidence,
				"請確認 DiagnosticReport.subject.reference 與 DiagnosticReport.result 指向的 Observation.subject.reference 為同一位 Patient。"
		);
	}

	private RuleResult notEvaluated(String path, String actual, String evidence) {
		return new RuleResult(
				RULE_CODE,
				RuleOutcome.NOT_EVALUATED,
				"warning",
				path,
				actual,
				"Bundle-local Patient and Observation references using Resource/{id} or entry.fullUrl.",
				evidence,
				"請改用 Bundle 內 reference，或在後續版本接上外部 FHIR Server 查詢。"
		);
	}

	private RuleResult notApplicable() {
		return new RuleResult(
				RULE_CODE,
				RuleOutcome.NOT_APPLICABLE,
				"information",
				"Bundle.entry",
				"N/A",
				"At least one DiagnosticReport is required to evaluate LAB-REF-003.",
				"No DiagnosticReport resource found in this Bundle.",
				"無需修正；此規則不適用。"
		);
	}

	private String actual(String reportSubject, String observationSubject) {
		return "DiagnosticReport.subject=" + reportSubject + "; Observation.subject=" + observationSubject;
	}

	private String idOrUnknown(DiagnosticReport report) {
		String id = report.getIdElement().getIdPart();
		return id == null || id.isBlank() ? "UNKNOWN" : id;
	}
}
