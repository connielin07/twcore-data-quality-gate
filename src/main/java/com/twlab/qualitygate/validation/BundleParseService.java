package com.twlab.qualitygate.validation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Service;

@Service
public class BundleParseService {

	private final ObjectMapper objectMapper;
	private final FhirContext fhirContext;
	private final FhirValidator fhirValidator;

	public BundleParseService(ObjectMapper objectMapper, FhirContext fhirContext, FhirValidator fhirValidator) {
		this.objectMapper = objectMapper;
		this.fhirContext = fhirContext;
		this.fhirValidator = fhirValidator;
	}

	public ValidationResult parse(String bundleJson) {
		if (bundleJson == null || bundleJson.isBlank()) {
			return new ValidationResult(
					ParseStatus.FAILED,
					ParseStatus.FAILED,
					ParseStatus.FAILED,
					ParseStatus.NOT_EVALUATED,
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

			return new ValidationResult(
					ParseStatus.PASSED,
					ParseStatus.PASSED,
					ParseStatus.PASSED,
					hasErrors(validationResult.getMessages()) ? ParseStatus.FAILED : ParseStatus.PASSED,
					issues,
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
				List.of(),
				ResourceSummary.empty(),
				List.of(),
				null,
				resourceType,
				"FHIR R4 parse failed: " + conciseMessage(ex)
			);
		}
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
