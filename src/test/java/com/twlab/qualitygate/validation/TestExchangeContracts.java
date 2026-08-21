package com.twlab.qualitygate.validation;

import java.util.Set;

final class TestExchangeContracts {

	static final ExchangeContract DEMO_V11 = new ExchangeContract(
			"demo-lab-hospital-a",
			"Demo Lab to Hospital A Exchange Contract",
			"1.1",
			Set.of(
					LabRef001ObservationSubjectRule.RULE_CODE,
					LabRef002DiagnosticReportResultRule.RULE_CODE,
					LabRef003ReportObservationPatientRule.RULE_CODE,
					LabCode001ObservationLoincRule.RULE_CODE,
					LabUnit001ObservationQuantityUnitRule.RULE_CODE,
					LabUnit002ObservationUcumCodeRule.RULE_CODE
			),
			Set.of("2345-7", "718-7"),
			Set.of("mg/dL", "mmol/L")
	);

	private TestExchangeContracts() {
	}
}
