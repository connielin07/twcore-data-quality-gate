package com.twlab.qualitygate.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
				.andExpect(content().string(containsString("TW Lab Contract Gate - Day 3")));
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
				.andExpect(content().string(containsString("Resource summary")))
				.andExpect(content().string(containsString("OperationOutcome issues")));
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
}
