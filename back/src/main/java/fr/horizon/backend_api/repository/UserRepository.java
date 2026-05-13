package fr.horizon.backend_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.horizon.backend_api.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);

    /**
     * Sélectionne les utilisateurs ayant le rôle 'TEACHER' 
     * dont l'ID n'est pas présent dans la liste des IDs des followers de la catégorie.
     */
    @Query("SELECT u FROM User u " +
           "JOIN u.roles r " +
           "WHERE r.name = 'TEACHER' " +
           "AND CAST(u.id AS string) NOT IN (" +
           "  SELECT cf.teacherId FROM CategoryFollower cf " +
           "  WHERE cf.categoryId = :categoryId" +
           ")")
    List<User> findTeachersNotFollowingCategory(@Param("categoryId") Long categoryId);
}
