package com.agenticprice.repository;

import com.agenticprice.model.UserQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface UserQueryRepository extends JpaRepository<UserQuery, UUID> {
}