package com.twlab.qualitygate.validation;

import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Service;

@Service
public class TwCoreValidationService {

	private final TwCorePackageProbe packageProbe;
	private volatile TwCorePackageProbeResult packageProbeResult;

	public TwCoreValidationService(TwCorePackageProbe packageProbe) {
		this.packageProbe = packageProbe;
	}

	public TwCoreValidationResult validate(Bundle bundle) {
		TwCorePackageProbeResult probeResult = packageProbeResult();
		if (probeResult.loaded()) {
			return TwCoreValidationResult.notEvaluated(
					probeResult.message() + " Day 5 僅完成 package loading probe，尚未執行正式 TW Core Profile validation，不冒充 Profile 通過。"
			);
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
