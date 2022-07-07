package com.example.springcloud.eurekaserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EurekaServiceApplicationTests {
    @Test
    void contextLoads() {}

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    void catalogLoads() {
        String expectedResponse = "{\"applications\":{\"versions__delta\":\"1\",\"apps__hashcode\":\"\",\"application\":[]}}";
        ResponseEntity<String> entity = testRestTemplate.getForEntity("/eureka/apps", String.class);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(expectedResponse, entity.getBody());
    }

    @Test
    void healthy() {
        String expectedResponseBody = "{\"status\":\"UP\"}";
        ResponseEntity<String> entity = testRestTemplate.getForEntity("/actuator/health", String.class);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(expectedResponseBody, entity.getBody());
    }
}
