package com.twlab.qualitygate.web;

import com.twlab.qualitygate.validation.BundleParseService;
import com.twlab.qualitygate.validation.ContractComparisonService;
import com.twlab.qualitygate.validation.OperationOutcomeIssue;
import com.twlab.qualitygate.validation.ParseStatus;
import com.twlab.qualitygate.validation.ResourceSummary;
import com.twlab.qualitygate.validation.TwCoreValidationResult;
import com.twlab.qualitygate.validation.ValidationResult;
import java.io.IOException;
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

	public ParseController(
			BundleParseService bundleParseService,
			ContractComparisonService contractComparisonService
	) {
		this.bundleParseService = bundleParseService;
		this.contractComparisonService = contractComparisonService;
	}

	@GetMapping({"/", "/parse", "/validate"})
	public String index(Model model) {
		model.addAttribute("bundleJson", sampleBundle());
		model.addAttribute("result", ValidationResult.empty());
		return "index";
	}

	@PostMapping({"/parse", "/validate"})
	public String parse(
			@RequestParam(name = "bundleJson", required = false) String bundleJson,
			@RequestParam(name = "bundleFile", required = false) MultipartFile bundleFile,
			Model model
	) {
		String input;
		try {
			input = readInput(bundleJson, bundleFile);
		} catch (IOException ex) {
			input = "";
			model.addAttribute("bundleJson", input);
			model.addAttribute("result", fileReadFailed(ex));
			return "index";
		}
		model.addAttribute("bundleJson", input);
		model.addAttribute("result", bundleParseService.parse(input));
		model.addAttribute("comparison", contractComparisonService.compare(input));
		return "index";
	}

	private String readInput(String bundleJson, MultipartFile bundleFile) throws IOException {
		if (bundleFile != null && !bundleFile.isEmpty()) {
			return new String(bundleFile.getBytes(), StandardCharsets.UTF_8);
		}
		return bundleJson == null ? "" : bundleJson;
	}

	private ValidationResult fileReadFailed(IOException ex) {
		return new ValidationResult(
				ParseStatus.FAILED,
				ParseStatus.FAILED,
				ParseStatus.FAILED,
				ParseStatus.NOT_EVALUATED,
				TwCoreValidationResult.notEvaluated("TW Core validation 未執行：檔案讀取失敗。"),
				List.of(new OperationOutcomeIssue("fatal", "N/A", "File read failed before FHIR validation.")),
				ResourceSummary.empty(),
				List.of(),
				null,
				null,
				"File read failed: " + ex.getMessage()
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
