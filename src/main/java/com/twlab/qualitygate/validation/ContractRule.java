package com.twlab.qualitygate.validation;

import java.util.List;
import org.hl7.fhir.r4.model.Bundle;

public interface ContractRule {

	String ruleCode();

	List<RuleResult> validate(Bundle bundle);
}
