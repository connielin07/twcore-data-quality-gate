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
				.andExpect(content().string(containsString("TW Lab Contract Gate - Day 1")));
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
				.andExpect(content().string(containsString("Bundle")));
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
}
