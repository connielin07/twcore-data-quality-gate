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

class BundleParseServiceTests {

	private final FhirConfig fhirConfig = new FhirConfig();
	private final FhirContext fhirContext = FhirContext.forR4Cached();
	private final CountingTwCorePackageProbe packageProbe = new CountingTwCorePackageProbe(
			TwCorePackageProbeResult.loaded("TW Core package loading probe 成功：test package loaded。")
	);
	private final StubTwCoreProfileValidator profileValidator = new StubTwCoreProfileValidator(
			TwCoreProfileValidationResult.passed("Day 6 test profile validation passed。", List.of())
	);
	private final BundleParseService service = new BundleParseService(
			new ObjectMapper(),
			fhirContext,
			fhirConfig.fhirValidator(fhirContext),
			new TwCoreValidationService(packageProbe, profileValidator),
			contractRules()
	);

	@Test
	void parsesBundleJson() {
		ValidationResult result = service.parse(fixture("valid-minimal-lab-bundle.json"));

		assertThat(result.jsonStatus()).isEqualTo(ParseStatus.PASSED);
		assertThat(result.fhirR4Status()).isEqualTo(ParseStatus.PASSED);
		assertThat(result.resourceTypeStatus()).isEqualTo(ParseStatus.PASSED);
		assertThat(result.fhirValidationStatus()).isEqualTo(ParseStatus.PASSED);
		assertThat(result.operationOutcomeIssues())
				.noneSatisfy(issue -> assertThat(issue.severity()).isIn("error", "fatal"));
		assertThat(result.resourceType()).isEqualTo("Bundle");
		assertThat(result.resourceCount()).isEqualTo(3);
		assertThat(result.resourceSummary().patientCount()).isEqualTo(1);
		assertThat(result.resourceSummary().observationCount()).isEqualTo(1);
		assertThat(result.resourceSummary().diagnosticReportCount()).isEqualTo(1);
		assertThat(result.resourceSummary().notEvaluatedCount()).isZero();
		assertThat(result.twCoreValidationResult().status()).isEqualTo(ParseStatus.PASSED);
		assertThat(result.twCoreValidationResult().packageId()).isEqualTo("tw.gov.mohw.twcore");
		assertThat(result.twCoreValidationResult().packageVersion()).isEqualTo("1.0.0");
		assertThat(result.twCoreValidationResult().message())
				.contains("package loading probe 成功")
				.contains("Day 6 test profile validation passed");
		assertThat(result.twCoreValidationResult().operationOutcomeIssues()).isEmpty();
		assertThat(result.gateOutcome()).isEqualTo(GateOutcome.PASSED);
		assertThat(result.contractRuleResults())
				.extracting(RuleResult::ruleCode)
				.containsExactly(
						LabRef001ObservationSubjectRule.RULE_CODE,
						LabRef002DiagnosticReportResultRule.RULE_CODE,
						LabRef003ReportObservationPatientRule.RULE_CODE,
						LabCode001ObservationLoincRule.RULE_CODE,
						LabUnit001ObservationQuantityUnitRule.RULE_CODE,
						LabUnit002ObservationUcumCodeRule.RULE_CODE
				);
		assertThat(result.contractRuleResults())
				.extracting(RuleResult::outcome)
				.containsOnly(RuleOutcome.PASS);
		assertThat(result.errorMessage()).isNull();
	}

	@Test
	void blocksGateWhenContractReferenceRuleFails() {
		ValidationResult result = service.parse(fixture("missing-internal-reference.json"));

		assertThat(result.gateOutcome()).isEqualTo(GateOutcome.BLOCKED);
		assertThat(result.contractRuleResults())
				.anySatisfy(ruleResult -> {
					assertThat(ruleResult.ruleCode()).isEqualTo(LabRef001ObservationSubjectRule.RULE_CODE);
					assertThat(ruleResult.outcome()).isEqualTo(RuleOutcome.FAIL);
				});
	}

	@Test
	void blocksGateWhenUcumContractRuleFails() {
		ValidationResult result = service.parse(fixture("observation-quantity-wrong-ucum-system.json"));

		assertThat(result.gateOutcome()).isEqualTo(GateOutcome.BLOCKED);
		assertThat(result.contractRuleResults())
				.anySatisfy(ruleResult -> {
					assertThat(ruleResult.ruleCode()).isEqualTo(LabUnit002ObservationUcumCodeRule.RULE_CODE);
					assertThat(ruleResult.outcome()).isEqualTo(RuleOutcome.FAIL);
				});
	}

	@Test
	void summarizesBundleEntriesForDay3ResourceInventory() {
		ValidationResult result = service.parse(fixture("valid-minimal-lab-bundle.json"));

		assertThat(result.bundleEntrySummaries())
				.extracting(BundleEntrySummary::resourceType)
				.containsExactly("Patient", "Observation", "DiagnosticReport");
		assertThat(result.bundleEntrySummaries())
				.extracting(BundleEntrySummary::id)
				.containsExactly("patient-1", "obs-1", "report-1");
		assertThat(result.bundleEntrySummaries())
				.extracting(BundleEntrySummary::fullUrl)
				.containsExactly(
						"urn:uuid:123e4567-e89b-12d3-a456-426614174000",
						"urn:uuid:223e4567-e89b-12d3-a456-426614174001",
						"urn:uuid:323e4567-e89b-12d3-a456-426614174002"
				);
		assertThat(result.bundleEntrySummaries())
				.extracting(BundleEntrySummary::evaluationStatus)
				.containsExactly("SUPPORTED", "SUPPORTED", "SUPPORTED");
	}

	@Test
	void marksUnsupportedBundleResourcesAsNotEvaluated() {
		ValidationResult result = service.parse(fixture("unsupported-resource-in-bundle.json"));

		assertThat(result.resourceSummary().patientCount()).isEqualTo(1);
		assertThat(result.resourceSummary().observationCount()).isZero();
		assertThat(result.resourceSummary().diagnosticReportCount()).isZero();
		assertThat(result.resourceSummary().notEvaluatedCount()).isEqualTo(1);
		assertThat(result.bundleEntrySummaries())
				.anySatisfy(entry -> {
					assertThat(entry.resourceType()).isEqualTo("Practitioner");
					assertThat(entry.id()).isEqualTo("practitioner-1");
					assertThat(entry.evaluationStatus()).isEqualTo("NOT_EVALUATED");
				});
	}

	@Test
	void reportsFhirValidationIssuesForInvalidBundle() {
		ValidationResult result = service.parse(fixture("missing-bundle-type.json"));

		assertThat(result.jsonStatus()).isEqualTo(ParseStatus.PASSED);
		assertThat(result.fhirR4Status()).isEqualTo(ParseStatus.PASSED);
		assertThat(result.resourceTypeStatus()).isEqualTo(ParseStatus.PASSED);
		assertThat(result.fhirValidationStatus()).isEqualTo(ParseStatus.FAILED);
		assertThat(result.operationOutcomeIssues())
				.anySatisfy(issue -> {
					assertThat(issue.severity()).isIn("error", "fatal");
					assertThat(issue.diagnostics()).containsIgnoringCase("Bundle.type");
				});
	}

	@Test
	void reportsInvalidJsonWithoutThrowing() {
		ValidationResult result = service.parse("{ not-json");

		assertThat(result.jsonStatus()).isEqualTo(ParseStatus.FAILED);
		assertThat(result.fhirR4Status()).isEqualTo(ParseStatus.FAILED);
		assertThat(result.resourceTypeStatus()).isEqualTo(ParseStatus.FAILED);
		assertThat(result.fhirValidationStatus()).isEqualTo(ParseStatus.NOT_EVALUATED);
		assertThat(result.operationOutcomeIssues()).isEmpty();
		assertThat(result.contractRuleResults()).isEmpty();
		assertThat(result.gateOutcome()).isEqualTo(GateOutcome.BLOCKED);
		assertThat(result.bundleEntrySummaries()).isEmpty();
		assertThat(result.resourceSummary()).isEqualTo(ResourceSummary.empty());
		assertThat(result.twCoreValidationResult().status()).isEqualTo(ParseStatus.NOT_EVALUATED);
		assertThat(result.twCoreValidationResult().message()).contains("Bundle gate 尚未通過");
		assertThat(result.errorMessage()).contains("JSON parse failed");
	}

	@Test
	void reportsNonBundleFhirResource() {
		ValidationResult result = service.parse("""
				{
				  "resourceType": "Patient",
				  "id": "patient-1"
				}
				""");

		assertThat(result.jsonStatus()).isEqualTo(ParseStatus.PASSED);
		assertThat(result.fhirR4Status()).isEqualTo(ParseStatus.PASSED);
		assertThat(result.resourceTypeStatus()).isEqualTo(ParseStatus.FAILED);
		assertThat(result.fhirValidationStatus()).isEqualTo(ParseStatus.NOT_EVALUATED);
		assertThat(result.operationOutcomeIssues()).isEmpty();
		assertThat(result.contractRuleResults()).isEmpty();
		assertThat(result.gateOutcome()).isEqualTo(GateOutcome.BLOCKED);
		assertThat(result.bundleEntrySummaries()).isEmpty();
		assertThat(result.resourceSummary()).isEqualTo(ResourceSummary.empty());
		assertThat(result.twCoreValidationResult().status()).isEqualTo(ParseStatus.NOT_EVALUATED);
		assertThat(result.twCoreValidationResult().message()).contains("Bundle gate 尚未通過");
		assertThat(result.resourceType()).isEqualTo("Patient");
		assertThat(result.errorMessage()).contains("resourceType is not Bundle");
	}

	@Test
	void reportsPlainJsonAsFhirFailure() {
		ValidationResult result = service.parse("""
				{
				  "hello": "world"
				}
				""");

		assertThat(result.jsonStatus()).isEqualTo(ParseStatus.PASSED);
		assertThat(result.fhirR4Status()).isEqualTo(ParseStatus.FAILED);
		assertThat(result.resourceTypeStatus()).isEqualTo(ParseStatus.FAILED);
		assertThat(result.fhirValidationStatus()).isEqualTo(ParseStatus.NOT_EVALUATED);
		assertThat(result.operationOutcomeIssues()).isEmpty();
		assertThat(result.bundleEntrySummaries()).isEmpty();
		assertThat(result.resourceSummary()).isEqualTo(ResourceSummary.empty());
		assertThat(result.errorMessage()).contains("FHIR R4 parse failed");
	}

	@Test
	void keepsTwCoreNotEvaluatedWhenPackageProbeFails() {
		BundleParseService failingService = new BundleParseService(
				new ObjectMapper(),
				fhirContext,
				fhirConfig.fhirValidator(fhirContext),
				new TwCoreValidationService(new CountingTwCorePackageProbe(
						TwCorePackageProbeResult.failed(
								"PACKAGE_SOURCE",
								"TW Core package loading probe 失敗：tw.gov.mohw.twcore#1.0.0 無法穩定載入；分類=PACKAGE_SOURCE；原因=test failure"
						)
				), profileValidator),
				contractRules()
		);

		ValidationResult result = failingService.parse(fixture("valid-minimal-lab-bundle.json"));

		assertThat(result.twCoreValidationResult().status()).isEqualTo(ParseStatus.NOT_EVALUATED);
		assertThat(result.twCoreValidationResult().message())
				.contains("PACKAGE_SOURCE")
				.contains("維持 NOT_EVALUATED")
				.contains("不冒充 Profile 通過");
		assertThat(result.gateOutcome()).isEqualTo(GateOutcome.PASS_WITH_WARNINGS);
	}

	@Test
	void onlyRunsTwCorePackageProbeAfterBundleGatePasses() {
		CountingTwCorePackageProbe countingProbe = new CountingTwCorePackageProbe(
				TwCorePackageProbeResult.loaded("TW Core package loading probe 成功：test package loaded。")
		);
		BundleParseService countingService = new BundleParseService(
				new ObjectMapper(),
				fhirContext,
				fhirConfig.fhirValidator(fhirContext),
				new TwCoreValidationService(countingProbe, profileValidator),
				contractRules()
		);

		countingService.parse("{ not-json");
		countingService.parse("""
				{
				  "resourceType": "Patient",
				  "id": "patient-1"
				}
				""");
		countingService.parse(fixture("valid-minimal-lab-bundle.json"));
		countingService.parse(fixture("valid-minimal-lab-bundle.json"));

		assertThat(countingProbe.callCount()).isEqualTo(1);
		assertThat(profileValidator.callCount()).isEqualTo(2);
	}

	@Test
	void keepsTwCoreNotEvaluatedWhenProfileValidatorCannotRun() {
		BundleParseService notEvaluatedService = new BundleParseService(
				new ObjectMapper(),
				fhirContext,
				fhirConfig.fhirValidator(fhirContext),
				new TwCoreValidationService(packageProbe, new StubTwCoreProfileValidator(
						TwCoreProfileValidationResult.notEvaluated(
								"Day 6 TW Core Profile validation spike 無法穩定執行；分類=VALIDATION_SUPPORT_CONFIG；原因=test failure。因此 TW Core validation 維持 NOT_EVALUATED，不冒充 Profile 通過。",
								List.of()
						)
				)),
				contractRules()
		);

		ValidationResult result = notEvaluatedService.parse(fixture("valid-minimal-lab-bundle.json"));

		assertThat(result.twCoreValidationResult().status()).isEqualTo(ParseStatus.NOT_EVALUATED);
		assertThat(result.twCoreValidationResult().message())
				.contains("VALIDATION_SUPPORT_CONFIG")
				.contains("維持 NOT_EVALUATED")
				.contains("不冒充 Profile 通過");
	}

	@Test
	void marksTwCoreFailedWhenProfileValidationReturnsError() {
		OperationOutcomeIssue issue = new OperationOutcomeIssue(
				"error",
				"Patient/patient-1: Patient.identifier",
				"Test profile validation error."
		);
		BundleParseService failingProfileService = new BundleParseService(
				new ObjectMapper(),
				fhirContext,
				fhirConfig.fhirValidator(fhirContext),
				new TwCoreValidationService(packageProbe, new StubTwCoreProfileValidator(
						TwCoreProfileValidationResult.failed(
								"Day 6 已執行最小 TW Core Profile validation；存在 error/fatal issue。",
								List.of(issue)
						)
				)),
				contractRules()
		);

		ValidationResult result = failingProfileService.parse(fixture("valid-minimal-lab-bundle.json"));

		assertThat(result.twCoreValidationResult().status()).isEqualTo(ParseStatus.FAILED);
		assertThat(result.twCoreValidationResult().operationOutcomeIssues()).containsExactly(issue);
		assertThat(result.gateOutcome()).isEqualTo(GateOutcome.BLOCKED);
	}

	@Test
	void parsesReferenceExplorationFixturesForDay5() {
		for (String fixtureName : new String[] {
				"valid-internal-reference.json",
				"missing-internal-reference.json",
				"external-http-reference.json"
		}) {
			ValidationResult result = service.parse(fixture(fixtureName));

			assertThat(result.jsonStatus()).isEqualTo(ParseStatus.PASSED);
			assertThat(result.fhirR4Status()).isEqualTo(ParseStatus.PASSED);
			assertThat(result.resourceTypeStatus()).isEqualTo(ParseStatus.PASSED);
			assertThat(result.resourceSummary().patientCount()).isEqualTo(1);
			assertThat(result.resourceSummary().observationCount()).isEqualTo(1);
			assertThat(result.resourceSummary().diagnosticReportCount()).isEqualTo(1);
			assertThat(result.bundleEntrySummaries())
					.extracting(BundleEntrySummary::evaluationStatus)
					.containsExactly("SUPPORTED", "SUPPORTED", "SUPPORTED");
		}
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

	private static class CountingTwCorePackageProbe implements TwCorePackageProbe {

		private final TwCorePackageProbeResult result;
		private int callCount;

		CountingTwCorePackageProbe(TwCorePackageProbeResult result) {
			this.result = result;
		}

		@Override
		public TwCorePackageProbeResult probe() {
			callCount++;
			return result;
		}

		int callCount() {
			return callCount;
		}
	}

	private static class StubTwCoreProfileValidator implements TwCoreProfileValidator {

		private final TwCoreProfileValidationResult result;
		private int callCount;

		StubTwCoreProfileValidator(TwCoreProfileValidationResult result) {
			this.result = result;
		}

		@Override
		public TwCoreProfileValidationResult validate(org.hl7.fhir.r4.model.Bundle bundle) {
			callCount++;
			return result;
		}

		int callCount() {
			return callCount;
		}
	}
}
