package com.twlab.qualitygate.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class ExchangeContractService {

	private static final String V1_0_PATH = "contracts/demo-lab-v1.0.json";
	private static final String V1_1_PATH = "contracts/demo-lab-v1.1.json";

	private final ObjectMapper objectMapper;
	private final Set<String> knownRuleCodes;
	private final Map<String, ExchangeContract> contractsByVersion;

	public ExchangeContractService(ObjectMapper objectMapper, List<ContractRule> contractRules) {
		this.objectMapper = objectMapper;
		this.knownRuleCodes = contractRules.stream()
				.map(ContractRule::ruleCode)
				.collect(Collectors.toUnmodifiableSet());
		List<ExchangeContract> contracts = List.of(
				load(objectMapper, V1_0_PATH),
				load(objectMapper, V1_1_PATH)
		);
		contracts.forEach(contract -> validate(contract, knownRuleCodes));
		this.contractsByVersion = contracts.stream()
				.collect(Collectors.toUnmodifiableMap(ExchangeContract::version, Function.identity()));
	}

	public ExchangeContract defaultContract() {
		return v1_1Contract();
	}

	public ExchangeContract v1Contract() {
		return contract("1.0");
	}

	public ExchangeContract v1_1Contract() {
		return contract("1.1");
	}

	public ExchangeContract parseUploadedContract(String contractJson) {
		try {
			ExchangeContract contract = objectMapper.readValue(contractJson, ExchangeContract.class);
			validate(contract, knownRuleCodes);
			return contract;
		} catch (IOException ex) {
			throw new IllegalArgumentException("Exchange contract JSON parse failed: " + conciseMessage(ex), ex);
		} catch (IllegalStateException ex) {
			throw new IllegalArgumentException(ex.getMessage(), ex);
		}
	}

	private ExchangeContract contract(String version) {
		ExchangeContract contract = contractsByVersion.get(version);
		if (contract == null) {
			throw new IllegalStateException("Missing bundled exchange contract version: " + version);
		}
		return contract;
	}

	private ExchangeContract load(ObjectMapper objectMapper, String path) {
		try (var input = new ClassPathResource(path).getInputStream()) {
			return objectMapper.readValue(input, ExchangeContract.class);
		} catch (IOException ex) {
			throw new UncheckedIOException("Failed to load exchange contract: " + path, ex);
		}
	}

	private void validate(ExchangeContract contract, Set<String> knownRuleCodes) {
		requireText(contract.id(), "id", contract);
		requireText(contract.name(), "name", contract);
		requireText(contract.version(), "version", contract);
		if (contract.enabledRuleCodes().isEmpty()) {
			throw new IllegalStateException("Exchange contract has no enabledRuleCodes: " + contract.displayName());
		}
		List<String> unknownRuleCodes = contract.enabledRuleCodes().stream()
				.filter(ruleCode -> !knownRuleCodes.contains(ruleCode))
				.sorted()
				.toList();
		if (!unknownRuleCodes.isEmpty()) {
			throw new IllegalStateException(
					"Exchange contract " + contract.displayName() + " references unknown rule codes: " + unknownRuleCodes
			);
		}
	}

	private void requireText(String value, String field, ExchangeContract contract) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException("Exchange contract missing " + field + ": " + contract);
		}
	}

	private String conciseMessage(Exception ex) {
		String message = ex.getMessage();
		if (message == null || message.isBlank()) {
			return ex.getClass().getSimpleName();
		}
		return message.lines().findFirst().orElse(message);
	}
}
