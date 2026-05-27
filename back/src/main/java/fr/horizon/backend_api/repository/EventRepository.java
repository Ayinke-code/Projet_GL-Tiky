package fr.horizon.backend_api.repository;

import fr.horizon.backend_api.model.Event;
import fr.horizon.backend_api.model.User;
import fr.horizon.backend_api.model.enums.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

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
     * - Participant direct invité
     * - Membre d'un groupe projet assigné à l'événement
     */
    @Query("SELECT DISTINCT e FROM Event e " +
            "LEFT JOIN e.participants p " +
            "LEFT JOIN e.projectGroups pg " +
            "LEFT JOIN pg.members m " +
            "WHERE (e.createdBy.id = :userId " +
            "OR p.user.id = :userId " +
            "OR m.id = :userId) " +
            "AND e.validated = true")
    List<Event> findCalendarEventsForUser(@Param("userId") Long userId);
}

