package com.vipul.claimq.exception;

public class JobNotFoundException extends RuntimeException {

    public JobNotFoundException(long id) {
        super("Job not found: " + id);
    }
}