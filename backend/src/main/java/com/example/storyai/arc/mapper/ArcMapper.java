package com.example.storyai.arc.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.storyai.arc.model.Arc;

/** MyBatis mapper for arcs (v0.1.1 Phase 6 / TASK-152). */
@Mapper
public interface ArcMapper {

    int insert(Arc arc);

    Arc findById(@Param("id") Long id);

    List<Arc> findByStoryId(@Param("storyId") Long storyId);

    /** The arc whose range covers the given chapter number, if any. */
    Arc findCurrentByChapter(@Param("storyId") Long storyId,
                             @Param("chapterNumber") Integer chapterNumber);

    /** At most one ACTIVE arc per story (enforced here, not by a DB constraint). */
    Arc findActive(@Param("storyId") Long storyId);

    int update(Arc arc);

    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
