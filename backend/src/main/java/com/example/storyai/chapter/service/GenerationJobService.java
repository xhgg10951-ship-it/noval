package com.example.storyai.chapter.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.storyai.chapter.mapper.GenerationJobMapper;
import com.example.storyai.chapter.model.GenerationJob;
import com.example.storyai.common.exception.ResourceNotFoundException;

/**
 * Transactional persistence for {@link GenerationJob} rows (M5 / TASK-036).
 *
 * <p>Orchestration (run / continue / retry) lives in
 * {@link GenerationOrchestrationService}; this service only owns the row.</p>
 */
@Service
public class GenerationJobService {

    private final GenerationJobMapper jobMapper;

    public GenerationJobService(GenerationJobMapper jobMapper) {
        this.jobMapper = jobMapper;
    }

    @Transactional
    public GenerationJob create(GenerationJob job) {
        jobMapper.insert(job);
        return jobMapper.findById(job.getId());
    }

    public GenerationJob get(Long jobId) {
        GenerationJob job = jobMapper.findById(jobId);
        if (job == null) {
            throw new ResourceNotFoundException("GenerationJob", jobId);
        }
        return job;
    }

    public GenerationJob findLatestByStage(Long stageId) {
        return jobMapper.findLatestByStage(stageId);
    }

    @Transactional
    public GenerationJob update(GenerationJob job) {
        jobMapper.update(job);
        return jobMapper.findById(job.getId());
    }
}
