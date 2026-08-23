-- ============================================================================
-- AI Story Co-Author v0.1.1 — Schema V15 (RH-02 / HB-002)
-- Minimal provenance for mutable live-state slots. This is not temporal memory:
-- each slot still stores only its current value, plus the candidate that last
-- wrote that value.
-- ============================================================================

ALTER TABLE current_state
    ADD COLUMN source_candidate_id BIGINT NULL
        COMMENT 'memory_candidate that last wrote this current slot';

ALTER TABLE relationship_state
    ADD COLUMN source_candidate_id BIGINT NULL
        COMMENT 'memory_candidate that last wrote this relationship slot';

CREATE INDEX idx_state_source_candidate
    ON current_state (source_candidate_id);

CREATE INDEX idx_rel_source_candidate
    ON relationship_state (source_candidate_id);

-- Conservatively attribute legacy rows only when their complete live value and
-- normalized slot match an APPLIED candidate. MAX(id) represents the last
-- matching upsert; ambiguous/unmatched hand-written rows remain NULL and cannot
-- be deleted by revision invalidation.
UPDATE current_state state_row
JOIN (
    SELECT existing_state.id AS state_id, MAX(candidate.id) AS candidate_id
    FROM current_state existing_state
    JOIN memory_candidate candidate
      ON candidate.story_id = existing_state.story_id
     AND candidate.type = 'CURRENT_STATE'
     AND candidate.processing_status = 'APPLIED'
     AND candidate.subject = existing_state.subject
     AND candidate.value = existing_state.value
     AND CASE LOWER(candidate.field)
           WHEN 'location' THEN 'LOCATION'
           WHEN 'place' THEN 'LOCATION'
           WHEN 'inventory' THEN 'INVENTORY'
           WHEN 'item' THEN 'INVENTORY'
           WHEN 'weapon' THEN 'INVENTORY'
           WHEN 'equipment' THEN 'INVENTORY'
           WHEN 'physical_condition' THEN 'PHYSICAL_CONDITION'
           WHEN 'injury' THEN 'PHYSICAL_CONDITION'
           WHEN 'health' THEN 'PHYSICAL_CONDITION'
           WHEN 'emotion' THEN 'EMOTION'
           WHEN 'mood' THEN 'EMOTION'
           WHEN 'current_goal' THEN 'CURRENT_GOAL'
           WHEN 'goal' THEN 'CURRENT_GOAL'
           ELSE 'OTHER'
         END = existing_state.category
     AND CASE
           WHEN LOWER(candidate.field) IN ('inventory', 'item', 'weapon', 'equipment')
                AND CHAR_LENGTH(TRIM(candidate.value)) > 0
             THEN CONCAT('item:', LEFT(REPLACE(TRIM(candidate.value), ' ', ''), 48))
           ELSE candidate.field
         END = existing_state.field
    GROUP BY existing_state.id
) matched ON matched.state_id = state_row.id
SET state_row.source_candidate_id = matched.candidate_id
WHERE state_row.source_candidate_id IS NULL;

UPDATE relationship_state relationship_row
JOIN (
    SELECT existing_relationship.id AS relationship_id,
           MAX(candidate.id) AS candidate_id
    FROM relationship_state existing_relationship
    JOIN memory_candidate candidate
      ON candidate.story_id = existing_relationship.story_id
     AND candidate.type = 'RELATIONSHIP'
     AND candidate.processing_status = 'APPLIED'
     AND candidate.value = existing_relationship.description
     AND CASE
           WHEN LOCATE('->', candidate.subject) > 1
             THEN TRIM(SUBSTRING_INDEX(candidate.subject, '->', 1))
           ELSE TRIM(candidate.subject)
         END = existing_relationship.subject_a
     AND CASE
           WHEN LOCATE('->', candidate.subject) > 1
             THEN TRIM(SUBSTRING(candidate.subject, LOCATE('->', candidate.subject) + 2))
           ELSE '(story)'
         END = existing_relationship.subject_b
    GROUP BY existing_relationship.id
) matched ON matched.relationship_id = relationship_row.id
SET relationship_row.source_candidate_id = matched.candidate_id
WHERE relationship_row.source_candidate_id IS NULL;

ALTER TABLE current_state
    ADD CONSTRAINT fk_state_source_candidate
        FOREIGN KEY (source_candidate_id) REFERENCES memory_candidate (id)
        ON DELETE SET NULL;

ALTER TABLE relationship_state
    ADD CONSTRAINT fk_rel_source_candidate
        FOREIGN KEY (source_candidate_id) REFERENCES memory_candidate (id)
        ON DELETE SET NULL;
