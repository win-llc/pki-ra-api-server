package com.winllc.pki.ra.domain;

import com.winllc.pki.ra.util.AppUtil;
import org.springframework.data.jpa.domain.AbstractPersistable;

import javax.persistence.Entity;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
public class TermsOfService extends AbstractPersistable<Long> {

    private String versionId;
    private String text;
    private Timestamp created;
    private String forDirectoryName;

    public static TermsOfService buildNew(String text, String forDirectoryName){
        TermsOfService tos = new TermsOfService();
        tos.setVersionId(AppUtil.generate20BitString());
        tos.setCreated(Timestamp.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)));
        tos.setText(text);
        tos.setForDirectoryName(forDirectoryName);
        return tos;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Timestamp getCreated() {
        return created;
    }

    public void setCreated(Timestamp created) {
        this.created = created;
    }

    public String getForDirectoryName() {
        return forDirectoryName;
    }

    public void setForDirectoryName(String forDirectoryName) {
        this.forDirectoryName = forDirectoryName;
    }
}
