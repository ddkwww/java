package com.fenglin.springboottest.repository;

import com.fenglin.springboottest.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByPermission(Integer permission);

    List<User> findByPermissionIsNull();
}
