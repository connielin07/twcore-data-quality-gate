package com.twlab.qualitygate.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FhirConfig {

	@Bean
	public FhirContext fhirContext() {
		return FhirContext.forR4Cached();
	}

	@Bean
	public FhirValidator fhirValidator(FhirContext fhirContext) {
		FhirInstanceValidator instanceValidator = new FhirInstanceValidator(
				new DefaultProfileValidationSupport(fhirContext)
		);
		instanceValidator.setErrorForUnknownProfiles(false);
		instanceValidator.setNoTerminologyChecks(true);

		FhirValidator validator = fhirContext.newValidator();
		validator.registerValidatorModule(instanceValidator);
		return validator;
	}
}
