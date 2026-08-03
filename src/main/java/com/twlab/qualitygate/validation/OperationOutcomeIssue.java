package com.twlab.qualitygate.validation;

public record OperationOutcomeIssue(
		String severity,
		String location,
		String diagnostics
) {
}
