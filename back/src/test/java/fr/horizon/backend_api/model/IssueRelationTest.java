package fr.horizon.backend_api.model;

import fr.horizon.backend_api.model.enums.RelationType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IssueRelationTest {

    @Test
    void testIssueRelationBuilderAndGetters() {
        Issue source = new Issue();
        source.setId(1L);

        Issue target = new Issue();
        target.setId(2L);

        User creator = new User();
        creator.setId(3L);

        IssueRelation relation = IssueRelation.builder()
                .id(10L)
                .sourceIssue(source)
                .targetIssue(target)
                .type(RelationType.DEPENDS_ON)
                .manager(true)
                .createdBy(creator)
                .build();

        assertNotNull(relation);
        assertEquals(10L, relation.getId());
        assertEquals(source, relation.getSourceIssue());
        assertEquals(target, relation.getTargetIssue());
        assertEquals(RelationType.DEPENDS_ON, relation.getType());
        assertTrue(relation.isManager());
        assertEquals(creator, relation.getCreatedBy());
    }

    @Test
    void testSetters() {
        IssueRelation relation = new IssueRelation();
        relation.setManager(false);
        relation.setType(RelationType.LINKED);

        assertFalse(relation.isManager());
        assertEquals(RelationType.LINKED, relation.getType());
    }
}