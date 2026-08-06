package com.twlab.qualitygate.validation;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.springframework.stereotype.Component;

@Component
public class HapiTwCorePackageProbe implements TwCorePackageProbe {

	private static final String CANONICAL_BASE = "https://twcore.mohw.gov.tw/ig/twcore";
	private static final String FHIR_VERSION = "4.0.1";
	private static final List<String> REQUIRED_CANONICALS = List.of(
			CANONICAL_BASE + "/StructureDefinition/Patient-twcore",
			CANONICAL_BASE + "/StructureDefinition/Observation-simple-twcore",
			CANONICAL_BASE + "/StructureDefinition/DiagnosticReport-twcore"
	);

	@Override
	public TwCorePackageProbeResult probe() {
		try {
			FilesystemPackageCacheManager packageCacheManager =
					new FilesystemPackageCacheManager.Builder()
							.build();
			NpmPackage npmPackage = packageCacheManager.loadPackage(
					TwCoreValidationResult.PACKAGE_ID,
					TwCoreValidationResult.PACKAGE_VERSION
			);

			List<String> missingCanonicals = missingRequiredCanonicals(npmPackage);
			if (!missingCanonicals.isEmpty()) {
				return TwCorePackageProbeResult.missingCanonicals(missingCanonicals);
			}

			String fhirVersion = valueOrDefault(npmPackage.fhirVersionList(), npmPackage.fhirVersion());
			String versionNote = fhirVersion.contains(FHIR_VERSION)
					? "FHIR R4 4.0.1 confirmed"
					: "FHIR version reported by package: " + fhirVersion;
			return TwCorePackageProbeResult.loaded(
					"TW Core package loading probe 成功："
							+ npmPackage.id() + "#" + npmPackage.version()
							+ "，canonical base " + CANONICAL_BASE
							+ "，" + versionNote
							+ "，已找到 Patient、Observation-simple、DiagnosticReport StructureDefinition。"
			);
		} catch (NoClassDefFoundError ex) {
			return TwCorePackageProbeResult.failed(
					"DEPENDENCY",
					"TW Core package loading probe 失敗：HAPI package loading dependency 不完整："
							+ conciseMessage(ex)
			);
		} catch (FHIRException ex) {
			return TwCorePackageProbeResult.failed("PACKAGE_SOURCE", failureMessage(ex));
		} catch (IOException ex) {
			return TwCorePackageProbeResult.failed("PACKAGE_SOURCE", failureMessage(ex));
		} catch (RuntimeException ex) {
			return TwCorePackageProbeResult.failed(classify(ex), failureMessage(ex));
		}
	}

	private List<String> missingRequiredCanonicals(NpmPackage npmPackage) throws IOException {
		List<String> missingCanonicals = new ArrayList<>();
		for (String canonical : REQUIRED_CANONICALS) {
			try (InputStream ignored = npmPackage.loadByCanonical(canonical)) {
				if (ignored == null) {
					missingCanonicals.add(canonical);
				}
			}
		}
		return missingCanonicals;
	}

	private String failureMessage(Throwable ex) {
		return "TW Core package loading probe 失敗："
				+ TwCoreValidationResult.PACKAGE_ID + "#" + TwCoreValidationResult.PACKAGE_VERSION
				+ " 無法穩定載入；分類=" + classify(ex)
				+ "；原因=" + conciseMessage(ex);
	}

	private String classify(Throwable ex) {
		String text = (ex.getClass().getName() + " " + ex.getMessage()).toLowerCase();
		if (text.contains("terminolog")) {
			return "TERMINOLOGY";
		}
		if (text.contains("canonical") || text.contains("structuredefinition")) {
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
		if (fallback != null && !fallback.isBlank()) {
			return fallback;
		}
		return "UNKNOWN";
	}
}
