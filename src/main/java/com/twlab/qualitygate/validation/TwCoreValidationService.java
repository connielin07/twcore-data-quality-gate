package com.twlab.qualitygate.validation;

import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Service;

@Service
public class TwCoreValidationService {

	public TwCoreValidationResult validate(Bundle bundle) {
		return TwCoreValidationResult.notEvaluated(
				"尚未穩定載入 tw.gov.mohw.twcore#1.0.0，不冒充 Profile 通過。"
		);
	}

	public TwCoreValidationResult notEvaluatedBeforeBundleGate() {
		return TwCoreValidationResult.notEvaluated(
				"TW Core validation 未執行：JSON、FHIR R4 parse 或 Bundle gate 尚未通過。"
		);
	}
}
