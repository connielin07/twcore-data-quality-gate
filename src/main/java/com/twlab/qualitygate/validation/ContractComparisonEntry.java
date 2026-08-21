package com.twlab.qualitygate.validation;

public record ContractComparisonEntry(
		ExchangeContract contract,
		ValidationResult result
) {
}
