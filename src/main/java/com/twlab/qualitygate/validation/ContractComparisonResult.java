package com.twlab.qualitygate.validation;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record ContractComparisonResult(
		List<ContractComparisonEntry> entries
) {
	public ContractComparisonResult {
		if (entries.size() < 2) {
			throw new IllegalArgumentException("Contract comparison requires at least two contract versions.");
		}
		entries = List.copyOf(entries);
	}

	public ValidationResult v1Result() {
		return entries.get(0).result();
	}

	public ValidationResult v1_1Result() {
		return entries.get(entries.size() - 1).result();
	}

	public boolean hasGateOutcomeChange() {
		GateOutcome baseline = entries.get(0).result().gateOutcome();
		return entries.stream()
				.map(ContractComparisonEntry::result)
				.anyMatch(result -> result.gateOutcome() != baseline);
	}

	public boolean hasNewRuleFailures() {
		return !newlyFailedRuleResults().isEmpty();
	}

	public List<ContractRuleFailureDelta> newlyFailedRuleResults() {
		Set<String> baselineFailedRuleCodes = failedRuleCodes(entries.get(0).result());
		return entries.stream()
				.skip(1)
				.flatMap(entry -> entry.result().contractRuleResults().stream()
						.filter(ruleResult -> ruleResult.outcome() == RuleOutcome.FAIL)
						.filter(ruleResult -> !baselineFailedRuleCodes.contains(ruleResult.ruleCode()))
						.map(ruleResult -> new ContractRuleFailureDelta(entry.contract(), ruleResult)))
				.toList();
	}

	public boolean hasNewV11RuleFailures() {
		return hasNewRuleFailures();
	}

	public List<RuleResult> newlyFailedV11RuleResults() {
		Set<String> baselineFailedRuleCodes = failedRuleCodes(entries.get(0).result());
		return entries.get(entries.size() - 1).result().contractRuleResults().stream()
				.filter(ruleResult -> ruleResult.outcome() == RuleOutcome.FAIL)
				.filter(ruleResult -> !baselineFailedRuleCodes.contains(ruleResult.ruleCode()))
				.toList();
	}

	private Set<String> failedRuleCodes(ValidationResult result) {
		return result.contractRuleResults().stream()
				.filter(ruleResult -> ruleResult.outcome() == RuleOutcome.FAIL)
				.map(RuleResult::ruleCode)
				.collect(Collectors.toSet());
	}
}
