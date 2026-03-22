package com.lawfirm.repository;

import com.lawfirm.entity.CaseRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseRequestRepository extends JpaRepository<CaseRequest, Long> {

    // ── Existing ──────────────────────────────────────────────────────────────

    /** All requests raised by a specific client — newest first */
    List<CaseRequest> findByClientIdOrderByCreatedAtDesc(Long clientId);

    /** All PENDING requests — shown to all lawyers in their portal */
    List<CaseRequest> findByStatusOrderByCreatedAtDesc(CaseRequest.RequestStatus status);

    /** All requests accepted or rejected by a specific lawyer */
    List<CaseRequest> findByLawyerIdOrderByUpdatedAtDesc(Long lawyerId);

    /** Count pending requests for a client */
    long countByClientIdAndStatus(Long clientId, CaseRequest.RequestStatus status);

    /** Pending requests filtered by case type, ordered by urgency then date */
    @Query("""
        SELECT r FROM CaseRequest r
        WHERE r.status = 'PENDING'
          AND (:caseType IS NULL OR r.caseType = :caseType)
        ORDER BY r.urgency DESC, r.createdAt ASC
    """)
    List<CaseRequest> findPendingByOptionalCaseType(
            @Param("caseType") com.lawfirm.entity.Case.CaseType caseType);

    // ── New queries needed by LawyerService ──────────────────────────────────

    /** All pending requests visible to lawyers (with client eagerly loaded) */
    @Query("""
        SELECT r FROM CaseRequest r
        JOIN FETCH r.client
        WHERE r.status = 'PENDING'
        ORDER BY
          CASE r.urgency
            WHEN 'CRITICAL' THEN 1
            WHEN 'HIGH'     THEN 2
            WHEN 'MEDIUM'   THEN 3
            WHEN 'LOW'      THEN 4
          END,
          r.createdAt ASC
    """)
    List<CaseRequest> findAllPendingOrderedByUrgency();

    /** Requests handled by a lawyer (accepted or rejected) by username */
    @Query("""
        SELECT r FROM CaseRequest r
        JOIN FETCH r.client
        LEFT JOIN FETCH r.createdCase
        WHERE r.lawyer.username = :username
        ORDER BY r.updatedAt DESC
    """)
    List<CaseRequest> findByLawyerUsernameOrderByUpdatedAtDesc(
            @Param("username") String username);

    /** Single pending request by ID — for accept/reject */
    @Query("""
        SELECT r FROM CaseRequest r
        JOIN FETCH r.client
        WHERE r.id = :id AND r.status = 'PENDING'
    """)
    Optional<CaseRequest> findPendingById(@Param("id") Long id);

    /** Count of pending requests (for lawyer badge) */
    long countByStatus(CaseRequest.RequestStatus status);

    /** Pending requests by case type string for lawyer filter */
    @Query("""
        SELECT r FROM CaseRequest r
        JOIN FETCH r.client
        WHERE r.status = 'PENDING'
          AND r.caseType = :caseType
        ORDER BY
          CASE r.urgency
            WHEN 'CRITICAL' THEN 1
            WHEN 'HIGH'     THEN 2
            WHEN 'MEDIUM'   THEN 3
            WHEN 'LOW'      THEN 4
          END,
          r.createdAt ASC
    """)
    List<CaseRequest> findPendingByCaseType(
            @Param("caseType") com.lawfirm.entity.Case.CaseType caseType);
}