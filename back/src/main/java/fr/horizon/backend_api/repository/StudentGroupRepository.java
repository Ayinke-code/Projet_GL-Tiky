package fr.horizon.backend_api.repository;

import fr.horizon.backend_api.model.StudentGroup;
import fr.horizon.backend_api.model.enums.GroupType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long> {

    List<StudentGroup> findAllByOrderByTypeAscNameAsc();

    List<StudentGroup> findByTypeOrderByNameAsc(GroupType type);

    @Query("SELECT g FROM StudentGroup g JOIN g.members m WHERE m.id = :studentId AND g.type = :type")
    List<StudentGroup> findByMemberIdAndType(@Param("studentId") Long studentId, @Param("type") GroupType type);

    /** Tous les groupes dont l'étudiant est directement membre (tous types confondus). */
    @Query("SELECT g FROM StudentGroup g JOIN g.members m WHERE m.id = :studentId")
    List<StudentGroup> findAllByMemberId(@Param("studentId") Long studentId);

    boolean existsByNameIgnoreCaseAndType(String name, GroupType type);

    boolean existsByNameIgnoreCaseAndTypeAndIdNot(String name, GroupType type, Long id);

    /** Nullifie parent_group_id sur tous les groupes PROJECT enfants de l'option supprimée. */
    @Modifying
    @Query("UPDATE StudentGroup g SET g.parentGroup = null WHERE g.parentGroup.id = :parentId")
    void clearParentGroup(@Param("parentId") Long parentId);

    /** Nullifie promotion_id sur tous les groupes OPTION liés à la promotion supprimée. */
    @Modifying
    @Query("UPDATE StudentGroup g SET g.promotion = null WHERE g.promotion.id = :promotionId")
    void clearPromotion(@Param("promotionId") Long promotionId);

    /** Retourne les groupes OPTION enfants d'une PROMOTION. */
    List<StudentGroup> findByPromotion_Id(Long promotionId);

    /** Retourne les groupes PROJECT enfants d'une OPTION. */
    List<StudentGroup> findByParentGroup_Id(Long parentGroupId);
}
