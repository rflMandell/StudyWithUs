package com.studywithus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class StudyWithUsApplicationTests {

    @Test
    @DisplayName("Application context should load successfully")
    void contextLoads() {
    }
}
