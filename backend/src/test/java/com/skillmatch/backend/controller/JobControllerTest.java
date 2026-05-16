package com.skillmatch.backend.controller;

import com.skillmatch.backend.dto.JobResponse;
import com.skillmatch.backend.exception.ResourceNotFoundException;
import com.skillmatch.backend.security.JwtTokenProvider;
import com.skillmatch.backend.service.JobService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SuppressWarnings("null")
@WebMvcTest(controllers = JobController.class)
@AutoConfigureMockMvc(addFilters = false)
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobService jobService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void getAllActiveJobs_returnsPageWithOkStatus() throws Exception {
        JobResponse job = new JobResponse();
        job.setId(1L);
        job.setTitle("Backend Developer");
        Page<JobResponse> page = new PageImpl<>(List.of(job));

        when(jobService.getAllActiveJobs(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/jobs").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].title").value("Backend Developer"));
    }

    @Test
    void getAllActiveJobs_emptyPage_returnsOk() throws Exception {
        when(jobService.getAllActiveJobs(any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void getRecentJobs_returnsList() throws Exception {
        JobResponse job = new JobResponse();
        job.setId(2L);
        job.setTitle("Frontend Developer");
        when(jobService.getRecentJobs()).thenReturn(List.of(job));

        mockMvc.perform(get("/api/jobs/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Frontend Developer"));
    }

    @Test
    void getJobsByCompany_returnsListForCompany() throws Exception {
        JobResponse job = new JobResponse();
        job.setId(3L);
        job.setTitle("Data Engineer");
        when(jobService.getJobsByCompany(5L)).thenReturn(List.of(job));

        mockMvc.perform(get("/api/jobs/company/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Data Engineer"));
    }

    @Test
    void getJobById_notFound_returns404() throws Exception {
        when(jobService.getJobById(99L))
                .thenThrow(new ResourceNotFoundException("Job no encontrado con ID: 99"));

        mockMvc.perform(get("/api/jobs/99"))
                .andExpect(status().isNotFound());
    }
}
