package com.twlab.qualitygate.validation;

import java.util.List;

public record TwCoreValidationResult(
		ParseStatus status,
		String packageId,
		String packageVersion,
		String message,
		List<OperationOutcomeIssue> operationOutcomeIssues
) {
	public static final String PACKAGE_ID = "tw.gov.mohw.twcore";
	public static final String PACKAGE_VERSION = "1.0.0";

	public static TwCoreValidationResult notEvaluated(String message) {
		return notEvaluated(message, List.of());
	}

	public static TwCoreValidationResult notEvaluated(String message, List<OperationOutcomeIssue> issues) {
		return new TwCoreValidationResult(
				ParseStatus.NOT_EVALUATED,
				PACKAGE_ID,
				PACKAGE_VERSION,
				message,
				issues
		);
	}

	public static TwCoreValidationResult passed(String message, List<OperationOutcomeIssue> issues) {
		return new TwCoreValidationResult(
				ParseStatus.PASSED,
				PACKAGE_ID,
				PACKAGE_VERSION,
				message,
				issues
		);
	}

	public static TwCoreValidationResult failed(String message, List<OperationOutcomeIssue> issues) {
		return new TwCoreValidationResult(
				ParseStatus.FAILED,
				PACKAGE_ID,
				PACKAGE_VERSION,
				message,
				issues
		);
	}
}
