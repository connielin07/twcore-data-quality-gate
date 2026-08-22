package com.twlab.qualitygate.validation;

import java.util.List;
import java.util.Set;

final class TestExchangeContracts {

	static final ExchangeContract DEMO_V11 = new ExchangeContract(
			"demo-lab-hospital-a",
			"Demo Lab to Hospital A Exchange Contract",
			"1.1",
			"active",
			"Demo Lab Integration Office",
			"TW",
			"2026-08-21",
			null,
			"4.0.1",
			List.of(
					policyAssertion(LabRef001ObservationSubjectRule.RULE_CODE),
					policyAssertion(LabRef002DiagnosticReportResultRule.RULE_CODE),
					policyAssertion(LabRef003ReportObservationPatientRule.RULE_CODE),
					policyAssertion(LabCode001ObservationLoincRule.RULE_CODE),
					policyAssertion(LabUnit001ObservationQuantityUnitRule.RULE_CODE),
					policyAssertion(LabUnit002ObservationUcumCodeRule.RULE_CODE)
			),
			new ExchangeContract.TerminologyPolicy(
					"http://loinc.org",
					"2.78",
					"https://example.org/fhir/ValueSet/demo-lab-hospital-a-lab-codes|1.1",
					"http://unitsofmeasure.org",
					"2.1",
					"https://example.org/fhir/ValueSet/demo-lab-hospital-a-lab-units|1.1",
					"2026-08-21T00:00:00+08:00",
					"Test local expansion snapshot."
			),
			Set.of("2345-7", "718-7"),
			Set.of("mg/dL", "mmol/L")
	);

	private static ExchangeContract.PolicyAssertion policyAssertion(String id) {
		return new ExchangeContract.PolicyAssertion(
				id,
				"test companion guide",
				"Test requirement mapped to " + id + ".",
				"SHALL",
				"error",
				true
		);
	}

	private TestExchangeContracts() {
	}
}
