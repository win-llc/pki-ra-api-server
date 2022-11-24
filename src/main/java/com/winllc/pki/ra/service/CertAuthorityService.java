package com.winllc.pki.ra.service;

import com.winllc.acme.common.client.ca.LoadedCertAuthorityStore;
import com.winllc.acme.common.constants.DateTimeUtil;
import com.winllc.pki.ra.beans.info.CertAuthorityInfo;
import com.winllc.pki.ra.beans.search.GridModel;
import com.winllc.ra.integration.ca.CertAuthority;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/certAuthority")
public class CertAuthorityService  {

    private static final Logger log = LogManager.getLogger(CertAuthorityService.class);

    private final LoadedCertAuthorityStore certAuthorityStore;

    public CertAuthorityService(LoadedCertAuthorityStore certAuthorityStore) {
        this.certAuthorityStore = certAuthorityStore;
    }

    @PostMapping("/paged")
    public Page<CertAuthorityInfo> getPaged(Integer page, Integer pageSize,
                                            String order, String sortBy,
                                            Map<String, String> allRequestParams,
             @RequestBody GridModel gridModel,
                                            Authentication authentication
    ) {
        List<CertAuthority> allCertAuthorities = certAuthorityStore.getAllCertAuthorities();

        List<CertAuthorityInfo> infoList = new ArrayList<>();
        for (CertAuthority ca : allCertAuthorities) {
            try {
                infoList.add(buildInfo(ca));
            } catch (Exception e) {
                log.error("Could not build info", e);
            }
        }

        return new PageImpl<>(infoList);
    }

    @GetMapping("/id/{id}")
    public CertAuthorityInfo findRest(@PathVariable String id,
                                      Authentication authentication) throws Exception {

        CertAuthority loadedCertAuthority = certAuthorityStore.getLoadedCertAuthority(id);

        return buildInfo(loadedCertAuthority);
    }


    private CertAuthorityInfo buildInfo(CertAuthority ca) throws Exception {
        CertAuthorityInfo info = new CertAuthorityInfo();

        X509Certificate certificate = (X509Certificate) ca.getTrustChain()[0];

        info.setName(ca.getName());
        info.setTrustChain(ca.getConnectionInfo().getTrustChainBase64());
        info.setDn(certificate.getSubjectX500Principal().getName());
        info.setValidFrom(DateTimeUtil.DATE_TIME_FORMATTER.format(certificate.getNotBefore().toInstant()));
        info.setValidTo(DateTimeUtil.DATE_TIME_FORMATTER.format(certificate.getNotAfter().toInstant()));

        return info;
    }

}
