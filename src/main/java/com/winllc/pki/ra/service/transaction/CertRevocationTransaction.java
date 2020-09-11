package com.winllc.pki.ra.service.transaction;

import com.winllc.acme.common.ra.RACertificateRevokeRequest;
import com.winllc.acme.common.util.CertUtil;
import com.winllc.acme.common.ca.CertAuthority;
import com.winllc.pki.ra.constants.AuditRecordType;
import com.winllc.pki.ra.domain.AuditRecord;
import com.winllc.pki.ra.domain.CertificateRequest;
import com.winllc.pki.ra.domain.Notification;
import com.winllc.pki.ra.domain.ServerEntry;
import com.winllc.pki.ra.exception.RAException;
import com.winllc.pki.ra.exception.RAObjectNotFoundException;
import com.winllc.pki.ra.repository.AuditRecordRepository;
import com.winllc.pki.ra.repository.CertificateRequestRepository;
import com.winllc.pki.ra.service.CertAuthorityConnectionService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.function.Supplier;

public class CertRevocationTransaction extends CertTransaction {

    private static final Logger log = LogManager.getLogger(CertRevocationTransaction.class);

    private final AuditRecordRepository auditRecordRepository;
    private final CertificateRequestRepository certificateRequestRepository;

    public CertRevocationTransaction(CertAuthority certAuthority, ApplicationContext context){
        super(certAuthority, context);
        this.auditRecordRepository = context.getBean(AuditRecordRepository.class);
        this.certificateRequestRepository = context.getBean(CertificateRequestRepository.class);
    }

    public boolean processRevokeCertificate(RACertificateRevokeRequest revokeRequest) throws Exception {
        log.info("Begin certificate revocation: "+revokeRequest);
        String serial = revokeRequest.getSerial();
        //Get serial from certificate request
        if(StringUtils.isBlank(serial)){
            serial = getSerialFromRequest(revokeRequest);
        }

        if(StringUtils.isBlank(serial)) throw new Exception("Request ID and Serial can't both be null");

        SystemActionRunner runner = SystemActionRunner.build(context)
                .createAuditRecord(AuditRecordType.CERTIFICATE_REVOKED)
                .sendNotification();

        Optional<ServerEntry> optionalServerEntry = getServerEntryAssociatedWithCertificate(serial);
        if(optionalServerEntry.isPresent()){
            ServerEntry serverEntry = optionalServerEntry.get();

            runner.createAuditRecord(AuditRecordType.CERTIFICATE_REVOKED, serverEntry);

            Hibernate.initialize(serverEntry.getAccount());
            runner.createNotificationForAccountPocs(
                    Notification.buildNew().addMessage("Certificate revoked"), serverEntry.getAccount());
        }

        String finalSerial = serial;
        ThrowingSupplier<Boolean, Exception> action = () -> {
            boolean revoked = false;
            try {
                revoked = certAuthority.revokeCertificate(finalSerial, revokeRequest.getReason());
            } catch (Exception e) {
                log.error("Could not revoke", e);
            }

            return revoked;
        };

        return runner.execute(action);
    }

    private Optional<ServerEntry> getServerEntryAssociatedWithCertificate(String serial){
        Optional<CertificateRequest> optionalRequest
                = certificateRequestRepository.findDistinctByIssuedCertificateSerialAndCertAuthorityName(serial, certAuthority.getName());

        if(optionalRequest.isPresent()){
            CertificateRequest request = optionalRequest.get();
            Hibernate.initialize(request.getServerEntry());
            ServerEntry serverEntry = request.getServerEntry();
            return Optional.of(serverEntry);
        }

        return Optional.empty();
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
