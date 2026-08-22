package com.twlab.qualitygate.validation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExchangeContract(
		String id,
		String name,
		String version,
		String status,
		String publisher,
		String jurisdiction,
		String effectiveDate,
		String retireDate,
		String fhirVersion,
		List<PolicyAssertion> policyAssertions,
		TerminologyPolicy terminologyPolicy,
		Set<String> allowedLoincCodes,
		Set<String> allowedUcumCodes
) {

	public ExchangeContract {
		policyAssertions = copyOrEmpty(policyAssertions);
		allowedLoincCodes = copyOrEmpty(allowedLoincCodes);
		allowedUcumCodes = copyOrEmpty(allowedUcumCodes);
	}

	public boolean enables(String ruleCode) {
		return policyAssertions.stream()
				.anyMatch(assertion -> assertion.enabled() && ruleCode.equals(assertion.id()));
	}

	public boolean blocksExchange(String ruleCode) {
		return policyAssertions.stream()
				.filter(assertion -> assertion.enabled() && ruleCode.equals(assertion.id()))
				.anyMatch(PolicyAssertion::blocksExchange);
	}

	public String severityFor(String ruleCode, String fallbackSeverity) {
		return policyAssertions.stream()
				.filter(assertion -> assertion.enabled() && ruleCode.equals(assertion.id()))
				.map(PolicyAssertion::severity)
				.filter(severity -> severity != null && !severity.isBlank())
				.findFirst()
				.orElse(fallbackSeverity);
	}

	public Set<String> enabledPolicyAssertionIds() {
		return policyAssertions.stream()
				.filter(PolicyAssertion::enabled)
				.map(PolicyAssertion::id)
				.collect(Collectors.toUnmodifiableSet());
	}

	public String displayName() {
		return id + "#" + version;
	}

	private static List<PolicyAssertion> copyOrEmpty(List<PolicyAssertion> values) {
		if (values == null) {
			return List.of();
		}
		return List.copyOf(values);
	}

	private static Set<String> copyOrEmpty(Set<String> values) {
		if (values == null) {
			return Set.of();
		}
		return Set.copyOf(values);
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record PolicyAssertion(
			String id,
			String source,
			String requirement,
			String obligation,
			String severity,
			boolean enabled
	) {
		public boolean blocksExchange() {
			return "SHALL".equalsIgnoreCase(obligation)
					&& ("error".equalsIgnoreCase(severity) || "fatal".equalsIgnoreCase(severity));
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record TerminologyPolicy(
			String loincSystem,
			String loincVersion,
			String loincValueSetCanonical,
			String ucumSystem,
			String ucumVersion,
			String ucumValueSetCanonical,
			String expansionTimestamp,
			String scope
	) {
	}
}
