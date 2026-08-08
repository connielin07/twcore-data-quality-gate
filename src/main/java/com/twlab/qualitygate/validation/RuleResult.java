package com.twlab.qualitygate.validation;

public record RuleResult(
		String ruleCode,
		RuleOutcome outcome,
		String severity,
		String path,
		String actual,
		String expected,
		String evidence,
		String suggestion
) {
}
