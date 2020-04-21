package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.Domain;
import com.winllc.pki.ra.domain.DomainLinkToAccountRequest;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface DomainLinkToAccountRequestRepository extends BaseRepository<DomainLinkToAccountRequest> {
    List<DomainLinkToAccountRequest> findAllByStatusEquals(String status);
}
