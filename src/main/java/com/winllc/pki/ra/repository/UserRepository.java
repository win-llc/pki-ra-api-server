package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.Account;
import com.winllc.pki.ra.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends CrudRepository<User, UUID> {
    @EntityGraph(attributePaths = "roles")
    Optional<User> findOneByEmail(String email);
    Optional<User> findOneByUsername(String username);
    Optional<User> findOneByIdentifier(UUID identifier);

    List<User> findAllByAccountsContains(Account account);

    void deleteUserByIdentifier(UUID identifier);
}
