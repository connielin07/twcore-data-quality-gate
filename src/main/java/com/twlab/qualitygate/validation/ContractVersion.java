package com.twlab.qualitygate.validation;

import java.util.Set;

public enum ContractVersion {
	V1_0(Set.of(
			LabRef001ObservationSubjectRule.RULE_CODE,
			LabRef002DiagnosticReportResultRule.RULE_CODE,
			LabRef003ReportObservationPatientRule.RULE_CODE,
			LabCode001ObservationLoincRule.RULE_CODE,
			LabUnit001ObservationQuantityUnitRule.RULE_CODE
	)),
	V1_1(Set.of(
			LabRef001ObservationSubjectRule.RULE_CODE,
			LabRef002DiagnosticReportResultRule.RULE_CODE,
			LabRef003ReportObservationPatientRule.RULE_CODE,
			LabCode001ObservationLoincRule.RULE_CODE,
			LabUnit001ObservationQuantityUnitRule.RULE_CODE,
			LabUnit002ObservationUcumCodeRule.RULE_CODE
	));

	private final Set<String> enabledRuleCodes;

	ContractVersion(Set<String> enabledRuleCodes) {
		this.enabledRuleCodes = enabledRuleCodes;
	}

	public boolean enables(String ruleCode) {
		return enabledRuleCodes.contains(ruleCode);
	}
}
