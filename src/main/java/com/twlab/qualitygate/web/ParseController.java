package com.twlab.qualitygate.web;

import com.twlab.qualitygate.validation.BundleParseService;
import com.twlab.qualitygate.validation.ContractComparisonService;
import com.twlab.qualitygate.validation.ExchangeContract;
import com.twlab.qualitygate.validation.ExchangeContractService;
import com.twlab.qualitygate.validation.OperationOutcomeIssue;
import com.twlab.qualitygate.validation.ParseStatus;
import com.twlab.qualitygate.validation.ResourceSummary;
import com.twlab.qualitygate.validation.TwCoreValidationResult;
import com.twlab.qualitygate.validation.ValidationResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class ParseController {

	private final BundleParseService bundleParseService;
	private final ContractComparisonService contractComparisonService;
	private final ExchangeContractService exchangeContractService;

	public ParseController(
			BundleParseService bundleParseService,
			ContractComparisonService contractComparisonService,
			ExchangeContractService exchangeContractService
	) {
		this.bundleParseService = bundleParseService;
		this.contractComparisonService = contractComparisonService;
		this.exchangeContractService = exchangeContractService;
	}

	@GetMapping({"/", "/parse", "/validate"})
	public String index(Model model) {
		model.addAttribute("bundleJson", sampleBundle());
		model.addAttribute("result", ValidationResult.empty());
		model.addAttribute("compareContractVersions", false);
		addContractAttributes(model);
		return "index";
	}

	@PostMapping({"/parse", "/validate"})
	public String parse(
			@RequestParam(name = "bundleJson", required = false) String bundleJson,
			@RequestParam(name = "bundleFile", required = false) MultipartFile bundleFile,
			@RequestParam(name = "contractFile", required = false) MultipartFile contractFile,
			@RequestParam(name = "comparisonContractFiles", required = false) List<MultipartFile> comparisonContractFiles,
			@RequestParam(name = "compareContractVersions", defaultValue = "false") boolean compareContractVersions,
			Model model
	) {
		String input;
		ExchangeContract activeContract;
		List<ExchangeContract> comparisonContracts;
		try {
			input = readInput(bundleJson, bundleFile);
			activeContract = readContract(contractFile);
			comparisonContracts = readComparisonContracts(comparisonContractFiles, compareContractVersions);
		} catch (IOException ex) {
			input = "";
			model.addAttribute("bundleJson", input);
			model.addAttribute("result", fileReadFailed(ex));
			model.addAttribute("compareContractVersions", compareContractVersions);
			addContractAttributes(model);
			return "index";
		} catch (IllegalArgumentException ex) {
			input = bundleJson == null ? "" : bundleJson;
			model.addAttribute("bundleJson", input);
			model.addAttribute("result", contractReadFailed(ex));
			model.addAttribute("compareContractVersions", compareContractVersions);
			addContractAttributes(model);
			return "index";
		}
		model.addAttribute("bundleJson", input);
		model.addAttribute("result", bundleParseService.parse(input, activeContract));
		if (compareContractVersions) {
			model.addAttribute("comparison", contractComparisonService.compare(input, comparisonContracts));
		}
		model.addAttribute("compareContractVersions", compareContractVersions);
		addContractAttributes(model, activeContract);
		return "index";
	}

	private void addContractAttributes(Model model) {
		addContractAttributes(model, exchangeContractService.defaultContract());
	}

	private void addContractAttributes(Model model, ExchangeContract activeContract) {
		model.addAttribute("activeContract", exchangeContractService.defaultContract());
		model.addAttribute("selectedContract", activeContract);
		model.addAttribute("v1Contract", exchangeContractService.v1Contract());
		model.addAttribute("v11Contract", exchangeContractService.v1_1Contract());
	}

	private String readInput(String bundleJson, MultipartFile bundleFile) throws IOException {
		if (bundleFile != null && !bundleFile.isEmpty()) {
			return new String(bundleFile.getBytes(), StandardCharsets.UTF_8);
		}
		return bundleJson == null ? "" : bundleJson;
	}

	private ExchangeContract readContract(MultipartFile contractFile) throws IOException {
		if (contractFile == null || contractFile.isEmpty()) {
			return exchangeContractService.defaultContract();
		}
		String contractJson = new String(contractFile.getBytes(), StandardCharsets.UTF_8);
		return exchangeContractService.parseUploadedContract(contractJson);
	}

	private List<ExchangeContract> readComparisonContracts(
			List<MultipartFile> comparisonContractFiles,
			boolean compareContractVersions
	) throws IOException {
		if (!compareContractVersions) {
			return List.of();
		}
		if (comparisonContractFiles == null) {
			throw new IllegalArgumentException("Upload at least two contract version JSON files to compare.");
		}
		List<ExchangeContract> contracts = new ArrayList<>();
		for (MultipartFile file : comparisonContractFiles) {
			if (!file.isEmpty()) {
				String contractJson = new String(file.getBytes(), StandardCharsets.UTF_8);
				contracts.add(exchangeContractService.parseUploadedContract(contractJson));
			}
		}
		if (contracts.size() < 2) {
			throw new IllegalArgumentException("Upload at least two contract version JSON files to compare.");
		}
		return contracts;
	}

	private ValidationResult fileReadFailed(IOException ex) {
		return new ValidationResult(
				ParseStatus.FAILED,
				ParseStatus.FAILED,
				ParseStatus.FAILED,
				ParseStatus.NOT_EVALUATED,
				TwCoreValidationResult.notEvaluated("TW Core validation did not run: file read failed."),
				List.of(new OperationOutcomeIssue("fatal", "N/A", "File read failed before FHIR validation.")),
				ResourceSummary.empty(),
				List.of(),
				null,
				null,
				"File read failed: " + ex.getMessage()
		);
	}

	private ValidationResult contractReadFailed(IllegalArgumentException ex) {
		return new ValidationResult(
				ParseStatus.FAILED,
				ParseStatus.FAILED,
				ParseStatus.FAILED,
				ParseStatus.NOT_EVALUATED,
				TwCoreValidationResult.notEvaluated("TW Core validation did not run: exchange contract upload failed."),
				List.of(new OperationOutcomeIssue("fatal", "ExchangeContract", ex.getMessage())),
				ResourceSummary.empty(),
				List.of(),
				null,
				null,
				"Exchange contract upload failed: " + ex.getMessage()
		);
	}

	private String sampleBundle() {
		return """
				{
				  "resourceType": "Bundle",
				  "type": "collection",
				  "entry": [
				    {
				      "fullUrl": "urn:uuid:123e4567-e89b-12d3-a456-426614174000",
				      "resource": {
				        "resourceType": "Patient",
				        "id": "patient-1"
				      }
				    },
				    {
				      "fullUrl": "urn:uuid:223e4567-e89b-12d3-a456-426614174001",
				      "resource": {
				        "resourceType": "Observation",
				        "id": "obs-1",
				        "status": "final",
				        "code": {
				          "coding": [
				            {
				              "system": "http://loinc.org",
				              "code": "2345-7",
				              "display": "Glucose [Mass/volume] in Blood"
				            }
				          ]
				        },
				        "subject": {
				          "reference": "Patient/patient-1"
				        },
				        "valueQuantity": {
				          "value": 95,
				          "unit": "mg/dL",
				          "system": "http://unitsofmeasure.org",
				          "code": "mg/dL"
				        }
				      }
				    },
				    {
				      "fullUrl": "urn:uuid:323e4567-e89b-12d3-a456-426614174002",
				      "resource": {
				        "resourceType": "DiagnosticReport",
				        "id": "report-1",
				        "status": "final",
				        "code": {
				          "text": "Basic lab report"
				        },
				        "subject": {
				          "reference": "Patient/patient-1"
				        },
				        "result": [
				          {
				            "reference": "Observation/obs-1"
				          }
				        ]
				      }
				    }
				  ]
				}
				""";
	}
}
