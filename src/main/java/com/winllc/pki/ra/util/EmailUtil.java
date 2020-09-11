package com.winllc.pki.ra.util;

import com.winllc.pki.ra.constants.ServerSettingRequired;
import com.winllc.pki.ra.domain.ServerSettings;
import com.winllc.pki.ra.service.ServerSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class EmailUtil {

    private final ServerSettingsService settingsService;
    private final TaskExecutor taskExecutor;

    public EmailUtil(ServerSettingsService settingsService, @Qualifier("taskExecutor") TaskExecutor taskExecutor) {
        this.settingsService = settingsService;
        this.taskExecutor = taskExecutor;
    }

    public void sendEmail(SimpleMailMessage message){
        JavaMailSenderImpl client = buildEmailClient();

        taskExecutor.execute(() -> {
            try {
                client.send(message);
            }catch (Exception e){
                e.printStackTrace();
            }
        });
    }

    private JavaMailSenderImpl buildEmailClient(){
        Optional<String> optionalHost = settingsService.getServerSettingValue(ServerSettingRequired.EMAIL_SERVER_HOST);
        Optional<String> optionalPort = settingsService.getServerSettingValue(ServerSettingRequired.EMAIL_SERVER_PORT);

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        if(optionalHost.isPresent()) {
            mailSender.setHost(optionalHost.get());
        }else{
            throw new IllegalArgumentException("No host for email server set");
        }

        if(optionalPort.isPresent()) {
            String port = optionalPort.get();
            mailSender.setPort(Integer.parseInt(port));
        }

        return mailSender;
    }
}
