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
				probeResult.message() + " 因此 TW Core validation 維持 NOT_EVALUATED，不冒充 Profile 通過。"
		);
	}

	public TwCoreValidationResult notEvaluatedBeforeBundleGate() {
		return TwCoreValidationResult.notEvaluated(
				"TW Core validation 未執行：JSON、FHIR R4 parse 或 Bundle gate 尚未通過。"
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
