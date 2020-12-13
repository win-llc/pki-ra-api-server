package com.winllc.pki.ra.service;

import com.winllc.pki.ra.domain.EstServerProperties;
import com.winllc.pki.ra.repository.EstServerPropertiesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/estServerManagement")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class EstServerManagementService {

    @Autowired
    private EstServerPropertiesRepository estServerPropertiesRepository;

    @GetMapping("/properties/byName/{name}")
    public EstServerProperties getProperties(@PathVariable String name){
        return estServerPropertiesRepository.findByName(name);
    }

    @GetMapping("/properties/all")
    public List<EstServerProperties> getAll(){
        return estServerPropertiesRepository.findAll();
    }

    @PostMapping("/properties/save")
    public EstServerProperties save(@RequestBody EstServerProperties properties){
        return estServerPropertiesRepository.save(properties);
    }

    @PostMapping("/properties/delete/{name}")
    public void delete(@PathVariable String name){
        EstServerProperties properties = estServerPropertiesRepository.findByName(name);
        if(properties != null){
            estServerPropertiesRepository.delete(properties);
        }
    }
}
