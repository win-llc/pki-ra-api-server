package com.winllc.pki.ra.security;

import com.winllc.pki.ra.beans.form.DomainForm;
import com.winllc.pki.ra.beans.form.ServerEntryForm;
import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.*;
import com.winllc.pki.ra.repository.AccountRepository;
import com.winllc.pki.ra.repository.PocEntryRepository;
import com.winllc.pki.ra.repository.ServerEntryRepository;
import com.winllc.pki.ra.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
class CustomPermissionEvaluatorTest {

    @Autowired
    private CustomPermissionEvaluator customPermissionEvaluator;
    @Autowired
    private PocEntryRepository pocEntryRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private ServerEntryRepository serverEntryRepository;

    @Test
    void hasPermission() {
        //todo
        Account account = new Account();
        account.setProjectName("Test Project");
        account.setKeyIdentifier("kidtest1");
        account.setMacKey("testmac1");

        account = accountRepository.save(account);

        PocEntry pocEntry = PocEntry.buildNew("test@test.com", account);
        pocEntryRepository.save(pocEntry);

        List<String> permissions = new ArrayList<>();
        permissions.add("add_domain");
        permissions.add("update_server_entry");

        UserDetails userDetails = new org.springframework.security.core.userdetails.User("test@test.com", "",
                permissions.stream().map(p -> new SimpleGrantedAuthority(p)).collect(Collectors.toList()));
        //raUser.setPermissions(permissions);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null);

        Domain domain = new Domain();
        domain.setBase("winllc-dev.com");
        DomainForm domainForm = new DomainForm(domain);

        boolean canAddDomain = customPermissionEvaluator.hasPermission(authentication, domainForm, "add_domain");
        assertTrue(canAddDomain);

        boolean canUpdateDomain = customPermissionEvaluator.hasPermission(authentication, domainForm, "update_domain");
        assertFalse(canUpdateDomain);

        ServerEntry serverEntry = new ServerEntry();
        serverEntry.setAccount(account);
        serverEntry.setFqdn("test.winllc-dev.com");

        serverEntry = serverEntryRepository.save(serverEntry);

        ServerEntryForm serverEntryForm = new ServerEntryForm();
        serverEntryForm.setAccountId(account.getId());
        serverEntryForm.setFqdn("test.winllc-dev.com");
        serverEntryForm.setId(serverEntry.getId());

        boolean canUpdateServerEntry = customPermissionEvaluator.hasPermission(authentication, serverEntryForm, "update_server_entry");
        assertTrue(canUpdateServerEntry);

        boolean canDeleteServerEntry = customPermissionEvaluator.hasPermission(authentication, serverEntryForm, "delete_server_entry");
        assertFalse(canDeleteServerEntry);

        accountRepository.deleteAll();
        pocEntryRepository.deleteAll();
        serverEntryRepository.deleteAll();
    }

}