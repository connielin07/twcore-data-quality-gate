package com.twlab.qualitygate.validation;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;

public record BundleEntrySummary(
		String resourceType,
		String id,
		String fullUrl,
		String evaluationStatus
) {
	private static final String NOT_AVAILABLE = "N/A";

	public static BundleEntrySummary fromEntry(Bundle.BundleEntryComponent entry) {
		Resource resource = entry.getResource();
		String resourceType = resource == null ? "MISSING_RESOURCE" : resource.fhirType();
		String id = resource == null ? NOT_AVAILABLE : valueOrDefault(resource.getIdElement().getIdPart());
		String fullUrl = valueOrDefault(entry.getFullUrl());
		String evaluationStatus = isMvpResource(resourceType) ? "SUPPORTED" : "NOT_EVALUATED";
		return new BundleEntrySummary(resourceType, id, fullUrl, evaluationStatus);
	}

	private static boolean isMvpResource(String resourceType) {
		return "Patient".equals(resourceType)
				|| "Observation".equals(resourceType)
				|| "DiagnosticReport".equals(resourceType);
	}

	private static String valueOrDefault(String value) {
		return value == null || value.isBlank() ? NOT_AVAILABLE : value;
	}
}
