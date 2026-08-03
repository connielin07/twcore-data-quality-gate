package com.twlab.qualitygate.validation;

import java.util.List;

public record ValidationResult(
		ParseStatus jsonStatus,
		ParseStatus fhirR4Status,
		ParseStatus resourceTypeStatus,
		ParseStatus fhirValidationStatus,
		List<OperationOutcomeIssue> operationOutcomeIssues,
		Integer resourceCount,
		String resourceType,
		String errorMessage
) {
	public static ValidationResult empty() {
		return new ValidationResult(null, null, null, null, List.of(), null, null, null);
	}
}
