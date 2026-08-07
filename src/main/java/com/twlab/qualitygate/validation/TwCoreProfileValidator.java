package com.twlab.qualitygate.validation;

import org.hl7.fhir.r4.model.Bundle;

public interface TwCoreProfileValidator {

	TwCoreProfileValidationResult validate(Bundle bundle);
}
