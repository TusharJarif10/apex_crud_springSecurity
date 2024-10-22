package com.apex.tushar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.apex.tushar.entity.Users;

import java.util.Optional;

public interface UsersRepo extends JpaRepository<Users, Integer>, JpaSpecificationExecutor<Users> {
    Optional<Users> findByUserid(String userid);
}
