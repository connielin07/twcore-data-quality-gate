package com.twlab.qualitygate.validation;

public record ContractComparisonResult(
		ValidationResult v1Result,
		ValidationResult v1_1Result
) {
}
