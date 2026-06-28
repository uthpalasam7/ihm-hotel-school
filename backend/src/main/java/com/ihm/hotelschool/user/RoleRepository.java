package com.ihm.hotelschool.user;

import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
	Optional<Role> findByCode(String code);

	Set<Role> findByCodeIn(Set<String> codes);
}
