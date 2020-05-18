package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.config.AppConfig;
import com.winllc.pki.ra.domain.ServerSettings;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = AppConfig.class)
@ActiveProfiles("test")
class ServerSettingsRepositoryTest {

    @Autowired
    private ServerSettingsRepository serverSettingsRepository;

    @Test
    void findDistinctByPropertyEquals() {
        ServerSettings serverSettings = new ServerSettings();
        serverSettings.setProperty("key");
        serverSettings.setValue("val");
        serverSettingsRepository.save(serverSettings);

        Optional<ServerSettings> settingOptional = serverSettingsRepository.findDistinctByPropertyEquals("key");
        assertTrue(settingOptional.isPresent());
    }
}