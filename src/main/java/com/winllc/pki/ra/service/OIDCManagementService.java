package com.winllc.pki.ra.service;

import com.nimbusds.jose.util.Base64;
import com.winllc.pki.ra.beans.AcmeClientDetails;
import com.winllc.pki.ra.beans.OIDCClientDetails;
import com.winllc.pki.ra.beans.ServerEntryDockerDeploymentFile;
import com.winllc.pki.ra.beans.form.ServerEntryForm;
import com.winllc.pki.ra.beans.info.ServerEntryInfo;
import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.ServerEntry;
import com.winllc.pki.ra.exception.RAException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.repository.ServerEntryRepository;
import com.winllc.pki.ra.service.external.vendorimpl.KeycloakOIDCProviderConnection;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/serverEntry")
public class OIDCManagementService {

    //todo replace with OIDCProviderService
    private final KeycloakOIDCProviderConnection oidcProviderConnection;
    private final ServerEntryRepository serverEntryRepository;
    private final AccountRepository accountRepository;

    public OIDCManagementService(KeycloakOIDCProviderConnection oidcProviderConnection, ServerEntryRepository serverEntryRepository, AccountRepository accountRepository) {
        this.oidcProviderConnection = oidcProviderConnection;
        this.serverEntryRepository = serverEntryRepository;
        this.accountRepository = accountRepository;
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
                serverEntry = oidcProviderConnection.createClient(serverEntry);

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

        Optional<ServerEntry> optionalServerEntry = serverEntryRepository.findById(form.getId());

        if(optionalServerEntry.isPresent()){
            ServerEntry serverEntry = optionalServerEntry.get();
            serverEntry = oidcProviderConnection.deleteClient(serverEntry);
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

    private ServerEntryDockerDeploymentFile buildDeploymentFile(ServerEntry serverEntry, Account account){
        AcmeClientDetails acmeClientDetails = new AcmeClientDetails();
        acmeClientDetails.setAcmeEabHmacKeyValue(Base64.encode(account.getMacKey()).toString());
        acmeClientDetails.setAcmeKidValue(account.getKeyIdentifier());
        //todo make dynamic
        acmeClientDetails.setAcmeServerValue("http://192.168.1.202:8181/acme/directory");

        OIDCClientDetails oidcClientDetails = oidcProviderConnection.getClient(serverEntry);

        ServerEntryDockerDeploymentFile dockerDeploymentFile = new ServerEntryDockerDeploymentFile();
        dockerDeploymentFile.setAcmeClientDetails(acmeClientDetails);
        dockerDeploymentFile.setOidcClientDetails(oidcClientDetails);
        //todo make dynamic
        //dockerDeploymentFile.setProxyAddressValue("http://192.168.1.13:8282/test");
        dockerDeploymentFile.setProxyAddressValue(serverEntry.getOpenidClientRedirectUrl());
        dockerDeploymentFile.setServerNameValue(serverEntry.getFqdn());
        return dockerDeploymentFile;
    }

    private ServerEntryInfo entryToInfo(ServerEntry entry){
        Hibernate.initialize(entry.getAlternateDnsValues());
        return new ServerEntryInfo(entry);
    }
}
