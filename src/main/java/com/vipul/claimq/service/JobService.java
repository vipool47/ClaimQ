package com.vipul.claimq.service;

import com.vipul.claimq.dto.CreateJobRequest;
import com.vipul.claimq.dto.PageResponse;
import com.vipul.claimq.exception.JobNotFoundException;
import com.vipul.claimq.exception.UnknownTaskTypeException;
import com.vipul.claimq.handler.JobHandlerRegistry;
import com.vipul.claimq.model.Job;
import com.vipul.claimq.model.JobStatus;
import com.vipul.claimq.repository.JobRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JobService {

    private static final int DEFAULT_MAX_ATTEMPTS = 3;

    private final JobRepository jobRepository;
    private final JobHandlerRegistry handlerRegistry;

    public JobService(JobRepository jobRepository, JobHandlerRegistry handlerRegistry) {
        this.jobRepository = jobRepository;
        this.handlerRegistry = handlerRegistry;
    }

    public Job submit(CreateJobRequest request) {
        if (!handlerRegistry.supports(request.taskType())) {
            throw new UnknownTaskTypeException(request.taskType(), handlerRegistry.taskTypes());
        }

        int maxAttempts = request.maxAttempts() != null
                ? request.maxAttempts()
                : DEFAULT_MAX_ATTEMPTS;

        return jobRepository.insert(
                request.taskType(),
                request.payload().toString(),
                maxAttempts
        );
    }

    public Job getById(long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException(id));
    }

    public PageResponse<Job> list(JobStatus status, int page, int size) {
        List<Job> jobs = jobRepository.findAll(status, size, (long) page * size);
        long total = jobRepository.count(status);
        int totalPages = (int) ((total + size - 1) / size);
        return new PageResponse<>(jobs, page, size, total, totalPages);
    }
}