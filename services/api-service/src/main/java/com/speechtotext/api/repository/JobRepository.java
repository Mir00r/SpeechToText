package com.speechtotext.api.repository;

import com.speechtotext.api.model.JobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Job entities.
 */
@Repository
public interface JobRepository extends JpaRepository<JobEntity, UUID> {

    /**
     * Find jobs by status.
     */
    List<JobEntity> findByStatus(JobEntity.JobStatus status);

    /**
     * Find jobs created after a specific date.
     */
    List<JobEntity> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Find jobs by status and created after a specific date.
     */
    List<JobEntity> findByStatusAndCreatedAtAfter(
        JobEntity.JobStatus status, 
        LocalDateTime date
    );

    /**
     * Find pending jobs older than specified minutes.
     */
    @Query("SELECT j FROM JobEntity j WHERE j.status = 'PENDING' AND j.createdAt < :cutoffTime")
    List<JobEntity> findStalePendingJobs(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Count jobs by status.
     */
    long countByStatus(JobEntity.JobStatus status);

    /**
     * Find job by filename (for duplicate detection).
     */
    Optional<JobEntity> findByFilename(String filename);
}
