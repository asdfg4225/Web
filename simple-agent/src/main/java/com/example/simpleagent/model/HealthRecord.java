// src/main/java/com/example/simpleagent/model/HealthRecord.java
package com.example.simpleagent.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "health_records")
@Data
public class HealthRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "record_date", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate recordDate;

    @Column(name = "record_type", nullable = false)
    private String recordType; // 血压、血糖、心率等

    @Column(name = "record_value")
    private String recordValue;

    private String unit;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "recorded_by")
    private String recordedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (recordedBy == null) {
            recordedBy = "用户本人";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}