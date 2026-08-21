package com.twlab.qualitygate.validation;

public record ContractRuleFailureDelta(
		ExchangeContract contract,
		RuleResult ruleResult
) {
}
