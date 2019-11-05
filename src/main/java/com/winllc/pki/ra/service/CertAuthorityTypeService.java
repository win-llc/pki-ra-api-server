package com.winllc.pki.ra.service;

import com.winllc.pki.ra.domain.CertAuthorityType;
import com.winllc.pki.ra.repository.CertAuthorityTypeRepository;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import java.util.List;

@Service
@RequestMapping("/api/certAuthorityType")
public class CertAuthorityTypeService {

    @Autowired
    private CertAuthorityTypeRepository repository;

    @PostConstruct
    private void init(){
        CertAuthorityType type = new CertAuthorityType();
        type.setName("winllc");
        type.setId(1l);

        repository.save(type);

        type.setId(2l);
        type.setName("internal");

        repository.save(type);
    }

    @Transactional
    @GetMapping("/all")
    public ResponseEntity<?> getAll(){

        List<CertAuthorityType> all = repository.findAll();
        for(CertAuthorityType type : all){
            Hibernate.initialize(type.getRequiredSettings());
        }

        return ResponseEntity.ok(all);
    }
}
