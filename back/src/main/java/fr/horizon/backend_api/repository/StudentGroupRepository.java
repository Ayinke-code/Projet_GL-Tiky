package fr.horizon.backend_api.repository;

import fr.horizon.backend_api.model.StudentGroup;
import fr.horizon.backend_api.model.enums.GroupType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long> {

    List<StudentGroup> findAllByOrderByTypeAscNameAsc();

    List<StudentGroup> findByTypeOrderByNameAsc(GroupType type);

    @Query("SELECT g FROM StudentGroup g JOIN g.members m WHERE m.id = :studentId AND g.type = :type")
    List<StudentGroup> findByMemberIdAndType(@Param("studentId") Long studentId, @Param("type") GroupType type);
}
