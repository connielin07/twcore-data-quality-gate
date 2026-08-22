package com.twlab.qualitygate.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExchangeContractServiceTests {

	private final ExchangeContractService service = new ExchangeContractService(
			new ObjectMapper(),
			contractRules()
	);

	@Test
	void loadsBundledV1AndV11Contracts() {
		ExchangeContract v1 = service.v1Contract();
		ExchangeContract v11 = service.v1_1Contract();

		assertThat(v1.id()).isEqualTo("demo-lab-hospital-a");
		assertThat(v1.version()).isEqualTo("1.0");
		assertThat(v1.status()).isEqualTo("active");
		assertThat(v1.publisher()).isEqualTo("Demo Lab Integration Office");
		assertThat(v1.jurisdiction()).isEqualTo("TW");
		assertThat(v1.effectiveDate()).isEqualTo("2026-08-01");
		assertThat(v1.fhirVersion()).isEqualTo("4.0.1");
		assertThat(v1.enables(LabUnit002ObservationUcumCodeRule.RULE_CODE)).isFalse();
		assertThat(v1.blocksExchange(LabRef001ObservationSubjectRule.RULE_CODE)).isTrue();
		assertThat(v1.terminologyPolicy().loincValueSetCanonical())
				.isEqualTo("https://example.org/fhir/ValueSet/demo-lab-hospital-a-lab-codes|1.0");
		assertThat(v1.allowedLoincCodes()).containsExactlyInAnyOrder("2345-7", "718-7");
		assertThat(v1.allowedUcumCodes()).containsExactlyInAnyOrder("mg/dL", "mmol/L");

		assertThat(v11.id()).isEqualTo("demo-lab-hospital-a");
		assertThat(v11.version()).isEqualTo("1.1");
		assertThat(v11.effectiveDate()).isEqualTo("2026-08-21");
		assertThat(v11.enables(LabUnit002ObservationUcumCodeRule.RULE_CODE)).isTrue();
		assertThat(v11.blocksExchange(LabUnit002ObservationUcumCodeRule.RULE_CODE)).isTrue();
		assertThat(v11.severityFor(LabUnit002ObservationUcumCodeRule.RULE_CODE, "information")).isEqualTo("error");
		assertThat(v11.terminologyPolicy().expansionTimestamp()).isEqualTo("2026-08-21T00:00:00+08:00");
		assertThat(v11.allowedLoincCodes()).containsExactlyInAnyOrder("2345-7", "718-7");
		assertThat(v11.allowedUcumCodes()).containsExactlyInAnyOrder("mg/dL", "mmol/L");
	}

	@Test
	void defaultContractIsV11() {
		assertThat(service.defaultContract()).isEqualTo(service.v1_1Contract());
	}

	private List<ContractRule> contractRules() {
		return List.of(
				new LabRef001ObservationSubjectRule(),
				new LabRef002DiagnosticReportResultRule(),
				new LabRef003ReportObservationPatientRule(),
				new LabCode001ObservationLoincRule(),
				new LabUnit001ObservationQuantityUnitRule(),
				new LabUnit002ObservationUcumCodeRule()
		);
	}
}
