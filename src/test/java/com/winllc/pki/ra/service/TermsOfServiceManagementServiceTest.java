package com.winllc.pki.ra.service;

import com.winllc.acme.common.DirectoryDataSettings;
import com.winllc.acme.common.domain.Account;
import com.winllc.pki.ra.BaseTest;
import com.winllc.acme.common.domain.*;
import com.winllc.pki.ra.exception.AcmeConnectionException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.acme.common.repository.AccountRepository;
import com.winllc.acme.common.repository.TermsOfServiceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class TermsOfServiceManagementServiceTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TermsOfServiceManagementService termsOfServiceManagementService;
    @Autowired
    private TermsOfServiceRepository termsOfServiceRepository;
    @Autowired
    private AccountRepository accountRepository;
    @MockBean
    private AcmeServerManagementService acmeServerManagementService;

    @BeforeEach
    @Transactional
    void before() throws Exception {
        DirectoryDataSettings directoryDataSettings = new DirectoryDataSettings();
        directoryDataSettings.setName("winllc");

        when(acmeServerManagementService.getDirectorySettingsByName(any(), any())).thenReturn(directoryDataSettings);

        Account account = Account.buildNew("Test Project");
        account.setKeyIdentifier("testkid1");
        account = accountRepository.save(account);

        TermsOfService termsOfService = new TermsOfService();
        termsOfService.setText("test tos 1");
        termsOfService.setVersionId("1a");
        termsOfService.setForDirectoryName("winllc");
        termsOfServiceRepository.save(termsOfService);
    }

    @AfterEach
    @Transactional
    void after(){
        termsOfServiceRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void getAll() {
        List<TermsOfService> all = termsOfServiceManagementService.getAll();
        assertEquals(1, all.size());
    }

    @Test
    void getAllForDirectory() {
        List<TermsOfService> winllc = termsOfServiceManagementService.getAllForDirectory("winllc");
        assertEquals(1, winllc.size());
    }

    @Test
    void getById() throws RAObjectNotFoundException {
        TermsOfService tos = termsOfServiceRepository.findAll().get(0);
        TermsOfService byId = termsOfServiceManagementService.getById(tos.getId());
        assertNotNull(byId);
    }

    @Test
    void getByVersionId() throws RAObjectNotFoundException {
        TermsOfService tos = termsOfServiceRepository.findAll().get(0);
        TermsOfService byId = termsOfServiceManagementService.getByVersionId(tos.getVersionId());
        assertNotNull(byId);
    }

    @Test
    void getForView() throws RAObjectNotFoundException {
        TermsOfService tos = termsOfServiceRepository.findAll().get(0);
        String view = termsOfServiceManagementService.getForView(tos.getVersionId());
        assertEquals("test tos 1", view);
    }

    @Test
    void save() throws IOException, AcmeConnectionException {
        TermsOfService tos = new TermsOfService();
        tos.setText("new text");
        Long id = termsOfServiceManagementService.save("winllc", tos);
        TermsOfService newTos = termsOfServiceRepository.findById(id).get();
        assertEquals(tos.getText(), newTos.getText());
    }

    @Test
    void update() throws RAObjectNotFoundException, IOException, AcmeConnectionException {
        TermsOfService tos = new TermsOfService();
        tos.setText("new text");
        tos.setVersionId("v1");
        termsOfServiceRepository.save(tos);

        tos.setText("new text 2");

        TermsOfService newTos = termsOfServiceManagementService.update("winllc", tos);
        assertEquals(tos.getText(), newTos.getText());
    }

    @Test
    @WithMockUser(value = "test@test.com", authorities = {"super_admin"})
    void delete() {
        TermsOfService tos = new TermsOfService();
        tos.setText("new text");
        tos.setVersionId("v2");
        TermsOfService save = termsOfServiceRepository.save(tos);
        assertNotNull(save);

        termsOfServiceManagementService.delete(save.getId());

        assertEquals(1, termsOfServiceRepository.findAll().size());
    }
}