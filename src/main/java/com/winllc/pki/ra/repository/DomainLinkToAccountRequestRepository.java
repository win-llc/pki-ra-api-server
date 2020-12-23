package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.DomainLinkToAccountRequest;

import java.util.List;

public interface DomainLinkToAccountRequestRepository extends BaseRepository<DomainLinkToAccountRequest> {
    List<DomainLinkToAccountRequest> findAllByStatusEquals(String status);
    Integer countAllByStatusEquals(String status);
    List<DomainLinkToAccountRequest> findAllByAccountIdIn(List<Long> accountIds);

}
