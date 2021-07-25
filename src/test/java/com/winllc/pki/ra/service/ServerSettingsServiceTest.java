package com.winllc.pki.ra.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winllc.pki.ra.BaseTest;
import com.winllc.pki.ra.beans.ServerSettingsGroup;
import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.ServerSettings;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.ServerSettingsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ServerSettingsServiceTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ServerSettingsService serverSettingsService;
    @Autowired
    private ServerSettingsRepository serverSettingsRepository;

    @BeforeEach
    @Transactional
    void before(){
        ServerSettings serverSettings = new ServerSettings();
        serverSettings.setProperty("prop1");
        serverSettings.setValue("value1");
        serverSettingsRepository.save(serverSettings);
    }

    @AfterEach
    @Transactional
    void after(){
        serverSettingsRepository.deleteAll();
    }

    @Test
    void findAll() {
        List<ServerSettingsGroup> all = serverSettingsService.findAll();
        assertTrue(all.size() > 0);
    }

    @Test
    void updateSettings() throws Exception {
        ServerSettings serverSettings = serverSettingsRepository.findAll().get(0);
        serverSettings.setValue("value2");
        ServerSettings serverSettings1 = serverSettingsService.updateSettings(serverSettings);
        assertEquals("value2", serverSettings1.getValue());

        /*
        serverSettings.setProperty("");
        String badJson = new ObjectMapper().writeValueAsString(serverSettings);
        mockMvc.perform(
                post("/api/settings/update")
                        .contentType("application/json")
                        .content(badJson))
                .andExpect(status().is(400));

         */
    }

    @Test
    void updateAllSettings() throws Exception {
        ServerSettings serverSettings = serverSettingsRepository.findAll().get(0);
        List<ServerSettings> settings = new ArrayList<>();
        settings.add(serverSettings);
        List<ServerSettingsGroup> serverSettingsGroups = serverSettingsService.updateAllSettings(settings);
        assertTrue(serverSettingsGroups.size() > 0);

        /*
        serverSettings.setProperty("");
        String badJson = new ObjectMapper().writeValueAsString(settings);
        mockMvc.perform(
                post("/api/settings/updateAll")
                        .contentType("application/json")
                        .content(badJson))
                .andExpect(status().is(400));

         */
    }

    @Test
    void getSetting() throws RAObjectNotFoundException {
        ServerSettings prop1 = serverSettingsService.getSetting("prop1");
        assertNotNull(prop1);
    }

}