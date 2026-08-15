package com.twlab.qualitygate.validation;

import org.springframework.stereotype.Service;

@Service
public class ContractComparisonService {

	private final BundleParseService bundleParseService;

	public ContractComparisonService(BundleParseService bundleParseService) {
		this.bundleParseService = bundleParseService;
	}

	public ContractComparisonResult compare(String bundleJson) {
		return new ContractComparisonResult(
				bundleParseService.parse(bundleJson, ContractVersion.V1_0),
				bundleParseService.parse(bundleJson, ContractVersion.V1_1)
		);
	}
}
