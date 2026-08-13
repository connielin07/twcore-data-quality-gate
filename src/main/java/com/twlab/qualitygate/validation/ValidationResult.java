package com.twlab.qualitygate.validation;

import java.util.List;

public record ValidationResult(
		ParseStatus jsonStatus,
		ParseStatus fhirR4Status,
		ParseStatus resourceTypeStatus,
		ParseStatus fhirValidationStatus,
		TwCoreValidationResult twCoreValidationResult,
		GateOutcome gateOutcome,
		List<OperationOutcomeIssue> operationOutcomeIssues,
		List<RuleResult> contractRuleResults,
		ResourceSummary resourceSummary,
		List<BundleEntrySummary> bundleEntrySummaries,
		Integer resourceCount,
		String resourceType,
		String errorMessage
) {
	public ValidationResult(
			ParseStatus jsonStatus,
			ParseStatus fhirR4Status,
			ParseStatus resourceTypeStatus,
			ParseStatus fhirValidationStatus,
			TwCoreValidationResult twCoreValidationResult,
			List<OperationOutcomeIssue> operationOutcomeIssues,
			ResourceSummary resourceSummary,
			List<BundleEntrySummary> bundleEntrySummaries,
			Integer resourceCount,
			String resourceType,
			String errorMessage
	) {
		this(
				jsonStatus,
				fhirR4Status,
				resourceTypeStatus,
				fhirValidationStatus,
				twCoreValidationResult,
				GateOutcome.BLOCKED,
				operationOutcomeIssues,
				List.of(),
				resourceSummary,
				bundleEntrySummaries,
				resourceCount,
				resourceType,
				errorMessage
		);
	}

	public static ValidationResult empty() {
		return new ValidationResult(
				null,
				null,
				null,
				null,
				TwCoreValidationResult.notEvaluated("尚未執行驗證。"),
				GateOutcome.BLOCKED,
				List.of(),
				List.of(),
				ResourceSummary.empty(),
				List.of(),
				null,
				null,
				null
		);
	}
}
