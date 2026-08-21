package com.twlab.qualitygate.validation;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ContractComparisonService {

	private final BundleParseService bundleParseService;
	private final ExchangeContractService exchangeContractService;

	public ContractComparisonService(BundleParseService bundleParseService, ExchangeContractService exchangeContractService) {
		this.bundleParseService = bundleParseService;
		this.exchangeContractService = exchangeContractService;
	}

	public ContractComparisonResult compare(String bundleJson) {
		return compare(bundleJson, List.of(exchangeContractService.v1Contract(), exchangeContractService.v1_1Contract()));
	}

	public ContractComparisonResult compare(String bundleJson, List<ExchangeContract> contracts) {
		if (contracts.size() < 2) {
			throw new IllegalArgumentException("Contract comparison requires at least two contract versions.");
		}
		return new ContractComparisonResult(contracts.stream()
				.map(contract -> new ContractComparisonEntry(contract, bundleParseService.parse(bundleJson, contract)))
				.toList());
	}
}
