package com.speechtotext.api.mapper;

import com.speechtotext.api.dto.TranscriptionJobResponse;
import com.speechtotext.api.dto.TranscriptionResponse;
import com.speechtotext.api.model.JobEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for converting between Job entities and DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface JobMapper {

    /**
     * Convert JobEntity to TranscriptionResponse.
     */
    @Mapping(target = "downloadUrl", expression = "java(generateDownloadUrl(job.getId()))")
    TranscriptionResponse toTranscriptionResponse(JobEntity job);

    /**
     * Convert JobEntity to TranscriptionJobResponse for async jobs.
     */
    @Mapping(target = "statusUrl", expression = "java(generateStatusUrl(job.getId()))")
    @Mapping(target = "message", constant = "Transcription job created successfully")
    TranscriptionJobResponse toTranscriptionJobResponse(JobEntity job);

    /**
     * Generate download URL for completed transcriptions.
     */
    default String generateDownloadUrl(java.util.UUID jobId) {
        if (jobId == null) return null;
        return "/api/v1/transcriptions/" + jobId + "/download";
    }

    /**
     * Generate status URL for job polling.
     */
    default String generateStatusUrl(java.util.UUID jobId) {
        if (jobId == null) return null;
        return "/api/v1/transcriptions/" + jobId;
    }
}
