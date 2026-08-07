package com.twlab.qualitygate.validation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationOptions;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.SnapshotGeneratingValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.springframework.stereotype.Component;

@Component
public class HapiTwCoreProfileValidator implements TwCoreProfileValidator {

	private static final String CANONICAL_BASE = "https://twcore.mohw.gov.tw/ig/twcore";
	private static final Map<String, String> PROFILES_BY_RESOURCE_TYPE = Map.of(
			"Patient", CANONICAL_BASE + "/StructureDefinition/Patient-twcore",
			"Observation", CANONICAL_BASE + "/StructureDefinition/Observation-simple-twcore",
			"DiagnosticReport", CANONICAL_BASE + "/StructureDefinition/DiagnosticReport-twcore"
	);
	private static final List<String> PACKAGE_RESOURCE_TYPES = List.of("StructureDefinition", "ValueSet", "CodeSystem");

	private final FhirContext fhirContext;
	private volatile FhirValidator twCoreValidator;

	public HapiTwCoreProfileValidator(FhirContext fhirContext) {
		this.fhirContext = fhirContext;
	}

	@Override
	public TwCoreProfileValidationResult validate(Bundle bundle) {
		try {
			FhirValidator validator = validator();
			List<OperationOutcomeIssue> issues = new ArrayList<>();

			for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
				if (entry.getResource() == null) {
					continue;
				}
				String resourceType = entry.getResource().fhirType();
				String profile = PROFILES_BY_RESOURCE_TYPE.get(resourceType);
				if (profile == null) {
					continue;
				}
				ca.uhn.fhir.validation.ValidationResult result = validator.validateWithResult(
						entry.getResource(),
						new ValidationOptions().addProfile(profile)
				);
				for (SingleValidationMessage message : result.getMessages()) {
					issues.add(toIssue(resourceType, entry.getResource().getIdElement().getIdPart(), message));
				}
			}

			if (hasErrors(issues)) {
				return TwCoreProfileValidationResult.failed(
						"Day 6 已執行 Patient、Observation-simple、DiagnosticReport 的最小 TW Core Profile validation；存在 error/fatal issue，因此 TW Core validation 標示 FAILED。",
						issues
				);
			}
			return TwCoreProfileValidationResult.passed(
					"Day 6 已執行 Patient、Observation-simple、DiagnosticReport 的最小 TW Core Profile validation；未回傳 error/fatal issue。",
					issues
			);
		} catch (NoClassDefFoundError ex) {
			return notEvaluated("DEPENDENCY", ex);
		} catch (IOException ex) {
			return notEvaluated("PACKAGE_SOURCE", ex);
		} catch (DataFormatException | IllegalArgumentException ex) {
			return notEvaluated("VALIDATION_SUPPORT_CONFIG", ex);
		} catch (RuntimeException ex) {
			return notEvaluated(classify(ex), ex);
		}
	}

	private FhirValidator validator() throws IOException {
		FhirValidator localValidator = twCoreValidator;
		if (localValidator == null) {
			synchronized (this) {
				localValidator = twCoreValidator;
				if (localValidator == null) {
					localValidator = buildValidator();
					twCoreValidator = localValidator;
				}
			}
		}
		return localValidator;
	}

	private FhirValidator buildValidator() throws IOException {
		PrePopulatedValidationSupport twCoreSupport = new PrePopulatedValidationSupport(fhirContext);
		NpmPackage npmPackage = loadPackage();
		for (String resourceName : npmPackage.listResources(PACKAGE_RESOURCE_TYPES.toArray(String[]::new))) {
			try (InputStream input = npmPackage.loadResource(resourceName)) {
				IBaseResource resource = fhirContext.newJsonParser().parseResource(input);
				twCoreSupport.addResource(resource);
			}
		}

		ValidationSupportChain supportChain = new ValidationSupportChain(
				new DefaultProfileValidationSupport(fhirContext),
				twCoreSupport,
				new SnapshotGeneratingValidationSupport(fhirContext)
		);
		FhirInstanceValidator instanceValidator = new FhirInstanceValidator(supportChain);
		instanceValidator.setErrorForUnknownProfiles(true);
		instanceValidator.setNoTerminologyChecks(true);

		FhirValidator validator = fhirContext.newValidator();
		validator.registerValidatorModule(instanceValidator);
		return validator;
	}

	private NpmPackage loadPackage() throws IOException {
		FilesystemPackageCacheManager packageCacheManager =
				new FilesystemPackageCacheManager.Builder()
						.build();
		return packageCacheManager.loadPackage(
				TwCoreValidationResult.PACKAGE_ID,
				TwCoreValidationResult.PACKAGE_VERSION
		);
	}

	private OperationOutcomeIssue toIssue(String resourceType, String id, SingleValidationMessage message) {
		String severity = message.getSeverity() == null ? "UNKNOWN" : message.getSeverity().getCode();
		String location = message.getLocationString();
		if (location == null || location.isBlank()) {
			location = resourceType + "/" + valueOrDefault(id, "UNKNOWN");
		} else {
			location = resourceType + "/" + valueOrDefault(id, "UNKNOWN") + ": " + location;
		}
		String diagnostics = message.getMessage();
		if (diagnostics == null || diagnostics.isBlank()) {
			diagnostics = "N/A";
		}
		return new OperationOutcomeIssue(severity, location, diagnostics);
	}

	private boolean hasErrors(List<OperationOutcomeIssue> issues) {
		return issues.stream()
				.map(OperationOutcomeIssue::severity)
				.anyMatch(severity -> ResultSeverityEnum.ERROR.getCode().equals(severity)
						|| ResultSeverityEnum.FATAL.getCode().equals(severity));
	}

	private TwCoreProfileValidationResult notEvaluated(String category, Throwable ex) {
		return TwCoreProfileValidationResult.notEvaluated(
				"Day 6 TW Core Profile validation spike 無法穩定執行；分類="
						+ category
						+ "；原因=" + conciseMessage(ex)
						+ "。因此 TW Core validation 維持 NOT_EVALUATED，不冒充 Profile 通過。",
				List.of()
		);
	}

	private String classify(Throwable ex) {
		String text = (ex.getClass().getName() + " " + ex.getMessage()).toLowerCase();
		if (text.contains("terminolog")) {
			return "TERMINOLOGY";
		}
		if (text.contains("canonical") || text.contains("structuredefinition") || text.contains("profile")) {
			return "CANONICAL_RESOLUTION";
		}
		if (text.contains("classpath") || text.contains("dependency") || text.contains("classnotfound")) {
			return "DEPENDENCY";
		}
		if (text.contains("npm") || text.contains("package") || text.contains("http") || text.contains("not found")) {
			return "PACKAGE_SOURCE";
		}
		return "VALIDATION_SUPPORT_CONFIG";
	}

	private String conciseMessage(Throwable ex) {
		String message = ex.getMessage();
		if (message == null || message.isBlank()) {
			return ex.getClass().getSimpleName();
		}
		return message.lines().findFirst().orElse(message);
	}

	private String valueOrDefault(String preferred, String fallback) {
		if (preferred != null && !preferred.isBlank()) {
			return preferred;
		}
		return fallback;
	}
}
