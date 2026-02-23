package com.example.aitemplate.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.aitemplate.app.AiTemplateApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        classes = AiTemplateApplication.class,
        properties = {
                "spring.ai.openai.api-key=test-key",
                "spring.ai.openai.speech.api-key=test-key",
                "spring.ai.openai.audio.transcription.api-key=test-key"
        }
)
@AutoConfigureMockMvc
class MetadataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnModels() throws Exception {
        mockMvc.perform(get("/api/models")).andExpect(status().isOk());
    }
}
