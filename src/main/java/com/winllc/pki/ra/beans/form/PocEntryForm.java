package com.winllc.pki.ra.beans.form;

import com.winllc.acme.common.domain.PocEntry;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PocEntryForm extends ValidForm<PocEntry> {

    private String email;
    private boolean groupEmail;
    private boolean enabled;
    private boolean owner;
    private boolean canManageAllServers;
    private boolean addedManually;

    public PocEntryForm(PocEntry entity) {
        super(entity);
        setEmail(entity.getEmail());
        setGroupEmail(entity.isGroupEmail());
        setEnabled(entity.isEnabled());
        setOwner(entity.isOwner());
        setCanManageAllServers(entity.isCanManageAllServers());
        setAddedManually(entity.isAddedManually());
    }

    public PocEntryForm() {
        super();
    }

    @Override
    protected void processIsValid() {

    }
}
