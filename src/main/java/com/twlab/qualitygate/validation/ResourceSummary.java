package com.twlab.qualitygate.validation;

import java.util.List;

public record ResourceSummary(
		long patientCount,
		long observationCount,
		long diagnosticReportCount,
		long notEvaluatedCount
) {
	public static ResourceSummary empty() {
		return new ResourceSummary(0, 0, 0, 0);
	}

	public static ResourceSummary fromEntries(List<BundleEntrySummary> entries) {
		return new ResourceSummary(
				count(entries, "Patient"),
				count(entries, "Observation"),
				count(entries, "DiagnosticReport"),
				entries.stream()
						.filter(entry -> "NOT_EVALUATED".equals(entry.evaluationStatus()))
						.count()
		);
	}

	private static long count(List<BundleEntrySummary> entries, String resourceType) {
		return entries.stream()
				.filter(entry -> resourceType.equals(entry.resourceType()))
				.count();
	}
}
