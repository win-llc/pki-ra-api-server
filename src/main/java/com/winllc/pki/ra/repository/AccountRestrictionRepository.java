package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.AccountRestriction;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.sql.Timestamp;
import java.util.List;

@Repository
@Transactional
public interface AccountRestrictionRepository extends BaseRepository<AccountRestriction> {

    List<AccountRestriction> findAllByDueByBefore(Timestamp timestamp);
    List<AccountRestriction> findAllByAccount(Account account);
    List<AccountRestriction> findAllByAccountAndCompleted(Account account, boolean completed);
    List<AccountRestriction> findAllByAccountAndDueByBefore(Account account, Timestamp timestamp);
    List<AccountRestriction> findAllByAccountAndDueByBeforeAndCompletedEquals(Account account, Timestamp timestamp, boolean completed);
}
