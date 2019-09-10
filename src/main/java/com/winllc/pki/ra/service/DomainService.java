package com.winllc.pki.ra.service;

import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.DomainLinkToAccountRequest;
import com.winllc.pki.ra.domain.Domain;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.repository.DomainLinkToAccountRequestRepository;
import com.winllc.pki.ra.repository.DomainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/domain")
public class DomainService {

    @Autowired
    private DomainRepository domainRepository;
    @Autowired
    private DomainLinkToAccountRequestRepository requestRepository;
    @Autowired
    private AccountRepository accountRepository;

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

    @PostMapping("/save")
    public void saveDomain(Domain domain){
        domainRepository.save(domain);
    }

    @DeleteMapping("/delete/{id}")
    public void deleteDomain(@PathVariable Long id){
        domainRepository.deleteById(id);
    }

    /*
    Domain requests
     */

    public void createDomainRequest(DomainLinkToAccountRequest request){
        //TODO mark a domain request to be associated to an account as approved or denied
        request.setStatus("requested");
    }

    public void domainRequestDecision(Long id, String status) throws Exception {
        //todo
        Optional<DomainLinkToAccountRequest> optionalDomainLinkToAccountRequest = requestRepository.findById(id);
        if(optionalDomainLinkToAccountRequest.isPresent()){
            DomainLinkToAccountRequest request = optionalDomainLinkToAccountRequest.get();
            if(status.contentEquals("approved")){
                request.setStatus(status);

                Optional<Account> optionalAccount = accountRepository.findById(request.getAccountId());
                Optional<Domain> optionalDomain = domainRepository.findById(request.getRequestedDomainId());
                if(optionalAccount.isPresent()){
                    Account account = optionalAccount.get();
                    Domain domain = optionalDomain.get();

                    account.getCanIssueDomains().add(domain);
                }
            }

        }else{
            throw new Exception("Could not find request");
        }

    }
}
