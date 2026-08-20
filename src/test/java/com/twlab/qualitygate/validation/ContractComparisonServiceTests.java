package com.twlab.qualitygate.validation;

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twlab.qualitygate.config.FhirConfig;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class ContractComparisonServiceTests {

	private final FhirConfig fhirConfig = new FhirConfig();
	private final FhirContext fhirContext = FhirContext.forR4Cached();
	private final BundleParseService bundleParseService = new BundleParseService(
			new ObjectMapper(),
			fhirContext,
			fhirConfig.fhirValidator(fhirContext),
			new TwCoreValidationService(
					() -> TwCorePackageProbeResult.loaded("TW Core package loading probe succeeded: test package loaded."),
					bundle -> TwCoreProfileValidationResult.passed("Test profile validation passed.", List.of())
			),
			contractRules()
	);
	private final ContractComparisonService service = new ContractComparisonService(bundleParseService);

	@Test
	void comparesV1AndV11WhenV11AddsUcumSystemCodeRequirement() {
		ContractComparisonResult comparison = service.compare(fixture("observation-quantity-wrong-ucum-system.json"));

		assertThat(comparison.v1Result().gateOutcome()).isEqualTo(GateOutcome.PASSED);
		assertThat(comparison.v1Result().contractRuleResults())
				.extracting(RuleResult::ruleCode)
				.doesNotContain(LabUnit002ObservationUcumCodeRule.RULE_CODE);

		assertThat(comparison.v1_1Result().gateOutcome()).isEqualTo(GateOutcome.BLOCKED);
		assertThat(comparison.v1_1Result().contractRuleResults())
				.anySatisfy(ruleResult -> {
					assertThat(ruleResult.ruleCode()).isEqualTo(LabUnit002ObservationUcumCodeRule.RULE_CODE);
					assertThat(ruleResult.outcome()).isEqualTo(RuleOutcome.FAIL);
				});
	}

	private String fixture(String name) {
		try (var input = getClass().getResourceAsStream("/cases/" + name)) {
			if (input == null) {
				throw new IllegalArgumentException("Missing fixture: " + name);
			}
			return new String(input.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
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
