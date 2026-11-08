package com.vipul.claimq.exception;

import java.util.Set;
import java.util.TreeSet;

public class UnknownTaskTypeException extends RuntimeException {

    public UnknownTaskTypeException(String taskType, Set<String> supported) {
        super("Unknown task type '" + taskType + "'. Supported: " + new TreeSet<>(supported));
    }
}