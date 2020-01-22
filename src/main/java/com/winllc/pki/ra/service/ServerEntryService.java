package com.winllc.pki.ra.service;

import com.nimbusds.jose.util.Base64;
import com.winllc.pki.ra.beans.*;
import com.winllc.pki.ra.beans.info.ServerEntryInfo;
import com.winllc.pki.ra.beans.validator.ServerEntryFormValidator;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.Domain;
import com.winllc.pki.ra.domain.ServerEntry;
import com.winllc.pki.ra.domain.User;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.repository.DomainRepository;
import com.winllc.pki.ra.repository.ServerEntryRepository;
import com.winllc.pki.ra.repository.UserRepository;
import com.winllc.pki.ra.security.RAUser;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/serverEntry")
public class ServerEntryService {

    private static final Logger log = LogManager.getLogger(ServerEntryService.class);

    @Autowired
    private ServerEntryRepository serverEntryRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private DomainRepository domainRepository;
    @Autowired
    private KeycloakService keycloakService;
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/create")
    public ResponseEntity<?> createServerEntry(@RequestBody ServerEntryForm form){

        Optional<Account> optionalAccount = accountRepository.findById(form.getAccountId());

        if(optionalAccount.isPresent()){
            Account account = optionalAccount.get();

            List<Domain> canIssueDomains = domainRepository.findAllByCanIssueAccountsContains(account);
            Optional<Domain> optionalDomain = canIssueDomains.stream()
                    .filter(d -> form.getFqdn().endsWith(d.getBase()))
                    .findAny();

            if(optionalDomain.isPresent()){
                //String hostname = form.getFqdn().split(".")[0];

                Domain domain = optionalDomain.get();
                ServerEntry entry = new ServerEntry();
                entry.setAccount(account);
                entry.setDomainParent(domain);
                entry.setFqdn(form.getFqdn());
                entry.setHostname(form.getFqdn());

                entry = serverEntryRepository.save(entry);

                log.info("Created a Server Entry: "+entry);

                return ResponseEntity.ok(entry.getId());
            }else{
                return ResponseEntity.status(403).build();
            }
        }else{
            return ResponseEntity.noContent().build();
        }

    }

    @PostMapping("/update")
    public ResponseEntity<?> updateServerEntry(@RequestBody ServerEntryForm form){
        //todo

        Optional<ServerEntry> optionalServerEntry = serverEntryRepository.findById(form.getId());
        if(optionalServerEntry.isPresent()){
            ServerEntry serverEntry = optionalServerEntry.get();

            boolean isValid = new ServerEntryFormValidator().validate(form, true);
            if(isValid){
                serverEntry.setAlternateDnsValues(form.getAlternateDnsValues());
                serverEntry.setOpenidClientRedirectUrl(form.getOpenidClientRedirectUrl());

                serverEntry = serverEntryRepository.save(serverEntry);

                return ResponseEntity.ok(serverEntry);
            }else{
                log.error("Invalid Server Entry form");
                return ResponseEntity.badRequest().build();
            }

        }else{
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/byId/{id}")
    @Transactional
    public ResponseEntity<?> getServerEntry(@PathVariable Long id){
        Optional<ServerEntry> entryOptional = serverEntryRepository.findById(id);

        if(entryOptional.isPresent()){
            ServerEntry entry = entryOptional.get();
            Hibernate.initialize(entry.getAlternateDnsValues());
            ServerEntryInfo serverEntryInfo = new ServerEntryInfo(entry);

            return ResponseEntity.ok(serverEntryInfo);
        }else{
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/allByAccountId/{accountId}")
    public ResponseEntity<?> getAllServerEntriesForAccount(@PathVariable Long accountId){

        List<ServerEntry> allByAccountId = serverEntryRepository.findAllByAccountId(accountId);

        return ResponseEntity.ok(allByAccountId);
    }

    @GetMapping("/allForUser")
    @Transactional
    public ResponseEntity<?> getAllServerEntriesForUser(@AuthenticationPrincipal RAUser raUser){

        Optional<User> optionalUser = userRepository.findOneByUsername(raUser.getUsername());

        if(optionalUser.isPresent()){
            User user = optionalUser.get();

            List<Account> allByAccountUsersContains = accountRepository.findAllByAccountUsersContains(user);

            List<ServerEntryInfo> entries = new ArrayList<>();
            for(Account account : allByAccountUsersContains) {
                List<ServerEntryInfo> temp = serverEntryRepository.findAllByAccountId(account.getId())
                        .stream().map(i -> {
                            Hibernate.initialize(i.getAlternateDnsValues());
                            return new ServerEntryInfo(i);
                        })
                        .collect(Collectors.toList());
                entries.addAll(temp);
            }

            return ResponseEntity.ok(entries);
        }else{
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/enableForOIDConnect")
    public ResponseEntity<?> enableForOIDConnect(@RequestBody ServerEntryForm form){
        //todo

        Optional<ServerEntry> serverEntryOptional = serverEntryRepository.findById(form.getId());
        if(serverEntryOptional.isPresent()){
            ServerEntry serverEntry = serverEntryOptional.get();

            try {
                serverEntry = keycloakService.createClient(serverEntry);

                if(serverEntry != null){
                    serverEntry.setOpenidClientRedirectUrl(form.getOpenidClientRedirectUrl());
                    serverEntry = serverEntryRepository.save(serverEntry);

                    return ResponseEntity.ok(serverEntry);
                }else{
                    return ResponseEntity.badRequest().build();
                }
            } catch (Exception e) {
                log.error(e.getMessage());
                return ResponseEntity.status(500).build();
            }
        }else{
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/disableForOIDConnect")
    public ResponseEntity<?> disableForOIDConnect(@RequestBody ServerEntryForm form){
        //todo

        Optional<ServerEntry> optionalServerEntry = serverEntryRepository.findById(form.getId());

        if(optionalServerEntry.isPresent()){
            ServerEntry serverEntry = optionalServerEntry.get();
            boolean deleted = keycloakService.deleteClient(serverEntry);
            if(deleted){
                return ResponseEntity.ok().build();
            }else {
                log.error("Did not delete the OIDC client");
                return ResponseEntity.status(500).build();
            }

        }else{
            return ResponseEntity.noContent().build();
        }

    }

    @PostMapping("/buildDeploymentPackage")
    public ResponseEntity<?> buildDeploymentPackage(@RequestBody ServerEntryForm form){

        Optional<ServerEntry> serverEntryOptional = serverEntryRepository.findById(form.getId());
        if(serverEntryOptional.isPresent()) {
            ServerEntry serverEntry = serverEntryOptional.get();
            Optional<Account> optionalAccount = accountRepository.findById(serverEntry.getAccount().getId());
            if(optionalAccount.isPresent()){
                Account account = optionalAccount.get();
                ServerEntryDockerDeploymentFile deploymentFile = buildDeploymentFile(serverEntry, account);
                return ResponseEntity.ok(deploymentFile.buildContent());
            }
        }

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteServerEntry(@PathVariable Long id){

        Optional<ServerEntry> serverEntryOptional = serverEntryRepository.findById(id);
        if(serverEntryOptional.isPresent()){
            ServerEntry serverEntry = serverEntryOptional.get();

            //if server entry is deleted, remove the OIDC client if it exists
            if(StringUtils.isNotBlank(serverEntry.getOpenidClientId())){
                keycloakService.deleteClient(serverEntry);
            }

            serverEntryRepository.deleteById(id);

            return ResponseEntity.ok().build();
        }else{
            return ResponseEntity.noContent().build();
        }
    }

    private ServerEntryDockerDeploymentFile buildDeploymentFile(ServerEntry serverEntry, Account account){
        AcmeClientDetails acmeClientDetails = new AcmeClientDetails();
        acmeClientDetails.setAcmeEabHmacKeyValue(Base64.encode(account.getMacKey()).toString());
        acmeClientDetails.setAcmeKidValue(account.getKeyIdentifier());
        //todo make dynamic
        acmeClientDetails.setAcmeServerValue("http://192.168.1.13:8181/acme/directory");

        OIDCClientDetails oidcClientDetails = keycloakService.getClient(serverEntry);

        ServerEntryDockerDeploymentFile dockerDeploymentFile = new ServerEntryDockerDeploymentFile();
        dockerDeploymentFile.setAcmeClientDetails(acmeClientDetails);
        dockerDeploymentFile.setOidcClientDetails(oidcClientDetails);
        //todo make dynamic
        //dockerDeploymentFile.setProxyAddressValue("http://192.168.1.13:8282/test");
        dockerDeploymentFile.setProxyAddressValue(serverEntry.getOpenidClientRedirectUrl());
        dockerDeploymentFile.setServerNameValue(serverEntry.getFqdn());
        return dockerDeploymentFile;
    }
}
