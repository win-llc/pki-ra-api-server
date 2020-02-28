package com.winllc.pki.ra.service;

import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.DomainLinkToAccountRequest;
import com.winllc.pki.ra.domain.Domain;
import com.winllc.pki.ra.domain.User;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.repository.DomainLinkToAccountRequestRepository;
import com.winllc.pki.ra.repository.DomainRepository;
import com.winllc.pki.ra.repository.UserRepository;
import com.winllc.pki.ra.security.RAUser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    public ResponseEntity<?> getDomainById(@PathVariable Long id) throws Exception {
        Optional<Domain> optionalDomain = domainRepository.findById(id);
        if(optionalDomain.isPresent()){
            return ResponseEntity.ok(optionalDomain.get());
        }

        throw new Exception("Could not find by ID: "+id);
    }

    @PostMapping("/create")
    public ResponseEntity<?> createDomain(@Valid @RequestBody Domain domain){
        domain = domainRepository.save(domain);

        return ResponseEntity.ok(domain.getId());
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateDomain(@Valid @RequestBody Domain domain){
        try {
            Optional<Domain> optionalDomain = domainRepository.findById(domain.getId());
            if(optionalDomain.isPresent()){
                Domain existing = optionalDomain.get();
                existing.setBase(domain.getBase());
                existing = domainRepository.save(existing);
                return ResponseEntity.ok(existing);
            }else{
                return ResponseEntity.noContent().build();
            }
        } catch (Exception e) {
            log.error("Could not find domain: "+domain.getId(), e);
        }
        return ResponseEntity.status(500).build();
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteDomain(@PathVariable Long id){
        domainRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }


}
