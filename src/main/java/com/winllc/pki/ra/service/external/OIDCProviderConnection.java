package com.winllc.pki.ra.service.external;

import com.winllc.pki.ra.beans.OIDCClientDetails;
import com.winllc.pki.ra.domain.ServerEntry;
import com.winllc.pki.ra.exception.RAException;

public interface OIDCProviderConnection extends ExternalServiceConnection {
    ServerEntry createClient(ServerEntry serverEntry) throws Exception;
    ServerEntry deleteClient(ServerEntry serverEntry) throws RAException;
    OIDCClientDetails getClient(ServerEntry serverEntry);
}
