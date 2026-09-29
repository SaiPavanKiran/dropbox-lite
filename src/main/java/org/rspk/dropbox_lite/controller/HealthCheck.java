package org.rspk.dropbox_lite.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController /* can be commented */
public class HealthCheck {

    @GetMapping("/health")
    ResponseEntity<?> healthCheck(){
        return ResponseEntity.ok("Welcome to dropbox-lite");
    }

}
