package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.TermsOfService;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface TermsOfServiceRepository extends BaseRepository<TermsOfService> {

    List<TermsOfService> findAllByForDirectoryName(String directoryName);
    Optional<TermsOfService> findByVersionId(String versionId);
}
