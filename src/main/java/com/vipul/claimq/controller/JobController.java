package com.vipul.claimq.controller;

import com.vipul.claimq.dto.CreateJobRequest;
import com.vipul.claimq.dto.JobResponse;
import com.vipul.claimq.dto.PageResponse;
import com.vipul.claimq.model.Job;
import com.vipul.claimq.model.JobStatus;
import com.vipul.claimq.service.JobService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<JobResponse> submit(@Valid @RequestBody CreateJobRequest request) {
        Job job = jobService.submit(request);
        return ResponseEntity
                .accepted()
                .location(URI.create("/api/jobs/" + job.id()))
                .body(JobResponse.from(job));
    }

    @GetMapping("/{id}")
    public JobResponse get(@PathVariable long id) {
        return JobResponse.from(jobService.getById(id));
    }

    @GetMapping
    public PageResponse<JobResponse> list(
            @RequestParam(required = false) JobStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        return jobService.list(status, page, size).map(JobResponse::from);
    }
}