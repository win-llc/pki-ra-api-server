package com.winllc.pki.ra.service;

import com.winllc.pki.ra.domain.ServerSettings;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/settings")
public class ServerSettingsService {

    @PostMapping("/update")
    public ResponseEntity<?> updateSettings(@RequestBody ServerSettings serverSettings){
        //todo
        return null;
    }
}
