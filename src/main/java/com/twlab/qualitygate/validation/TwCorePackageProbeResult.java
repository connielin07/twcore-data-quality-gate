package com.twlab.qualitygate.validation;

import java.util.List;

public record TwCorePackageProbeResult(
		boolean loaded,
		String category,
		String message,
		List<String> missingCanonicals
) {
	public static TwCorePackageProbeResult loaded(String message) {
		return new TwCorePackageProbeResult(true, "LOADED", message, List.of());
	}

	public static TwCorePackageProbeResult failed(String category, String message) {
		return new TwCorePackageProbeResult(false, category, message, List.of());
	}

	public static TwCorePackageProbeResult missingCanonicals(List<String> missingCanonicals) {
		return new TwCorePackageProbeResult(
				false,
				"CANONICAL_RESOLUTION",
				"TW Core package was loaded, but required StructureDefinitions were not found: "
						+ String.join(", ", missingCanonicals),
				List.copyOf(missingCanonicals)
		);
	}
}
