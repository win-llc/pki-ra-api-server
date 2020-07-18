package com.winllc.pki.ra.util;

import com.winllc.pki.ra.constants.ServerSettingRequired;
import com.winllc.pki.ra.domain.ServerSettings;
import com.winllc.pki.ra.service.ServerSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class EmailUtil {

    @Autowired
    private ServerSettingsService settingsService;

    public void sendEmail(){
        //todo
    }

    public void buildEmailClient(){
        //todo continue this
        Optional<String> serverSettingValue = settingsService.getServerSettingValue(ServerSettingRequired.EMAIL_SERVER_HOST);
    }
}
