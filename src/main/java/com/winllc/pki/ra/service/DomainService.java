package com.winllc.pki.ra.service;

import com.winllc.pki.ra.beans.form.DomainForm;
import com.winllc.pki.ra.beans.info.DomainInfo;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.DomainLinkToAccountRequest;
import com.winllc.pki.ra.domain.Domain;
import com.winllc.pki.ra.domain.User;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.repository.DomainLinkToAccountRequestRepository;
import com.winllc.pki.ra.repository.DomainRepository;
import com.winllc.pki.ra.repository.UserRepository;
import com.winllc.pki.ra.security.RAUser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import javax.validation.Valid;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/domain")
public class DomainService {

    private static final Logger log = LogManager.getLogger(DomainService.class);

    @Autowired
    private DomainRepository domainRepository;

    @GetMapping("/all")
    public ResponseEntity<?> getAllAvailableDomains(){
        List<Domain> all = domainRepository.findAll();

        return ResponseEntity.ok(all);
    }

    @GetMapping("/searchByBase/{search}")
    public ResponseEntity<?> searchDomainByBaseDomain(@PathVariable String search){
        return ResponseEntity.ok(domainRepository.findAllByBaseContains(search));
    }

    @GetMapping("/byId/{id}")
    @Transactional
    public ResponseEntity<?> getDomainById(@PathVariable Long id) throws Exception {
        Optional<Domain> optionalDomain = domainRepository.findById(id);
        if(optionalDomain.isPresent()){
            Domain domain = optionalDomain.get();
            Hibernate.initialize(domain.getCanIssueAccounts());
            return ResponseEntity.ok(new DomainInfo(domain, true));
        }else{
            throw new RAObjectNotFoundException(Domain.class, id);
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createDomain(@Valid @RequestBody DomainForm form){
        Domain domain = new Domain();
        domain.setBase(form.getBase());
        domain = domainRepository.save(domain);

        return ResponseEntity.ok(domain.getId());
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateDomain(@Valid @RequestBody DomainForm form) throws RAObjectNotFoundException{
        Optional<Domain> optionalDomain = domainRepository.findById(form.getId());
        if(optionalDomain.isPresent()){
            Domain existing = optionalDomain.get();
            existing.setBase(form.getBase());
            existing = domainRepository.save(existing);
            return ResponseEntity.ok(existing);
        }else{
            throw new RAObjectNotFoundException(form);
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteDomain(@PathVariable Long id){
        domainRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }


}
