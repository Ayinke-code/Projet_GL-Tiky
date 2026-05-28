package fr.horizon.backend_api.repository;

import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.enums.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {

    List<Issue> findByCategoryId(Long categoryId);

    List<Issue> findByCreatedById(Long userId);

    List<Issue> findByStatus(IssueStatus status);

    boolean existsByCreatedByIdAndStatus(Long userId, IssueStatus status);

    List<Issue> findByStatusAndCreatedBy_Id(IssueStatus status, Long createdById);

    /**
     * Récupère les issues visibles pour un étudiant.
     * <p>
     * Un étudiant a accès aux issues publiques au statut 'OUVERT'
     * ainsi qu'aux issues qu'il a lui-même créées.
     *
     * @param studentId identifiant de l'étudiant connecté
     * @return liste des issues correspondantes
     */
    @Query("SELECT i FROM Issue i WHERE (i.status = fr.horizon.backend_api.model.enums.IssueStatus.OUVERT AND i.isPrivate = false) OR i.createdBy.id = :studentId")
    List<Issue> findIssuesForStudent(@Param("studentId") Long studentId);
}

