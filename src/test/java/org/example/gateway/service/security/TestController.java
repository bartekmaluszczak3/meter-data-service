package org.example.gateway.service.security;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/v1/test")
    String admin() { return "ok"; }

    @GetMapping("/public/ping")
    String publicPing() { return "ok"; }
}
