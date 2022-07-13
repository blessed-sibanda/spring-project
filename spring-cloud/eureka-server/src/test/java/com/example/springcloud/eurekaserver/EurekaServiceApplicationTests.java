package com.example.springcloud.eurekaserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EurekaServiceApplicationTests {
    @Value("${app.eureka-username}") private String username;
    @Value("${app.eureka-password}") private String password;

    @Test
    void contextLoads() {}

    private TestRestTemplate testRestTemplate;

    @Autowired
    public void setTestRestTemplate(TestRestTemplate testRestTemplate) {
        this.testRestTemplate=testRestTemplate.withBasicAuth(username, password);
    }

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
