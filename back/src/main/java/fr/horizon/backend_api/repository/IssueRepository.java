package fr.horizon.backend_api.repository;

import fr.horizon.backend_api.model.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {

    List<Issue> findByCategorie_IdCat(Long categorieId);

    List<Issue> findByCreateur_Id(Long createurId);

    List<Issue> findByStatut(String statut);
}
