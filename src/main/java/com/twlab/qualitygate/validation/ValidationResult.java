package com.twlab.qualitygate.validation;

public record ValidationResult(
		ParseStatus jsonStatus,
		ParseStatus fhirR4Status,
		ParseStatus resourceTypeStatus,
		Integer resourceCount,
		String resourceType,
		String errorMessage
) {
	public static ValidationResult empty() {
		return new ValidationResult(null, null, null, null, null, null);
	}
}
