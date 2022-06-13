package uk.gov.companieshouse.charges.delta.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    @GetMapping("/healthcheck")
    public ResponseEntity<String> healthcheck() {
        //TODO: Introduce spring actuator
        return ResponseEntity.status(HttpStatus.OK).body("I am healthy");
    }

}