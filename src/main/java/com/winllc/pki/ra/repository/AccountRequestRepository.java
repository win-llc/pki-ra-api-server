package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.AccountRequest;
import com.winllc.pki.ra.domain.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRequestRepository extends CrudRepository<AccountRequest, Long> {

    List<AccountRequest> findAll();
    List<AccountRequest> findAllByStateEquals(String state);

}
