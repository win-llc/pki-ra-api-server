package com.winllc.pki.ra.service;

import com.winllc.pki.ra.beans.ServerSettingsGroup;
import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.ServerSettings;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.ServerSettingsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
class ServerSettingsServiceTest {

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
        assertEquals(1, all.size());
    }

    @Test
    void updateSettings() {
        ServerSettings serverSettings = serverSettingsRepository.findAll().get(0);
        serverSettings.setValue("value2");
        ServerSettings serverSettings1 = serverSettingsService.updateSettings(serverSettings);
        assertEquals("value2", serverSettings1.getValue());
    }

    @Test
    void updateAllSettings() {
        ServerSettings serverSettings = serverSettingsRepository.findAll().get(0);
        List<ServerSettings> settings = new ArrayList<>();
        settings.add(serverSettings);
        List<ServerSettingsGroup> serverSettingsGroups = serverSettingsService.updateAllSettings(settings);
        assertEquals(1, serverSettingsGroups.size());
    }

    @Test
    void getSetting() throws RAObjectNotFoundException {
        ServerSettings prop1 = serverSettingsService.getSetting("prop1");
        assertNotNull(prop1);
    }

}