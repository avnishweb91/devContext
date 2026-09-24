package com.devcontext.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsDashboardMetricsAndPullRequests() throws Exception {
        mockMvc.perform(get("/api/dashboard").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metrics.openPullRequests").value(24))
                .andExpect(jsonPath("$.metrics.contextCoverage").value(87))
                .andExpect(jsonPath("$.pullRequests.length()").value(3))
                .andExpect(jsonPath("$.pullRequests[0].status").value("VERIFIED"));
    }

    @Test
    void runsVerificationAndReturnsReviewCount() throws Exception {
        mockMvc.perform(post("/api/verification/run").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.reviewItems").value(2));
    }
}
