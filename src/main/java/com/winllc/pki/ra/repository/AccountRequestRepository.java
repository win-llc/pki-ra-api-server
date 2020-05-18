package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.AccountRequest;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

@Repository
@Transactional
public interface AccountRequestRepository extends BaseRepository<AccountRequest> {

    List<AccountRequest> findAllByStateEquals(String state);

}
