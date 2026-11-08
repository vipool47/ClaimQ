package com.vipul.claimq;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @Autowired
    protected JdbcTemplate jdbc;

    @BeforeEach
    protected void cleanJobsTable() {
        jdbc.execute("TRUNCATE TABLE jobs RESTART IDENTITY");
    }
}