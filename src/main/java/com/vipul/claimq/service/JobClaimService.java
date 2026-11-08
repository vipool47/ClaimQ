package com.vipul.claimq.service;

import com.vipul.claimq.model.Job;
import com.vipul.claimq.repository.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class JobClaimService {

    private final JobRepository jobRepository;

    public JobClaimService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Transactional
    public Optional<Job> claimNext(String workerId) {
        return jobRepository.claimNext(workerId);
    }
}