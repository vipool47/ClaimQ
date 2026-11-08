package com.vipul.claimq;


import com.vipul.claimq.model.Job;
import com.vipul.claimq.service.JobClaimService;
import com.vipul.claimq.service.JobLifecycleService;
import com.vipul.claimq.worker.JobProcessor;
import com.vipul.claimq.worker.JobWorker;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.UUID;


@SpringBootApplication
public class ClaimqApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClaimqApplication.class, args);
    }

//    @Bean
//    CommandLineRunner startWorker(JobClaimService claimService,
//                                  JobProcessor processor,
//                                  JobLifecycleService lifecycle) {
//        return args -> {
//            Thread thread = new Thread(
//                    new JobWorker("worker-1", 1000, claimService, processor, lifecycle),
//                    "worker-1");
//            thread.setDaemon(true);
//            thread.start();
//        };
//    }
}

