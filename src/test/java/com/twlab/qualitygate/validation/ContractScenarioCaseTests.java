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

class ContractScenarioCaseTests {

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
	private final ContractComparisonService comparisonService = new ContractComparisonService(bundleParseService);

	@Test
	void recordsExpectedGateOutcomeForMinimalScenarioPack() {
		for (ScenarioExpectation scenario : scenarioExpectations()) {
			ContractComparisonResult actual = comparisonService.compare(fixture(scenario.fixtureName()));

			assertThat(actual.v1Result().gateOutcome())
					.as("%s v1.0 gate", scenario.caseName())
					.isEqualTo(scenario.expectedV1Gate());
			assertThat(actual.v1_1Result().gateOutcome())
					.as("%s v1.1 gate", scenario.caseName())
					.isEqualTo(scenario.expectedV1_1Gate());
			assertThat(failedRuleCodes(actual.v1Result()))
					.as("%s v1.0 failed rules", scenario.caseName())
					.containsExactlyElementsOf(scenario.expectedV1FailedRules());
			assertThat(failedRuleCodes(actual.v1_1Result()))
					.as("%s v1.1 failed rules", scenario.caseName())
					.containsExactlyElementsOf(scenario.expectedV1_1FailedRules());
		}
	}

	@Test
	void showsV11UcumUpgradeChangesOnlyTheNewUcumRule() {
		ContractComparisonResult actual = comparisonService.compare(fixture("observation-quantity-wrong-ucum-system.json"));

		assertThat(actual.v1Result().gateOutcome()).isEqualTo(GateOutcome.PASSED);
		assertThat(actual.v1Result().contractRuleResults())
				.extracting(RuleResult::ruleCode)
				.doesNotContain(LabUnit002ObservationUcumCodeRule.RULE_CODE);
		assertThat(failedRuleCodes(actual.v1_1Result()))
				.containsExactly(LabUnit002ObservationUcumCodeRule.RULE_CODE);
	}

	@Test
	void keepsReferenceFailureBlockedInBothVersions() {
		ContractComparisonResult actual = comparisonService.compare(fixture("missing-internal-reference.json"));

		assertThat(actual.v1Result().gateOutcome()).isEqualTo(GateOutcome.BLOCKED);
		assertThat(actual.v1_1Result().gateOutcome()).isEqualTo(GateOutcome.BLOCKED);
		assertThat(failedRuleCodes(actual.v1Result()))
				.contains(LabRef001ObservationSubjectRule.RULE_CODE)
				.doesNotContain(LabUnit002ObservationUcumCodeRule.RULE_CODE);
		assertThat(failedRuleCodes(actual.v1_1Result()))
				.contains(LabRef001ObservationSubjectRule.RULE_CODE)
				.doesNotContain(LabUnit002ObservationUcumCodeRule.RULE_CODE);
	}

	@Test
	void coversNotApplicableWhenObservationValueIsNotQuantity() {
		ContractComparisonResult actual = comparisonService.compare(fixture("non-quantity-observation-bundle.json"));

		assertThat(actual.v1_1Result().gateOutcome()).isEqualTo(GateOutcome.PASSED);
		assertRuleOutcome(
				actual.v1_1Result(),
				LabUnit001ObservationQuantityUnitRule.RULE_CODE,
				RuleOutcome.NOT_APPLICABLE
		);
		assertRuleOutcome(
				actual.v1_1Result(),
				LabUnit002ObservationUcumCodeRule.RULE_CODE,
				RuleOutcome.NOT_APPLICABLE
		);
	}

	private List<ScenarioExpectation> scenarioExpectations() {
		return List.of(
				new ScenarioExpectation(
						"valid minimal lab Bundle",
						"valid-minimal-lab-bundle.json",
						GateOutcome.PASSED,
						GateOutcome.PASSED,
						List.of(),
						List.of()
				),
				new ScenarioExpectation(
						"v1.1 UCUM requirement upgrade",
						"observation-quantity-wrong-ucum-system.json",
						GateOutcome.PASSED,
						GateOutcome.BLOCKED,
						List.of(),
						List.of(LabUnit002ObservationUcumCodeRule.RULE_CODE)
				),
				new ScenarioExpectation(
						"missing internal patient reference",
						"missing-internal-reference.json",
						GateOutcome.BLOCKED,
						GateOutcome.BLOCKED,
						List.of(
								LabRef001ObservationSubjectRule.RULE_CODE,
								LabRef003ReportObservationPatientRule.RULE_CODE
						),
						List.of(
								LabRef001ObservationSubjectRule.RULE_CODE,
								LabRef003ReportObservationPatientRule.RULE_CODE
						)
				),
				new ScenarioExpectation(
						"non-quantity Observation",
						"non-quantity-observation-bundle.json",
						GateOutcome.PASSED,
						GateOutcome.PASSED,
						List.of(),
						List.of()
				)
		);
	}

	private List<String> failedRuleCodes(ValidationResult result) {
		return result.contractRuleResults().stream()
				.filter(ruleResult -> ruleResult.outcome() == RuleOutcome.FAIL)
				.map(RuleResult::ruleCode)
				.toList();
	}

	private void assertRuleOutcome(ValidationResult result, String ruleCode, RuleOutcome outcome) {
		assertThat(result.contractRuleResults())
				.anySatisfy(ruleResult -> {
					assertThat(ruleResult.ruleCode()).isEqualTo(ruleCode);
					assertThat(ruleResult.outcome()).isEqualTo(outcome);
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

	private record ScenarioExpectation(
			String caseName,
			String fixtureName,
			GateOutcome expectedV1Gate,
			GateOutcome expectedV1_1Gate,
			List<String> expectedV1FailedRules,
			List<String> expectedV1_1FailedRules
	) {
	}
}
