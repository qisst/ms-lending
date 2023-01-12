package com.qisstpay.lendingservice.repository;

import com.qisstpay.lendingservice.entity.TasdeeqCallLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TasdeeqCallRepository extends JpaRepository<TasdeeqCallLog, Long> {
    String getLastTokenId = "SELECT id FROM call_logs_tasdeeq where end_point = 'AUTH' and status = 'SUCCESS' ORDER BY id DESC LIMIT 1";

    @Query(value = getLastTokenId, nativeQuery = true)
    Long findLastTokenId();

}
