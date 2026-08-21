package com.twlab.qualitygate.validation;

import java.util.Set;

public record ExchangeContract(
		String id,
		String name,
		String version,
		Set<String> enabledRuleCodes,
		Set<String> allowedLoincCodes,
		Set<String> allowedUcumCodes
) {

	public ExchangeContract {
		enabledRuleCodes = copyOrEmpty(enabledRuleCodes);
		allowedLoincCodes = copyOrEmpty(allowedLoincCodes);
		allowedUcumCodes = copyOrEmpty(allowedUcumCodes);
	}

	public boolean enables(String ruleCode) {
		return enabledRuleCodes.contains(ruleCode);
	}

	public String displayName() {
		return id + "#" + version;
	}

	private static Set<String> copyOrEmpty(Set<String> values) {
		if (values == null) {
			return Set.of();
		}
		return Set.copyOf(values);
	}
}
