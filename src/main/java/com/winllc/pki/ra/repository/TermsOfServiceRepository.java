package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.PocEntry;
import com.winllc.pki.ra.domain.TermsOfService;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface TermsOfServiceRepository extends BaseRepository<TermsOfService> {

    List<TermsOfService> findAllByForDirectoryName(String directoryName);
    Optional<TermsOfService> findByVersionId(String versionId);
}
