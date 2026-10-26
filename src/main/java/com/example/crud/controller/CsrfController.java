package com.example.crud.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestAttribute;

@RestController
public class CsrfController {
    @GetMapping("/csrf")
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "basicAuth")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
    public CsrfResponse csrf(@RequestAttribute("rawCsrfToken") CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getToken());
    }

    public record CsrfResponse(String headerName, String token) { }
}
