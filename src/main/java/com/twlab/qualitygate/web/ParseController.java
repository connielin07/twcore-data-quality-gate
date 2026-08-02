package com.twlab.qualitygate.web;

import com.twlab.qualitygate.validation.BundleParseService;
import com.twlab.qualitygate.validation.ParseStatus;
import com.twlab.qualitygate.validation.ValidationResult;
import java.io.IOException;
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

	public ParseController(BundleParseService bundleParseService) {
		this.bundleParseService = bundleParseService;
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
				      "fullUrl": "urn:uuid:patient-1",
				      "resource": {
				        "resourceType": "Patient",
				        "id": "patient-1"
				      }
				    }
				  ]
				}
				""";
	}
}
