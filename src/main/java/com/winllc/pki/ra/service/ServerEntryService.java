package com.winllc.pki.ra.service;

import com.nimbusds.jose.util.Base64;
import com.winllc.pki.ra.beans.*;
import com.winllc.pki.ra.beans.form.ServerEntryForm;
import com.winllc.pki.ra.beans.info.ServerEntryInfo;
import com.winllc.pki.ra.beans.validator.ServerEntryFormValidator;
import com.winllc.pki.ra.beans.validator.ValidationResponse;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.Domain;
import com.winllc.pki.ra.domain.ServerEntry;
import com.winllc.pki.ra.domain.User;
import com.winllc.pki.ra.exception.RAException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.repository.DomainRepository;
import com.winllc.pki.ra.repository.ServerEntryRepository;
import com.winllc.pki.ra.repository.UserRepository;
import com.winllc.pki.ra.security.RAUser;
import com.winllc.pki.ra.service.external.KeycloakService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import javax.validation.Valid;
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
    @ResponseStatus(HttpStatus.CREATED)
    public Long createServerEntry(@Valid @RequestBody ServerEntryForm form) throws RAObjectNotFoundException {

        Optional<Account> optionalAccount = accountRepository.findById(form.getAccountId());

        if(optionalAccount.isPresent()){
            Account account = optionalAccount.get();

            List<Domain> canIssueDomains = domainRepository.findAllByCanIssueAccountsContains(account);
            Optional<Domain> optionalDomain = canIssueDomains.stream()
                    .filter(d -> form.getFqdn().endsWith(d.getBase()))
                    .findAny();

            if(optionalDomain.isPresent()){
                Domain domain = optionalDomain.get();
                ServerEntry entry = new ServerEntry();
                entry.setAccount(account);
                entry.setDomainParent(domain);
                entry.setFqdn(form.getFqdn());
                entry.setHostname(form.getFqdn());

                entry = serverEntryRepository.save(entry);

                log.info("Created a Server Entry: "+entry);

                return entry.getId();
            }else{
                throw new RAObjectNotFoundException(Domain.class, form.getFqdn());
            }
        }else{
            throw new RAObjectNotFoundException(Account.class, form.getAccountId());
        }

    }

    @PostMapping("/update")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public ServerEntryInfo updateServerEntry(@Valid @RequestBody ServerEntryForm form) throws RAException {
        //todo update attributes in directory

        log.info("Is account linked: "+form.isAccountLinkedForm());

        Optional<ServerEntry> optionalServerEntry = serverEntryRepository.findById(form.getId());
        if(optionalServerEntry.isPresent()){
            ServerEntry serverEntry = optionalServerEntry.get();

            ValidationResponse validationResponse = new ServerEntryFormValidator().validate(form, true);
            if(validationResponse.isValid()){
                serverEntry.setAlternateDnsValues(form.getAlternateDnsValues());
                serverEntry.setOpenidClientRedirectUrl(form.getOpenidClientRedirectUrl());

                List<String> alternateDnsValues = new ArrayList<>(serverEntry.getAlternateDnsValues());
                serverEntry.setAlternateDnsValues(alternateDnsValues);

                serverEntry = serverEntryRepository.save(serverEntry);

                return entryToInfo(serverEntry);
            }else{
                throw new RAException("Invalid Server Entry form");
            }
        }else{
            throw new RAObjectNotFoundException(form);
        }
    }

    @GetMapping("/byId/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public ServerEntryInfo getServerEntry(@PathVariable Long id) throws RAObjectNotFoundException {
        Optional<ServerEntry> entryOptional = serverEntryRepository.findById(id);

        if(entryOptional.isPresent()){
            ServerEntry entry = entryOptional.get();

            return entryToInfo(entry);
        }else{
            throw new RAObjectNotFoundException(ServerEntry.class, id);
        }
    }

    @GetMapping("/allByAccountId/{accountId}")
    @ResponseStatus(HttpStatus.OK)
    public List<ServerEntry> getAllServerEntriesForAccount(@PathVariable Long accountId){

        List<ServerEntry> allByAccountId = serverEntryRepository.findAllByAccountId(accountId);

        return allByAccountId;
    }

    @GetMapping("/allForUser")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public List<ServerEntryInfo> getAllServerEntriesForUser(@AuthenticationPrincipal UserDetails raUser) throws RAObjectNotFoundException {

        Optional<User> optionalUser = userRepository.findOneByUsername(raUser.getUsername());

        if(optionalUser.isPresent()){
            User user = optionalUser.get();

            List<Account> allByAccountUsersContains = accountRepository.findAllByAccountUsersContains(user);

            List<ServerEntryInfo> entries = new ArrayList<>();
            for(Account account : allByAccountUsersContains) {
                List<ServerEntryInfo> temp = serverEntryRepository.findAllByAccountId(account.getId())
                        .stream().map(i -> {
                            return entryToInfo(i);
                        })
                        .collect(Collectors.toList());
                entries.addAll(temp);
            }

            return entries;
        }else{
            throw new RAObjectNotFoundException(User.class, raUser.getUsername());
        }
    }

    @PostMapping("/enableForOIDConnect")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public ServerEntryInfo enableForOIDConnect(@RequestBody ServerEntryForm form) throws RAException {
        //todo

        Optional<ServerEntry> serverEntryOptional = serverEntryRepository.findById(form.getId());
        if(serverEntryOptional.isPresent()){
            ServerEntry serverEntry = serverEntryOptional.get();

            try {
                //todo generify
                serverEntry = keycloakService.createClient(serverEntry);

                if(serverEntry != null){
                    serverEntry.setOpenidClientRedirectUrl(form.getOpenidClientRedirectUrl());
                    Hibernate.initialize(serverEntry.getAlternateDnsValues());
                    serverEntry = serverEntryRepository.save(serverEntry);

                    return entryToInfo(serverEntry);
                }else{
                    throw new RAException("Could not create OIDC client");
                }
            } catch (Exception e) {
                throw new RAException(e.getMessage());
            }
        }else{
            throw new RAObjectNotFoundException(form);
        }
    }

    @PostMapping("/disableForOIDConnect")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    public ServerEntryInfo disableForOIDConnect(@RequestBody ServerEntryForm form) throws RAException {
        //todo

        Optional<ServerEntry> optionalServerEntry = serverEntryRepository.findById(form.getId());

        if(optionalServerEntry.isPresent()){
            ServerEntry serverEntry = optionalServerEntry.get();
            serverEntry = keycloakService.deleteClient(serverEntry);
            if(serverEntry != null){
                return entryToInfo(serverEntry);
            }else {
                throw new RAException("Did not delete the OIDC client");
            }

        }else{
            throw new RAObjectNotFoundException(form);
        }

    }

    @PostMapping("/buildDeploymentPackage")
    @ResponseStatus(HttpStatus.OK)
    public List<String> buildDeploymentPackage(@RequestBody ServerEntryForm form) throws RAObjectNotFoundException {

        Optional<ServerEntry> serverEntryOptional = serverEntryRepository.findById(form.getId());
        if(serverEntryOptional.isPresent()) {
            ServerEntry serverEntry = serverEntryOptional.get();
            Optional<Account> optionalAccount = accountRepository.findById(serverEntry.getAccount().getId());
            if(optionalAccount.isPresent()){
                Account account = optionalAccount.get();
                ServerEntryDockerDeploymentFile deploymentFile = buildDeploymentFile(serverEntry, account);
                return deploymentFile.buildContent();
            }else{
                throw new RAObjectNotFoundException(Account.class, serverEntry.getAccount().getId());
            }
        }else{
            throw new RAObjectNotFoundException(form);
        }
    }

    @DeleteMapping("/delete/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteServerEntry(@PathVariable Long id) throws RAException {

        Optional<ServerEntry> serverEntryOptional = serverEntryRepository.findById(id);
        if(serverEntryOptional.isPresent()){
            ServerEntry serverEntry = serverEntryOptional.get();

            //if server entry is deleted, remove the OIDC client if it exists
            if(StringUtils.isNotBlank(serverEntry.getOpenidClientId())){
                keycloakService.deleteClient(serverEntry);
            }

            serverEntryRepository.deleteById(id);
        }else{
            throw new RAObjectNotFoundException(ServerEntry.class, id);
        }
    }

    private ServerEntryInfo entryToInfo(ServerEntry entry){
        Hibernate.initialize(entry.getAlternateDnsValues());
        ServerEntryInfo serverEntryInfo = new ServerEntryInfo(entry);
        return serverEntryInfo;
    }

    private ServerEntryDockerDeploymentFile buildDeploymentFile(ServerEntry serverEntry, Account account){
        AcmeClientDetails acmeClientDetails = new AcmeClientDetails();
        acmeClientDetails.setAcmeEabHmacKeyValue(Base64.encode(account.getMacKey()).toString());
        acmeClientDetails.setAcmeKidValue(account.getKeyIdentifier());
        //todo make dynamic
        acmeClientDetails.setAcmeServerValue("http://192.168.1.202:8181/acme/directory");

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
