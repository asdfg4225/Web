// src/main/java/com/example/simpleagent/repository/UserRepository.java
package com.example.simpleagent.repository;

import com.example.simpleagent.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);

    @Query("SELECT u FROM User u WHERE u.phone = :phone")
    Optional<User> findByPhone(@Param("phone") String phone);
}