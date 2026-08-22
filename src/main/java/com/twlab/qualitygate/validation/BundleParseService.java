package com.twlab.qualitygate.validation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Service;

@Service
public class BundleParseService {

	private final ObjectMapper objectMapper;
	private final FhirContext fhirContext;
	private final FhirValidator fhirValidator;
	private final TwCoreValidationService twCoreValidationService;
	private final List<ContractRule> contractRules;
	private final ExchangeContractService exchangeContractService;

	public BundleParseService(
			ObjectMapper objectMapper,
			FhirContext fhirContext,
			FhirValidator fhirValidator,
			TwCoreValidationService twCoreValidationService,
			List<ContractRule> contractRules,
			ExchangeContractService exchangeContractService
	) {
		this.objectMapper = objectMapper;
		this.fhirContext = fhirContext;
		this.fhirValidator = fhirValidator;
		this.twCoreValidationService = twCoreValidationService;
		this.exchangeContractService = exchangeContractService;
		this.contractRules = contractRules.stream()
				.sorted(Comparator.comparingInt(rule -> ruleOrder().getOrDefault(rule.ruleCode(), Integer.MAX_VALUE)))
				.toList();
	}

	public ValidationResult parse(String bundleJson) {
		return parse(bundleJson, exchangeContractService.defaultContract());
	}

	public ValidationResult parse(String bundleJson, ExchangeContract contract) {
		if (bundleJson == null || bundleJson.isBlank()) {
			return new ValidationResult(
					ParseStatus.FAILED,
					ParseStatus.FAILED,
					ParseStatus.FAILED,
					ParseStatus.NOT_EVALUATED,
					twCoreValidationService.notEvaluatedBeforeBundleGate(),
					List.of(),
					ResourceSummary.empty(),
					List.of(),
					null,
					null,
					"Input JSON is blank."
			);
		}

		JsonNode root;
		try {
			root = objectMapper.readTree(bundleJson);
		} catch (JsonProcessingException ex) {
			return new ValidationResult(
					ParseStatus.FAILED,
					ParseStatus.FAILED,
					ParseStatus.FAILED,
					ParseStatus.NOT_EVALUATED,
					twCoreValidationService.notEvaluatedBeforeBundleGate(),
					List.of(),
					ResourceSummary.empty(),
					List.of(),
					null,
					null,
					"JSON parse failed: " + conciseMessage(ex)
			);
		}

		try {
			IBaseResource resource = fhirContext.newJsonParser().parseResource(bundleJson);
			if (!(resource instanceof Bundle bundle)) {
				String resourceType = root.path("resourceType").asText("UNKNOWN");
				return new ValidationResult(
						ParseStatus.PASSED,
						ParseStatus.PASSED,
						ParseStatus.FAILED,
						ParseStatus.NOT_EVALUATED,
						twCoreValidationService.notEvaluatedBeforeBundleGate(),
						List.of(),
						ResourceSummary.empty(),
						List.of(),
						null,
						resourceType,
						"FHIR R4 parse succeeded, but resourceType is not Bundle."
				);
			}

			ca.uhn.fhir.validation.ValidationResult validationResult = fhirValidator.validateWithResult(bundle);
			List<OperationOutcomeIssue> issues = validationResult.getMessages().stream()
					.map(this::toIssue)
					.toList();
			List<BundleEntrySummary> bundleEntrySummaries = bundle.getEntry().stream()
					.map(BundleEntrySummary::fromEntry)
					.toList();
			TwCoreValidationResult twCoreValidationResult = twCoreValidationService.validate(bundle);
			List<RuleResult> contractRuleResults = validateContractRules(bundle, contract);
			ParseStatus fhirValidationStatus = hasErrors(validationResult.getMessages())
					? ParseStatus.FAILED
					: ParseStatus.PASSED;

			return new ValidationResult(
					ParseStatus.PASSED,
					ParseStatus.PASSED,
					ParseStatus.PASSED,
					fhirValidationStatus,
					twCoreValidationResult,
					gateOutcome(
							ParseStatus.PASSED,
							ParseStatus.PASSED,
							ParseStatus.PASSED,
							fhirValidationStatus,
							twCoreValidationResult,
							contractRuleResults,
							contract
					),
					issues,
					contractRuleResults,
					ResourceSummary.fromEntries(bundleEntrySummaries),
					bundleEntrySummaries,
					bundle.getEntry().size(),
					"Bundle",
					null
			);
		} catch (DataFormatException | IllegalArgumentException ex) {
			String resourceType = root.path("resourceType").asText("UNKNOWN");
			return new ValidationResult(
					ParseStatus.PASSED,
					ParseStatus.FAILED,
					ParseStatus.FAILED,
					ParseStatus.NOT_EVALUATED,
					twCoreValidationService.notEvaluatedBeforeBundleGate(),
					List.of(),
					ResourceSummary.empty(),
					List.of(),
					null,
					resourceType,
					"FHIR R4 parse failed: " + conciseMessage(ex)
			);
		}
	}

	private List<RuleResult> validateContractRules(Bundle bundle, ExchangeContract contract) {
		return contractRules.stream()
				.filter(rule -> contract.enables(rule.ruleCode()))
				.flatMap(rule -> rule.validate(bundle, contract).stream())
				.map(result -> withContractSeverity(result, contract))
				.toList();
	}

	private RuleResult withContractSeverity(RuleResult result, ExchangeContract contract) {
		return new RuleResult(
				result.ruleCode(),
				result.outcome(),
				contract.severityFor(result.ruleCode(), result.severity()),
				result.path(),
				result.actual(),
				result.expected(),
				result.evidence(),
				result.suggestion()
		);
	}

	private GateOutcome gateOutcome(
			ParseStatus jsonStatus,
			ParseStatus fhirR4Status,
			ParseStatus resourceTypeStatus,
			ParseStatus fhirValidationStatus,
			TwCoreValidationResult twCoreValidationResult,
			List<RuleResult> contractRuleResults,
			ExchangeContract contract
	) {
		if (jsonStatus == ParseStatus.FAILED
				|| fhirR4Status == ParseStatus.FAILED
				|| resourceTypeStatus == ParseStatus.FAILED
				|| fhirValidationStatus == ParseStatus.FAILED
				|| twCoreValidationResult.status() == ParseStatus.FAILED
				|| contractRuleResults.stream().anyMatch(result -> result.outcome() == RuleOutcome.FAIL
						&& contract.blocksExchange(result.ruleCode()))) {
			return GateOutcome.BLOCKED;
		}
		if (twCoreValidationResult.status() == ParseStatus.NOT_EVALUATED
				|| contractRuleResults.stream().anyMatch(result -> result.outcome() == RuleOutcome.NOT_EVALUATED
						|| result.outcome() == RuleOutcome.FAIL)) {
			return GateOutcome.PASS_WITH_WARNINGS;
		}
		return GateOutcome.PASSED;
	}

	private Map<String, Integer> ruleOrder() {
		return Map.of(
				LabRef001ObservationSubjectRule.RULE_CODE, 1,
				LabRef002DiagnosticReportResultRule.RULE_CODE, 2,
				LabRef003ReportObservationPatientRule.RULE_CODE, 3,
				LabCode001ObservationLoincRule.RULE_CODE, 4,
				LabUnit001ObservationQuantityUnitRule.RULE_CODE, 5,
				LabUnit002ObservationUcumCodeRule.RULE_CODE, 6
		);
	}

	private boolean hasErrors(List<SingleValidationMessage> messages) {
		return messages.stream()
				.map(SingleValidationMessage::getSeverity)
				.anyMatch(severity -> severity == ResultSeverityEnum.ERROR || severity == ResultSeverityEnum.FATAL);
	}

	private OperationOutcomeIssue toIssue(SingleValidationMessage message) {
		String severity = message.getSeverity() == null ? "UNKNOWN" : message.getSeverity().getCode();
		String location = message.getLocationString();
		if (location == null || location.isBlank()) {
			location = "N/A";
		}
		String diagnostics = message.getMessage();
		if (diagnostics == null || diagnostics.isBlank()) {
			diagnostics = "N/A";
		}
		return new OperationOutcomeIssue(severity, location, diagnostics);
	}

	private String conciseMessage(Exception ex) {
		String message = ex.getMessage();
		if (message == null || message.isBlank()) {
			return ex.getClass().getSimpleName();
		}
		return message.lines().findFirst().orElse(message);
	}
}
