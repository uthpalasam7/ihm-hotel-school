package com.ihm.hotelschool.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository extends JpaRepository<UserAccount, Long>, JpaSpecificationExecutor<UserAccount> {
    @EntityGraph(attributePaths = {"roles", "branches"})
    java.util.List<UserAccount> findByIdIn(java.util.Collection<Long> ids);
	boolean existsByUsername(String username);

	boolean existsByUsernameAndIdNot(String username, Long id);

	boolean existsByEmail(String email);

	boolean existsByEmailAndIdNot(String email, Long id);

	@EntityGraph(attributePaths = {"roles", "branches"})
	Optional<UserAccount> findByUsername(String username);

	@EntityGraph(attributePaths = {"roles", "branches"})
	Optional<UserAccount> findWithRolesAndBranchesById(Long id);
}
