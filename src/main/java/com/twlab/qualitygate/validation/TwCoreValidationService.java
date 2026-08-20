package com.twlab.qualitygate.validation;

import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Service;

@Service
public class TwCoreValidationService {

	private final TwCorePackageProbe packageProbe;
	private final TwCoreProfileValidator profileValidator;
	private volatile TwCorePackageProbeResult packageProbeResult;

	public TwCoreValidationService(TwCorePackageProbe packageProbe, TwCoreProfileValidator profileValidator) {
		this.packageProbe = packageProbe;
		this.profileValidator = profileValidator;
	}

	public TwCoreValidationResult validate(Bundle bundle) {
		TwCorePackageProbeResult probeResult = packageProbeResult();
		if (probeResult.loaded()) {
			TwCoreProfileValidationResult profileResult = profileValidator.validate(bundle);
			String message = probeResult.message() + " " + profileResult.message();
			if (profileResult.status() == ParseStatus.PASSED) {
				return TwCoreValidationResult.passed(message, profileResult.operationOutcomeIssues());
			}
			if (profileResult.status() == ParseStatus.FAILED) {
				return TwCoreValidationResult.failed(message, profileResult.operationOutcomeIssues());
			}
			return TwCoreValidationResult.notEvaluated(message, profileResult.operationOutcomeIssues());
		}
		return TwCoreValidationResult.notEvaluated(
				probeResult.message() + " TW Core validation stays NOT_EVALUATED and is not presented as a Profile pass."
		);
	}

	public TwCoreValidationResult notEvaluatedBeforeBundleGate() {
		return TwCoreValidationResult.notEvaluated(
				"TW Core validation did not run: JSON, FHIR R4 parse, or Bundle gate did not pass."
		);
	}

	private TwCorePackageProbeResult packageProbeResult() {
		TwCorePackageProbeResult localResult = packageProbeResult;
		if (localResult == null) {
			synchronized (this) {
				localResult = packageProbeResult;
				if (localResult == null) {
					localResult = packageProbe.probe();
					packageProbeResult = localResult;
				}
			}
		}
		return localResult;
	}
}
