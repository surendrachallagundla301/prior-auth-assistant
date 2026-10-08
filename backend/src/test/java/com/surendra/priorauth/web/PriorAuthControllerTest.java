package com.surendra.priorauth.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PriorAuthControllerTest {

    private static final String BODY = """
            {
              "patientName": "Jane Doe",
              "memberId": "ABC123456789",
              "dateOfBirth": "1985-04-12",
              "payer": "ACME_HEALTH",
              "cptCode": "27447",
              "icd10Codes": ["M17.11"],
              "submittedDocuments": ["PHYSICIAN_ORDER", "CLINICAL_NOTES", "IMAGING_REPORT", "PRIOR_TREATMENT_HISTORY"]
            }
            """;

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;

    private long createRequest() throws Exception {
        MvcResult result = mvc.perform(post("/api/requests").with(httpBasic("intake", "intake-demo"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberId").value("********6789"))
                .andExpect(jsonPath("$.patientName").value("J*** D**"))
                .andExpect(jsonPath("$.dateOfBirth").value("****-**-**"))
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void checkFlagsPayerSpecificMissingDocument() throws Exception {
        long id = createRequest();

        mvc.perform(post("/api/requests/{id}/check", id).with(httpBasic("reviewer", "reviewer-demo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readyForSubmission").value(false))
                .andExpect(jsonPath("$.missingDocuments[0]").value("MEDICAL_NECESSITY_LETTER"))
                .andExpect(jsonPath("$.denialRisk").value("MEDIUM"));

        mvc.perform(get("/api/requests/{id}", id).with(httpBasic("reviewer", "reviewer-demo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEEDS_DOCUMENTATION"));
    }

    @Test
    void phiIsEncryptedAtRest() throws Exception {
        long id = createRequest();

        String storedMemberId = jdbc.queryForObject(
                "select member_id from prior_auth_request where id = ?", String.class, id);
        String storedName = jdbc.queryForObject(
                "select patient_name from prior_auth_request where id = ?", String.class, id);

        assertThat(storedMemberId).isNotBlank().doesNotContain("ABC123456789");
        assertThat(storedName).doesNotContain("Jane");
    }

    @Test
    void everyActionIsAudited() throws Exception {
        long id = createRequest();
        mvc.perform(post("/api/requests/{id}/check", id).with(httpBasic("intake", "intake-demo")))
                .andExpect(status().isOk());

        mvc.perform(get("/api/audit").param("requestId", String.valueOf(id))
                        .with(httpBasic("auditor", "auditor-demo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("REQUEST_CREATED"))
                .andExpect(jsonPath("$[1].action").value("DOCUMENTATION_CHECKED"))
                .andExpect(jsonPath("$[0].actor").value("intake"));
    }

    @Test
    void rolesAreEnforced() throws Exception {
        mvc.perform(post("/api/requests").with(httpBasic("reviewer", "reviewer-demo"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/audit").with(httpBasic("intake", "intake-demo")))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/requests/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validationErrorsDoNotEchoPhi() throws Exception {
        String bad = BODY.replace("ABC123456789", "12345");

        MvcResult result = mvc.perform(post("/api/requests").with(httpBasic("intake", "intake-demo"))
                        .contentType(MediaType.APPLICATION_JSON).content(bad))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("memberId").doesNotContain("12345");
    }
}
