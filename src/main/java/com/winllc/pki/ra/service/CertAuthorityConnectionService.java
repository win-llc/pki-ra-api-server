package com.winllc.pki.ra.service;

import com.winllc.acme.common.util.CertUtil;
import com.winllc.pki.ra.ca.CertAuthority;
import com.winllc.pki.ra.ca.InternalCertAuthority;
import com.winllc.pki.ra.domain.CertAuthorityConnectionInfo;
import com.winllc.pki.ra.repository.CertAuthorityConnectionInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Optional;

@RestController
@RequestMapping("/ca")
public class CertAuthorityConnectionService {

    //todo remove this
    @PostConstruct
    private void init(){
        CertAuthorityConnectionInfo info = new CertAuthorityConnectionInfo();
        info.setType("internal");
        info.setName("internal");

        repository.save(info);
    }

    @Autowired
    private CertAuthorityConnectionInfoRepository repository;

    @PostMapping("/issueCertificate/{connectionName}")
    public ResponseEntity<?> issueCertificate(@PathVariable String connectionName, @RequestParam String pkcs10){
        //todo issue through here, not /ca/internal

        Optional<CertAuthority> certAuthorityOptional = buildCertAuthority(connectionName);
        if(certAuthorityOptional.isPresent()){
            CertAuthority ca = certAuthorityOptional.get();
            X509Certificate cert = ca.issueCertificate(pkcs10);
            try {
                return ResponseEntity.ok(CertUtil.convertToPem(cert));
            } catch (CertificateEncodingException e) {
                e.printStackTrace();
                return ResponseEntity.badRequest().build();
            }
        }else{
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/revokeCertificate/{connectionName}")
    public ResponseEntity<?> revokeCertificate(@PathVariable String connectionName,
                                               @RequestParam String serial, @RequestParam int reason){
        Optional<CertAuthority> certAuthorityOptional = buildCertAuthority(connectionName);
        if(certAuthorityOptional.isPresent()) {
            CertAuthority ca = certAuthorityOptional.get();
            boolean revoked = ca.revokeCertificate(serial, reason);
            if(revoked){
                return ResponseEntity.ok().build();
            }else{
                return ResponseEntity.badRequest().build();
            }
        }else{
            return ResponseEntity.notFound().build();
        }
    }


    private Optional<CertAuthority> buildCertAuthority(String connectionName){
        CertAuthority certAuthority = null;
        CertAuthorityConnectionInfo info = repository.findByName(connectionName);
        if(info.getType().contentEquals("internal")){
            certAuthority = new InternalCertAuthority(info);
        }

        if(certAuthority != null){
            return Optional.of(certAuthority);
        }else{
            return Optional.empty();
        }
    }
}
