package com.dadumserver.user.infrastructure.persistence;

import com.dadumserver.user.domain.model.BlackList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BlackListRepository extends JpaRepository<BlackList, UUID> {
  boolean existsByUserEmailHashed(String userEmailHashed);
  Optional<BlackList> findByUserEmailHashed(String userEmailHashed);
}
