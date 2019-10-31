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
    public List<Domain> getAllAvailableDomains(){
        //TODO
        return domainRepository.findAll();
    }

    @GetMapping("/searchByBase/{search}")
    public List<Domain> searchDomainByBaseDomain(@PathVariable String search){
        return domainRepository.findAllByBaseContains(search);
    }

    @GetMapping("/byId/{id}")
    public Domain getDomainById(@PathVariable Long id) throws Exception {
        Optional<Domain> optionalDomain = domainRepository.findById(id);
        if(optionalDomain.isPresent()){
            return optionalDomain.get();
        }

        throw new Exception("Could not find by ID: "+id);
    }

    @PostMapping("/create")
    public void createDomain(@RequestBody Domain domain){
        domainRepository.save(domain);
    }

    @PostMapping("/update")
    public void updateDomain(@RequestBody Domain domain){
        try {
            Domain domainById = getDomainById(domain.getId());
            if(domainById != null){
                domainById.setBase(domain.getBase());
                domainRepository.save(domain);
            }
        } catch (Exception e) {
            log.error("Could not find domain: "+domain.getId(), e);
        }
    }

    @DeleteMapping("/delete/{id}")
    public void deleteDomain(@PathVariable Long id){
        domainRepository.deleteById(id);
    }


}
