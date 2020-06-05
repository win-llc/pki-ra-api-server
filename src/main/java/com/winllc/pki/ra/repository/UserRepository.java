package com.winllc.pki.ra.repository;

import com.winllc.pki.ra.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public interface UserRepository extends CrudRepository<User, UUID> {
    @EntityGraph(attributePaths = "roles")
    Optional<User> findOneByUsername(String username);
    Optional<User> findOneByIdentifier(UUID identifier);

    void deleteUserByIdentifier(UUID identifier);
    void deleteByUsernameEquals(String username);
}
