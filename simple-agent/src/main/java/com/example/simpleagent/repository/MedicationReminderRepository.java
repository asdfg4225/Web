// src/main/java/com/example/simpleagent/repository/MedicationReminderRepository.java
package com.example.simpleagent.repository;

import com.example.simpleagent.model.MedicationReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface MedicationReminderRepository extends JpaRepository<MedicationReminder, Long> {
    List<MedicationReminder> findByUserIdAndIsActiveTrue(Long userId);

    List<MedicationReminder> findByUserId(Long userId);

    @Query("SELECT mr FROM MedicationReminder mr WHERE mr.user.id = :userId AND mr.isActive = true " +
            "AND (mr.endDate IS NULL OR mr.endDate >= :today)")
    List<MedicationReminder> findActiveReminders(@Param("userId") Long userId, @Param("today") LocalDate today);
}