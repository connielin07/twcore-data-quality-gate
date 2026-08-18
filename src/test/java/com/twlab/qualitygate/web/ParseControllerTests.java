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
				.andExpect(content().string(containsString("TW Lab Contract Gate - Day 17")));
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
				.andExpect(content().string(containsString("Resource summary")))
				.andExpect(content().string(containsString("OperationOutcome issues")))
				.andExpect(content().string(containsString("TW Core Profile issues")))
				.andExpect(content().string(containsString("Quality Gate")))
				.andExpect(content().string(containsString("Exchange contract rule results")))
				.andExpect(content().string(containsString("LAB-REF-001")))
				.andExpect(content().string(containsString("LAB-UNIT-002")));
	}

	@Test
	void rendersContractComparisonForValidBundle() throws Exception {
		mockMvc.perform(post("/parse")
						.param("bundleJson", fixture("valid-twcore-contract-bundle.json")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Contract comparison")))
				.andExpect(content().string(containsString("v1.0")))
				.andExpect(content().string(containsString("v1.1")))
				.andExpect(content().string(containsString("PASSED")))
				.andExpect(content().string(containsString("None")))
				.andExpect(content().string(not(containsString("升級後新增阻擋"))));
	}

	@Test
	void rendersV11UcumFailureInContractComparison() throws Exception {
		mockMvc.perform(post("/parse")
						.param("bundleJson", fixture("twcore-valid-wrong-ucum-system.json")))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Contract comparison")))
				.andExpect(content().string(containsString("v1.0")))
				.andExpect(content().string(containsString("PASSED")))
				.andExpect(content().string(containsString("v1.1")))
				.andExpect(content().string(containsString("BLOCKED")))
				.andExpect(content().string(containsString("升級後新增阻擋")))
				.andExpect(content().string(containsString("PASSED")))
				.andExpect(content().string(containsString("BLOCKED")))
				.andExpect(content().string(containsString("LAB-UNIT-002")))
				.andExpect(content().string(containsString("Observation/obs-wrong-ucum-system.valueQuantity.system/code")))
				.andExpect(content().string(containsString("http://example.org/local-units|mg/dL")))
				.andExpect(content().string(containsString("Observation.valueQuantity.system must be http://unitsofmeasure.org")))
				.andExpect(content().string(containsString("請使用合作契約允許的 UCUM 條件")));
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
}
