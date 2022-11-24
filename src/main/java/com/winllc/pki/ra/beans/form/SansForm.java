package com.winllc.pki.ra.beans.form;

import com.winllc.acme.common.domain.BaseEntity;
import com.winllc.acme.common.domain.ServerEntry;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Getter
@Setter
public class SansForm extends ValidForm {

    private Long id;
    private Long serverId;
    private String value;
    private String domain;

    public SansForm(String san, Long serverId) {
        setId(ThreadLocalRandom.current().nextLong());
        if(san.contains(".")){
            String hostname = san.split("\\.")[0];
            setValue(hostname);

            String domain = san.substring(san.indexOf(".") + 1);
            setDomain(domain);
        }else{
            setValue(san);
            setDomain("");
        }

        setServerId(serverId);
    }

    public SansForm() {

    }

    public String buildFqdn(){
        if(StringUtils.isNotBlank(domain)){
            return value + "." + domain;
        }else{
            return value;
        }
    }

    @Override
    protected void processIsValid() {

    }
}
