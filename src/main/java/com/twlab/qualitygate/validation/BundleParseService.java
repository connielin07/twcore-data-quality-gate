package com.twlab.qualitygate.validation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Service;

@Service
public class BundleParseService {

	private final ObjectMapper objectMapper;
	private final FhirContext fhirContext;

	public BundleParseService(ObjectMapper objectMapper, FhirContext fhirContext) {
		this.objectMapper = objectMapper;
		this.fhirContext = fhirContext;
	}

	public ValidationResult parse(String bundleJson) {
		if (bundleJson == null || bundleJson.isBlank()) {
			return new ValidationResult(
					ParseStatus.FAILED,
					ParseStatus.FAILED,
					ParseStatus.FAILED,
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
						null,
						resourceType,
						"FHIR R4 parse succeeded, but resourceType is not Bundle."
				);
			}

			return new ValidationResult(
					ParseStatus.PASSED,
					ParseStatus.PASSED,
					ParseStatus.PASSED,
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
					null,
					resourceType,
					"FHIR R4 parse failed: " + conciseMessage(ex)
			);
		}
	}

	private String conciseMessage(Exception ex) {
		String message = ex.getMessage();
		if (message == null || message.isBlank()) {
			return ex.getClass().getSimpleName();
		}
		return message.lines().findFirst().orElse(message);
	}
}
