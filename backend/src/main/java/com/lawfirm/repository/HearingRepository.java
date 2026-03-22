package com.lawfirm.repository;

import com.lawfirm.entity.Hearing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HearingRepository extends JpaRepository<Hearing, Long> {

    // ── Existing client queries ───────────────────────────────────────────────
    List<Hearing> findByCaseEntityIdOrderByHearingDateAsc(Long caseId);

    @Query("""
        SELECT h FROM Hearing h JOIN FETCH h.caseEntity c
        WHERE c.client.username = :username
        ORDER BY h.hearingDate ASC
    """)
    List<Hearing> findAllByClientUsername(@Param("username") String username);

    @Query("""
        SELECT h FROM Hearing h JOIN FETCH h.caseEntity c
        WHERE c.client.username = :username
          AND h.hearingDate >= :now AND h.status = 'SCHEDULED'
        ORDER BY h.hearingDate ASC
    """)
    List<Hearing> findUpcomingByClientUsername(@Param("username") String username,
                                               @Param("now") LocalDateTime now);

    @Query("""
        SELECT h FROM Hearing h JOIN FETCH h.caseEntity c
        WHERE c.client.username = :username AND h.hearingDate < :now
        ORDER BY h.hearingDate DESC
    """)
    List<Hearing> findPastByClientUsername(@Param("username") String username,
                                           @Param("now") LocalDateTime now);

    @Query("""
        SELECT h FROM Hearing h JOIN FETCH h.caseEntity c
        WHERE c.id = :caseId AND c.client.username = :username
        ORDER BY h.hearingDate ASC
    """)
    List<Hearing> findByCaseIdAndClientUsername(@Param("caseId") Long caseId,
                                                @Param("username") String username);

    @Query("""
        SELECT h FROM Hearing h JOIN FETCH h.caseEntity c
        WHERE h.id = :hearingId AND c.client.username = :username
    """)
    Optional<Hearing> findByIdAndClientUsername(@Param("hearingId") Long hearingId,
                                                @Param("username") String username);

    // ── Lawyer queries (NEW) ──────────────────────────────────────────────────

    /** All hearings for cases assigned to this lawyer */
    @Query("""
        SELECT h FROM Hearing h JOIN FETCH h.caseEntity c
        WHERE c.assignedLawyer.username = :username
        ORDER BY h.hearingDate ASC
    """)
    List<Hearing> findAllByLawyerUsername(@Param("username") String username);

    /** Upcoming scheduled hearings for lawyer */
    @Query("""
        SELECT h FROM Hearing h JOIN FETCH h.caseEntity c
        WHERE c.assignedLawyer.username = :username
          AND h.hearingDate >= :now AND h.status = 'SCHEDULED'
        ORDER BY h.hearingDate ASC
    """)
    List<Hearing> findUpcomingByLawyerUsername(@Param("username") String username,
                                               @Param("now") LocalDateTime now);

    /** Past hearings for lawyer */
    @Query("""
        SELECT h FROM Hearing h JOIN FETCH h.caseEntity c
        WHERE c.assignedLawyer.username = :username
          AND h.hearingDate < :now
        ORDER BY h.hearingDate DESC
    """)
    List<Hearing> findPastByLawyerUsername(@Param("username") String username,
                                           @Param("now") LocalDateTime now);

    /** Today's hearings for lawyer */
    @Query("""
        SELECT h FROM Hearing h JOIN FETCH h.caseEntity c
        WHERE c.assignedLawyer.username = :username
          AND h.hearingDate >= :startOfDay
          AND h.hearingDate < :endOfDay
        ORDER BY h.hearingDate ASC
    """)
    List<Hearing> findTodayByLawyerUsername(@Param("username") String username,
                                            @Param("startOfDay") LocalDateTime startOfDay,
                                            @Param("endOfDay") LocalDateTime endOfDay);

    /** Hearings for a specific case with lawyer ownership check */
    @Query("""
        SELECT h FROM Hearing h JOIN FETCH h.caseEntity c
        WHERE c.id = :caseId AND c.assignedLawyer.username = :username
        ORDER BY h.hearingDate ASC
    """)
    List<Hearing> findByCaseIdAndLawyerUsername(@Param("caseId") Long caseId,
                                                @Param("username") String username);

    /** Single hearing with lawyer ownership check */
    @Query("""
        SELECT h FROM Hearing h JOIN FETCH h.caseEntity c
        WHERE h.id = :hearingId AND c.assignedLawyer.username = :username
    """)
    Optional<Hearing> findByIdAndLawyerUsername(@Param("hearingId") Long hearingId,
                                                @Param("username") String username);
}