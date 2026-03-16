// src/main/java/com/example/simpleagent/repository/HealthRecordRepository.java
package com.example.simpleagent.repository;

import com.example.simpleagent.model.HealthRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface HealthRecordRepository extends JpaRepository<HealthRecord, Long> {
    List<HealthRecord> findByUserIdOrderByRecordDateDesc(Long userId);

    List<HealthRecord> findByUserIdAndRecordDateBetweenOrderByRecordDateDesc(
            Long userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT hr FROM HealthRecord hr WHERE hr.user.id = :userId AND hr.recordType = :recordType ORDER BY hr.recordDate DESC")
    List<HealthRecord> findByUserIdAndRecordType(@Param("userId") Long userId, @Param("recordType") String recordType);

    @Query("SELECT DISTINCT hr.recordType FROM HealthRecord hr WHERE hr.user.id = :userId")
    List<String> findRecordTypesByUserId(@Param("userId") Long userId);
}