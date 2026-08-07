package com.twlab.qualitygate.validation;

import java.util.List;

public record TwCoreProfileValidationResult(
		ParseStatus status,
		String message,
		List<OperationOutcomeIssue> operationOutcomeIssues
) {
	public static TwCoreProfileValidationResult passed(String message, List<OperationOutcomeIssue> issues) {
		return new TwCoreProfileValidationResult(ParseStatus.PASSED, message, issues);
	}

	public static TwCoreProfileValidationResult failed(String message, List<OperationOutcomeIssue> issues) {
		return new TwCoreProfileValidationResult(ParseStatus.FAILED, message, issues);
	}

	public static TwCoreProfileValidationResult notEvaluated(String message, List<OperationOutcomeIssue> issues) {
		return new TwCoreProfileValidationResult(ParseStatus.NOT_EVALUATED, message, issues);
	}
}
