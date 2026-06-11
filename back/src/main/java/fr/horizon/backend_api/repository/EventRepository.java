package fr.horizon.backend_api.repository;

import fr.horizon.backend_api.model.Event;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.enums.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("""
            SELECT DISTINCT e FROM Event e
            LEFT JOIN FETCH e.projectGroups pg
            LEFT JOIN FETCH pg.category
            LEFT JOIN FETCH pg.parentGroup
            WHERE e.categoryType = :type
            ORDER BY e.createdAt DESC
            """)
    List<Event> findByCategoryTypeWithGroups(@Param("type") CategoryType type);

    List<Event> findByCategoryTypeOrderByCreatedAtDesc(CategoryType type);
    List<Event> findByCreatedByOrderByCreatedAtDesc(User user);


    // ... tes autres imports

    /**
     * Récupère les événements pour le calendrier d'un utilisateur donné s'il est :
     * - Créateur de l'événement
     * - Participant direct invité (accepté)
     * - Membre d'un groupe projet assigné à l'événement
     * - Enseignant d'un groupe projet (direct) ou du groupe OPTION parent
     */
    @Query("SELECT DISTINCT e FROM Event e " +
            "LEFT JOIN e.participants p " +
            "LEFT JOIN e.projectGroups pg " +
            "LEFT JOIN pg.members m " +
            "LEFT JOIN pg.teachers t " +
            "LEFT JOIN pg.parentGroup pgParent " +
            "LEFT JOIN pgParent.teachers t2 " +
            "WHERE (e.createdBy.id = :userId " +
            "OR (p.user.id = :userId AND p.hasAccepted = true) " +
            "OR m.id = :userId " +
            "OR t.id = :userId " +
            "OR t2.id = :userId) " +
            "AND e.validated = true")
    List<Event> findCalendarEventsForUser(@Param("userId") Long userId);

    /** Récupère les events de type ISSUE/MEETING où l'utilisateur est créateur ou participant.
     *  Inclut les RDVs en attente (validated=false) pour qu'ils apparaissent dans "Mes sessions". */
    @Query("SELECT DISTINCT e FROM Event e " +
            "LEFT JOIN e.participants p " +
            "WHERE (e.createdBy.id = :userId OR p.user.id = :userId) " +
            "AND e.categoryType = :type " +
            "ORDER BY e.startDate ASC")
    List<Event> findIssueEventsForUser(@Param("userId") Long userId, @Param("type") CategoryType type);

    /** Récupère les events AUDIT/DEFENSE des groupes dont l'utilisateur est enseignant (direct ou via parent OPTION). */
    @Query("SELECT DISTINCT e FROM Event e " +
            "LEFT JOIN e.projectGroups pg " +
            "LEFT JOIN pg.teachers t " +
            "LEFT JOIN pg.parentGroup pgParent " +
            "LEFT JOIN pgParent.teachers t2 " +
            "WHERE (t.id = :userId OR t2.id = :userId) " +
            "AND e.validated = true " +
            "ORDER BY e.startDate ASC")
    List<Event> findGroupEventsForTeacher(@Param("userId") Long userId);

    @Query("SELECT e FROM Event e JOIN e.participants p WHERE p.user.id = :userId AND p.hasAccepted IS NULL")
    List<Event> findPendingInvitationsByUserId(@Param("userId") Long userId);

    @Query("SELECT e FROM Event e JOIN e.participants p " +
            "WHERE p.user.id = :userId " +
            "AND p.hasAccepted = true " +
            "AND e.createdBy.id != :userId " +
            "ORDER BY e.startDate ASC")
    List<Event> findAcceptedParticipationsByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(e) FROM Event e JOIN e.participants p " +
            "WHERE p.user.id = :userId " +
            "AND (p.hasAccepted IS NULL OR p.hasAccepted = true) " + // Uniquement les events acceptés ou en attente
            "AND e.startDate < :endDate " +
            "AND e.endDate > :startDate " +
            "AND (:eventId IS NULL OR e.id != :eventId)") // Exclut l'event actuel (gère le cas où l'ID est null lors d'une création)
    long countConflictingEvents(@Param("userId") Long userId,
                                @Param("eventId") Long eventId,
                                @Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);

    boolean existsByIssueIdAndStartDateAfterAndValidatedTrue(Long issueId, LocalDateTime date);

    /** Vérifie l'existence d'un event lié à un ticket, qu'il soit validé ou en attente. */
    boolean existsByIssueIdAndStartDateAfter(Long issueId, LocalDateTime date);

    List<Event> findByCreatedByIdOrderByCreatedAtDesc(Long creatorId);

    @Query("SELECT e FROM Event e WHERE e.createdBy.id = :userId OR (e.issue IS NOT NULL AND e.issue.manager.id = :userId) ORDER BY e.createdAt DESC")
    List<Event> findEventsManagedOrCreatedByUser(@Param("userId") Long userId);

    /**
     * Retourne les événements liés à l'un des groupes dont l'identifiant est dans {@code groupIds}.
     * Couvre directement les groupes PROJECT, OPTION et PROMOTION de l'étudiant.
     */
    @Query("""
            SELECT DISTINCT e FROM Event e
            LEFT JOIN FETCH e.projectGroups pg
            LEFT JOIN FETCH pg.category
            LEFT JOIN FETCH pg.parentGroup
            JOIN e.projectGroups evg
            WHERE evg.id IN :groupIds
            ORDER BY e.startDate ASC
            """)
    List<Event> findByProjectGroupIds(@Param("groupIds") Set<Long> groupIds);

    /** Supprime toutes les entrées de la table de jointure event_project_groups pour un groupe donné. */
    @Modifying
    @Query(value = "DELETE FROM event_project_groups WHERE group_id = :groupId", nativeQuery = true)
    void removeGroupFromAllEvents(@Param("groupId") Long groupId);

    /** Récupère les disponibilités déclarées par un utilisateur, triées par date de début. */
    List<Event> findByCreatedBy_IdAndCategoryTypeOrderByStartDateAsc(Long userId, CategoryType categoryType);

    /**
     * Retourne les events (AUDIT, DEFENSE, ISSUE/RDV) chevauchant un créneau pour un utilisateur donné.
     * Utilisé pour détecter les conflits d'agenda avant de proposer un RDV.
     */
    @Query("SELECT DISTINCT e FROM Event e " +
            "LEFT JOIN e.participants p " +
            "WHERE (e.createdBy.id = :userId OR p.user.id = :userId) " +
            "AND e.categoryType IN ('AUDIT', 'DEFENSE', 'ISSUE') " +
            "AND e.startDate < :endDate " +
            "AND e.endDate > :startDate " +
            "AND (e.validated = true OR p.hasAccepted = true OR e.createdBy.id = :userId) " +
            "ORDER BY e.startDate ASC")
    List<Event> findBusyEventsForUser(@Param("userId") Long userId,
                                      @Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    void deleteByIssueId(Long issueId);

    void deleteByIssueIdIn(List<Long> issueIds);
}

