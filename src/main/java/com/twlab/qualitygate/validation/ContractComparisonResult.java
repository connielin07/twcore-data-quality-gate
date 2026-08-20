package com.twlab.qualitygate.validation;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record ContractComparisonResult(
		ValidationResult v1Result,
		ValidationResult v1_1Result
) {
	public boolean hasGateOutcomeChange() {
		return v1Result.gateOutcome() != v1_1Result.gateOutcome();
	}

	public boolean hasNewV11RuleFailures() {
		return !newlyFailedV11RuleResults().isEmpty();
	}

	public List<RuleResult> newlyFailedV11RuleResults() {
		Set<String> v1FailedRuleCodes = v1Result.contractRuleResults().stream()
				.filter(ruleResult -> ruleResult.outcome() == RuleOutcome.FAIL)
				.map(RuleResult::ruleCode)
				.collect(Collectors.toSet());

		return v1_1Result.contractRuleResults().stream()
				.filter(ruleResult -> ruleResult.outcome() == RuleOutcome.FAIL)
				.filter(ruleResult -> !v1FailedRuleCodes.contains(ruleResult.ruleCode()))
				.toList();
	}
}
