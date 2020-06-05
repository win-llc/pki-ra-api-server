package com.winllc.pki.ra.service;

import com.winllc.pki.ra.domain.CertAuthorityType;
import com.winllc.pki.ra.repository.CertAuthorityTypeRepository;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/certAuthorityType")
public class CertAuthorityTypeService {

    @Autowired
    private CertAuthorityTypeRepository repository;

    //@PostConstruct
    private void init(){
        Optional<CertAuthorityType> typeOptional = repository.findDistinctByName("winllc");
        if(!typeOptional.isPresent()) {
            CertAuthorityType type = new CertAuthorityType();
            type.setName("winllc");
            repository.save(type);
        }

        typeOptional = repository.findDistinctByName("internal");
        if(!typeOptional.isPresent()) {
            CertAuthorityType type2 = new CertAuthorityType();
            type2.setName("internal");
            repository.save(type2);
        }
    }

    @Transactional
    @GetMapping("/all")
    @ResponseStatus(HttpStatus.OK)
    public List<CertAuthorityType> getAll(){

        List<CertAuthorityType> all = repository.findAll();
        for(CertAuthorityType type : all){
            Hibernate.initialize(type.getRequiredSettings());
        }

        return all;
    }


}
