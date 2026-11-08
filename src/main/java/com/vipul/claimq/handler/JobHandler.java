package com.vipul.claimq.handler;

public interface JobHandler {

    String getTaskType();

    void execute(String payload) throws Exception;
}