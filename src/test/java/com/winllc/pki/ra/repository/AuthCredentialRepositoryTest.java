package com.winllc.acme.common.repository;

import com.winllc.pki.ra.BaseTest;
import com.winllc.acme.common.domain.AuditRecord;
import com.winllc.acme.common.domain.AuthCredential;
import com.winllc.acme.common.domain.ServerEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AuthCredentialRepositoryTest extends BaseTest {

    @Autowired
    private ServerEntryRepository serverEntryRepository;
    @Autowired
    private AuthCredentialRepository authCredentialRepository;

    @BeforeEach
    @Transactional
    void before(){
        ServerEntry serverEntry = ServerEntry.buildNew();
        serverEntry.setFqdn("test.com");
        serverEntry = serverEntryRepository.save(serverEntry);

        AuthCredential authCredential = AuthCredential.buildNew(serverEntry);
        authCredential = authCredentialRepository.save(authCredential);

        serverEntry.getAuthCredentials().add(authCredential);
        serverEntryRepository.save(serverEntry);
    }

    @AfterEach
    @Transactional
    void after(){
        serverEntryRepository.deleteAll();
        authCredentialRepository.deleteAll();
    }

    @Test
    void getAll() {
        List<AuthCredential> credentialList = authCredentialRepository.findAll();
        assertEquals(1, credentialList.size());
    }

    @Test
    void create(){
        Optional<ServerEntry> optionalServer = serverEntryRepository.findDistinctByFqdnEquals("test.com");
        AuthCredential authCredential = AuthCredential.buildNew(optionalServer.get());

        AuthCredential saved = authCredentialRepository.save(authCredential);

        assertEquals(saved.getMacKey(), authCredential.getMacKey());
        assertEquals(ServerEntry.class, saved.getParentEntity().getClass());
    }

    @Test
    void updateAndFindById(){
        List<AuthCredential> credentialList = authCredentialRepository.findAll();
        AuthCredential credential = credentialList.get(0);

        credential.setValid(false);

        authCredentialRepository.save(credential);

        Optional<AuthCredential> optionalCred = authCredentialRepository.findById(credential.getId());
        assertFalse(optionalCred.get().getValid());
    }

    @Test
    @Transactional
    void delete(){
        ServerEntry serverEntry = serverEntryRepository.findDistinctByFqdnEquals("test.com").get();
        AuthCredential credential = authCredentialRepository.findAll().get(0);

        assertTrue(serverEntry.getAuthCredentials().contains(credential));

        authCredentialRepository.delete(credential);

        serverEntry = serverEntryRepository.findDistinctByFqdnEquals("test.com").get();
        assertFalse(serverEntry.getAuthCredentials().contains(credential));
    }
}