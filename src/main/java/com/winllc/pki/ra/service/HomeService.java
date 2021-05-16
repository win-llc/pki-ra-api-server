package com.winllc.pki.ra.service;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class HomeService {

    @GetMapping
    public String get(){
        return "OK";
    }
}
