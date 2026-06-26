package com.ihm.hotelschool.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserAccount, Long> {
	boolean existsByUsername(String username);

	@EntityGraph(attributePaths = {"roles", "branches"})
	Optional<UserAccount> findByUsername(String username);

	@EntityGraph(attributePaths = {"roles", "branches"})
	Optional<UserAccount> findWithRolesAndBranchesById(Long id);
}
