package com.roofingcrm.domain.repository;

import com.roofingcrm.domain.entity.CustomerPhotoReportSectionPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CustomerPhotoReportSectionPhotoRepository extends JpaRepository<CustomerPhotoReportSectionPhoto, UUID> {

    /**
     * Batch-loads section photos with attachments in a single query (one bag root), avoiding
     * MultipleBagFetchException from joining report.sections and section.photos together.
     */
    @Query("select p from CustomerPhotoReportSectionPhoto p join fetch p.attachment where p.section.id in :ids")
    List<CustomerPhotoReportSectionPhoto> findBySectionIdInWithAttachmentFetched(@Param("ids") Collection<UUID> ids);

    @Query("""
            select case when count(p) > 0 then true else false end
            from CustomerPhotoReportSectionPhoto p
            where p.archived = false
              and p.attachment.id = :attachmentId
              and p.section.archived = false
              and p.section.report.archived = false
            """)
    boolean existsActiveReportReference(@Param("attachmentId") UUID attachmentId);
}
