package com.twlab.qualitygate.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ParseControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void rendersHomePage() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("TW Lab Contract Gate")))
				.andExpect(content().string(containsString("Loaded partner contracts")))
				.andExpect(content().string(containsString("Default validation")))
				.andExpect(content().string(containsString("Current validation")))
				.andExpect(content().string(containsString("demo-lab-hospital-a#1.1")))
				.andExpect(content().string(containsString("Comparison upload")))
				.andExpect(content().string(containsString("Upload two or more contract version JSON files")))
				.andExpect(content().string(containsString("Contract upload")))
				.andExpect(content().string(containsString("Optional JSON upload for the current validation only")))
				.andExpect(content().string(containsString("Input Bundle")))
				.andExpect(content().string(containsString("Upload Bundle JSON")))
				.andExpect(content().string(containsString("Optional partner contract JSON")))
				.andExpect(content().string(containsString("Contract versions to compare JSON")))
				.andExpect(content().string(containsString("Compare contract versions")))
				.andExpect(content().string(containsString("Validate Bundle")));
	}

	@Test
	void parsesPastedBundleJson() throws Exception {
		mockMvc.perform(post("/parse")
						.param("bundleJson", """
								{
								  "resourceType": "Bundle",
								  "type": "collection"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("PASSED")))
				.andExpect(content().string(containsString("Bundle")))
				.andExpect(content().string(containsString("tw.gov.mohw.twcore#1.0.0")))
				.andExpect(content().string(containsString("Exchange contract")))
				.andExpect(content().string(containsString("demo-lab-hospital-a#1.1")))
				.andExpect(content().string(containsString("Quality Test Report")))
				.andExpect(content().string(containsString("Overall Quality Gate")))
				.andExpect(content().string(containsString("Input summary")))
				.andExpect(content().string(containsString("Layer summary")))
				.andExpect(content().string(containsString("JSON parse")))
				.andExpect(content().string(containsString("FHIR R4 parse")))
				.andExpect(content().string(containsString("Resource Type Gate")))
				.andExpect(content().string(containsString("FHIR R4 validation")))
				.andExpect(content().string(containsString("TW Core validation")))
				.andExpect(content().string(containsString("errors")))
				.andExpect(content().string(containsString("warnings")))
				.andExpect(content().string(containsString("TW Core check ran without error/fatal issues.")))
				.andExpect(content().string(containsString("Exchange contract rules")))
				.andExpect(content().string(not(containsString("Contract version comparison"))))
				.andExpect(content().string(containsString("Resource inventory")))
				.andExpect(content().string(containsString("FHIR R4 issues")))
				.andExpect(content().string(containsString("TW Core Profile issues")))
				.andExpect(content().string(containsString("Quality Gate")))
				.andExpect(content().string(containsString("Exchange rule evidence")))
				.andExpect(content().string(containsString("Exchange rules show rule code")))
				.andExpect(content().string(containsString("table-scroll")))
				.andExpect(content().string(containsString("LAB-REF-001")))
				.andExpect(content().string(containsString("LAB-UNIT-002")));
	}

	@Test
	void rendersContractComparisonForValidBundle() throws Exception {
		mockMvc.perform(multipart("/parse")
				.file(comparisonContractFile("demo-lab-v1.0.json"))
				.file(comparisonContractFile("demo-lab-v1.1.json"))
				.param("compareContractVersions", "true")
				.param("bundleJson", fixture("valid-twcore-contract-bundle.json")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Contract version comparison")))
				.andExpect(content().string(containsString("1.0")))
				.andExpect(content().string(containsString("1.1")))
				.andExpect(content().string(containsString("Loaded contract")))
				.andExpect(content().string(containsString("demo-lab-hospital-a#1.0")))
				.andExpect(content().string(containsString("demo-lab-hospital-a#1.1")))
				.andExpect(content().string(containsString("PASSED")))
				.andExpect(content().string(containsString("No impact")))
				.andExpect(content().string(containsString("Blocking reason")))
				.andExpect(content().string(containsString("No blocking issue.")))
				.andExpect(content().string(containsString("None")))
				.andExpect(content().string(not(containsString("Upgrade blocker evidence"))));
	}

	@Test
	void rendersV11UcumFailureInContractComparison() throws Exception {
		mockMvc.perform(multipart("/parse")
				.file(comparisonContractFile("demo-lab-v1.0.json"))
				.file(comparisonContractFile("demo-lab-v1.1.json"))
				.param("compareContractVersions", "true")
				.param("bundleJson", fixture("observation-quantity-wrong-ucum-system.json")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Contract version comparison")))
				.andExpect(content().string(containsString("1.0")))
				.andExpect(content().string(containsString("1.1")))
				.andExpect(content().string(containsString("BLOCKED")))
				.andExpect(content().string(containsString("Impact")))
				.andExpect(content().string(containsString("Exchange contract rule failed.")))
				.andExpect(content().string(containsString("Upgrade blocker evidence")))
				.andExpect(content().string(containsString("upgrade-evidence-table")))
				.andExpect(content().string(containsString("Later uploaded contract versions add exchange rule failures")))
				.andExpect(content().string(containsString("LAB-UNIT-002")))
				.andExpect(content().string(containsString("Observation/obs-wrong-ucum-system.valueQuantity.system/code")))
				.andExpect(content().string(containsString("http://example.org/local-units|mg/dL")))
				.andExpect(content().string(containsString("Observation.valueQuantity.system must be http://unitsofmeasure.org")))
				.andExpect(content().string(containsString("Use UCUM conditions allowed by the exchange contract")));
	}

	@Test
	void reportsMissingUploadedContractsForVersionComparison() throws Exception {
		mockMvc.perform(post("/parse")
						.param("compareContractVersions", "true")
						.param("bundleJson", fixture("valid-twcore-contract-bundle.json")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Exchange contract upload failed")))
				.andExpect(content().string(containsString("Upload at least two contract version JSON files to compare.")))
				.andExpect(content().string(not(containsString("Contract version comparison"))));
	}

	@Test
	void rendersResourceInventoryForSupportedAndUnsupportedEntries() throws Exception {
		mockMvc.perform(post("/parse")
						.param("bundleJson", """
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
								      "fullUrl": "urn:uuid:423e4567-e89b-12d3-a456-426614174003",
								      "resource": {
								        "resourceType": "Practitioner",
								        "id": "practitioner-1"
								      }
								    }
								  ]
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("patient-1")))
				.andExpect(content().string(containsString("practitioner-1")))
				.andExpect(content().string(containsString("NOT_EVALUATED")));
	}

	@Test
	void parsesUploadedBundleJson() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"bundleFile",
				"bundle.json",
				"application/json",
				"""
						{
						  "resourceType": "Bundle",
						  "type": "collection"
						}
						""".getBytes()
		);

		mockMvc.perform(multipart("/parse").file(file))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("PASSED")))
				.andExpect(content().string(containsString("Bundle")));
	}

	@Test
	void parsesUploadedContractJsonForCurrentValidation() throws Exception {
		MockMultipartFile contract = new MockMultipartFile(
				"contractFile",
				"hospital-b-v1.1.json",
				"application/json",
				"""
						{
						  "id": "demo-lab-hospital-b",
						  "name": "Demo Lab to Hospital B Exchange Contract",
						  "version": "1.1",
						  "enabledRuleCodes": [
						    "LAB-REF-001",
						    "LAB-REF-002",
						    "LAB-REF-003",
						    "LAB-CODE-001",
						    "LAB-UNIT-001",
						    "LAB-UNIT-002"
						  ],
						  "allowedLoincCodes": ["2345-7", "718-7"],
						  "allowedUcumCodes": ["g/L"]
						}
						""".getBytes(StandardCharsets.UTF_8)
		);

		mockMvc.perform(multipart("/parse")
						.file(contract)
						.param("bundleJson", fixture("valid-ucum-code.json")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("demo-lab-hospital-b#1.1")))
				.andExpect(content().string(containsString("LAB-UNIT-002")))
				.andExpect(content().string(containsString("code currently allows g/L")))
				.andExpect(content().string(containsString("demo-lab-hospital-a#1.1")))
				.andExpect(content().string(not(containsString("Contract version comparison"))));
	}

	@Test
	void reportsUploadedContractWithUnknownRuleCode() throws Exception {
		MockMultipartFile contract = new MockMultipartFile(
				"contractFile",
				"bad-contract.json",
				"application/json",
				"""
						{
						  "id": "bad-contract",
						  "name": "Bad Contract",
						  "version": "1.0",
						  "enabledRuleCodes": ["LAB-UNKNOWN"],
						  "allowedLoincCodes": ["2345-7"],
						  "allowedUcumCodes": ["mg/dL"]
						}
						""".getBytes(StandardCharsets.UTF_8)
		);

		mockMvc.perform(multipart("/parse")
						.file(contract)
						.param("bundleJson", fixture("valid-ucum-code.json")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Exchange contract upload failed")))
				.andExpect(content().string(containsString("LAB-UNKNOWN")));
	}

	@Test
	void rendersFhirValidationIssue() throws Exception {
		mockMvc.perform(post("/parse")
						.param("bundleJson", """
								{
								  "resourceType": "Bundle"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("FAILED")))
				.andExpect(content().string(containsString("Bundle.type")));
	}

	private String fixture(String name) {
		try (var input = getClass().getResourceAsStream("/cases/" + name)) {
			if (input == null) {
				throw new IllegalArgumentException("Missing fixture: " + name);
			}
			return new String(input.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private MockMultipartFile comparisonContractFile(String name) {
		return new MockMultipartFile(
				"comparisonContractFiles",
				name,
				"application/json",
				resource("/contracts/" + name).getBytes(StandardCharsets.UTF_8)
		);
	}

	private String resource(String path) {
		try (var input = getClass().getResourceAsStream(path)) {
			if (input == null) {
				throw new IllegalArgumentException("Missing resource: " + path);
			}
			return new String(input.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}
}
