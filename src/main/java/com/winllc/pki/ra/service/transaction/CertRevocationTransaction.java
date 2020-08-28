package com.winllc.pki.ra.service.transaction;

import com.winllc.acme.common.ra.RACertificateRevokeRequest;
import com.winllc.acme.common.util.CertUtil;
import com.winllc.pki.ra.ca.CertAuthority;
import com.winllc.pki.ra.constants.AuditRecordType;
import com.winllc.pki.ra.domain.AuditRecord;
import com.winllc.pki.ra.domain.CertificateRequest;
import com.winllc.pki.ra.exception.RAException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.AuditRecordRepository;
import com.winllc.pki.ra.repository.CertificateRequestRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Optional;

public class CertRevocationTransaction extends CertTransaction {

    private final AuditRecordRepository auditRecordRepository;
    private final CertificateRequestRepository certificateRequestRepository;

    public CertRevocationTransaction(CertAuthority certAuthority, ApplicationContext context){
        super(certAuthority);
        this.auditRecordRepository = context.getBean(AuditRecordRepository.class);
        this.certificateRequestRepository = context.getBean(CertificateRequestRepository.class);
    }

    public boolean processRevokeCertificate(RACertificateRevokeRequest revokeRequest) throws Exception {
        String serial = revokeRequest.getSerial();
        //Get serial from certificate request
        if(StringUtils.isBlank(serial)){
            serial = getSerialFromRequest(revokeRequest);
        }

        if(StringUtils.isBlank(serial)) throw new Exception("Request ID and Serial can't both be null");

        boolean revoked = certAuthority.revokeCertificate(serial, revokeRequest.getReason());
        if (revoked) {
            processRevokedCertificate(serial);
        }

        return revoked;
    }

    private void processRevokedCertificate(String serial){
        AuditRecord record = AuditRecord.buildNew(AuditRecordType.CERTIFICATE_REVOKED);

        auditRecordRepository.save(record);
    }

    private String getSerialFromRequest(RACertificateRevokeRequest revokeRequest) throws RAException, CertificateException, IOException {
        Optional<CertificateRequest> optionalCertificateRequest = certificateRequestRepository.findById(revokeRequest.getRequestId());
        if(optionalCertificateRequest.isPresent()){
            CertificateRequest certificateRequest = optionalCertificateRequest.get();
            if(StringUtils.isNotBlank(certificateRequest.getIssuedCertificate())){
                X509Certificate x509Certificate = CertUtil.base64ToCert(certificateRequest.getIssuedCertificate());
                return x509Certificate.getSerialNumber().toString();
            }else{
                throw new RAException("No certificate in request, most likely not issued yet");
            }
        }else{
            throw new RAObjectNotFoundException(CertificateRequest.class, revokeRequest.getRequestId());
        }
    }
}
