package com.winllc.pki.ra.service.transaction;

import com.winllc.acme.common.ca.CachedCertificate;
import com.winllc.acme.common.cache.CachedCertificateService;
import com.winllc.acme.common.constants.RevocationReason;
import com.winllc.acme.common.domain.RevocationRequest;
import com.winllc.acme.common.repository.RevocationRequestRepository;
import com.winllc.acme.common.constants.AuditRecordType;
import com.winllc.acme.common.domain.CertificateRequest;
import com.winllc.acme.common.domain.Notification;
import com.winllc.acme.common.domain.ServerEntry;
import com.winllc.acme.common.repository.CertificateRequestRepository;
import com.winllc.ra.integration.ca.CertAuthority;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.context.ApplicationContext;

import java.security.cert.X509Certificate;
import java.util.Optional;

public class CertRevocationTransaction extends CertTransaction {

    private static final Logger log = LogManager.getLogger(CertRevocationTransaction.class);

    private final CertificateRequestRepository certificateRequestRepository;
    private final CachedCertificateService cachedCertificateService;
    private RevocationRequestRepository revocationRequestRepository;

    public CertRevocationTransaction(CertAuthority certAuthority, ApplicationContext context) {
        super(certAuthority, context);
        this.cachedCertificateService = context.getBean(CachedCertificateService.class);
        this.certificateRequestRepository = context.getBean(CertificateRequestRepository.class);
        this.revocationRequestRepository = context.getBean(RevocationRequestRepository.class);
    }

    public boolean processRevokeCertificate(RevocationRequest revokeRequest) throws Exception {
        log.info("Begin certificate revocation: " + revokeRequest);
        String serial = revokeRequest.getSerial();
        //Get serial from certificate request

        if (StringUtils.isBlank(serial)) throw new Exception("Request ID and Serial can't both be null");

        SystemActionRunner runner = SystemActionRunner.build(context)
                .createAuditRecord(AuditRecordType.CERTIFICATE_REVOKED)
                .sendNotification();

        Optional<ServerEntry> optionalServerEntry = getServerEntryAssociatedWithCertificate(serial);
        if (optionalServerEntry.isPresent()) {
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
                Integer reason = RevocationReason.UNSPECIFIED.getCode();
                if(revokeRequest.getReason() != null){
                    reason = revokeRequest.getReason();
                }

                revoked = certAuthority.revokeCertificate(finalSerial, reason);
            } catch (Exception e) {
                log.error("Could not revoke", e);
            }

            return revoked;
        };

        boolean success = runner.execute(action);
        if (success) {
            postProcessRevokedCert(revokeRequest);
        }

        return success;
    }

    private void postProcessRevokedCert(RevocationRequest request) {
        String finalSerial = request.getSerial();
        ThrowingSupplier<Boolean, Exception> action = () -> {
            boolean cached = false;

            try {
                request.setStatus("revoked");
                revocationRequestRepository.save(request);
            } catch (Exception e) {
                log.error("Could not update revocation request in DB", e);
            }

            try {
                X509Certificate certificate = certAuthority.getCertificateBySerial(finalSerial);
                String thumbprint = DigestUtils.sha1Hex(certificate.getEncoded());

                Optional<CachedCertificate> certificateOptional = cachedCertificateService.findById(thumbprint);
                if (certificateOptional.isPresent()) {
                    CachedCertificate cachedCertificate = certificateOptional.get();
                    cachedCertificate.setStatus("REVOKED");
                    cachedCertificateService.update(cachedCertificate);
                    cached = true;
                }
            } catch (Exception e) {
                log.error("Could not cache certificate", e);
            }
            return cached;
        };

        SystemActionRunner runner = SystemActionRunner.build(context);
        runner.executeAsync(action);

    }

    private Optional<ServerEntry> getServerEntryAssociatedWithCertificate(String serial) {
        Optional<CertificateRequest> optionalRequest
                = certificateRequestRepository.findDistinctByIssuedCertificateSerialAndCertAuthorityName(serial, certAuthority.getName());

        if (optionalRequest.isPresent()) {
            CertificateRequest request = optionalRequest.get();
            Hibernate.initialize(request.getServerEntry());
            ServerEntry serverEntry = request.getServerEntry();
            return Optional.of(serverEntry);
        }

        return Optional.empty();
    }
}
