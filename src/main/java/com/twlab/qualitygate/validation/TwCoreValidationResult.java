package com.twlab.qualitygate.validation;

public record TwCoreValidationResult(
		ParseStatus status,
		String packageId,
		String packageVersion,
		String message
) {
	public static final String PACKAGE_ID = "tw.gov.mohw.twcore";
	public static final String PACKAGE_VERSION = "1.0.0";

	public static TwCoreValidationResult notEvaluated(String message) {
		return new TwCoreValidationResult(
				ParseStatus.NOT_EVALUATED,
				PACKAGE_ID,
				PACKAGE_VERSION,
				message
		);
	}
}
