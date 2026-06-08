package com.agenticprice.repository;

import com.agenticprice.model.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

public interface OtpCodeRepository extends JpaRepository<OtpCode, UUID> {
    Optional<OtpCode> findTopByEmailAndUsedFalseOrderByCreatedAtDesc(String email);

    @Transactional
    void deleteByEmail(String email);
}