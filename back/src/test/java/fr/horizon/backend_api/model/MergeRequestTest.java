package fr.horizon.backend_api.model;

import fr.horizon.backend_api.model.enums.MergeRequestStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MergeRequestTest {

    @Test
    void testMergeRequestBuilderAndGetters() {
        Issue source = new Issue();
        source.setId(1L);

        Issue target = new Issue();
        target.setId(2L);

        User requester = new User();
        requester.setId(3L);


        MergeRequest request = MergeRequest.builder()
                .id(100L)
                .sourceIssue(source)
                .targetIssue(target)
                .requester(requester)
                .status(MergeRequestStatus.PENDING)
                .rejectionReason("Incompatible")
                .sourceManagerApproved(true)
                .targetManagerApproved(false)
                .build();

        assertNotNull(request);
        assertEquals(100L, request.getId());
        assertEquals(source, request.getSourceIssue());
        assertEquals(target, request.getTargetIssue());
        assertEquals(requester, request.getRequester());
        assertEquals(MergeRequestStatus.PENDING, request.getStatus());
        assertEquals("Incompatible", request.getRejectionReason());
        assertTrue(request.getSourceManagerApproved());
        assertFalse(request.getTargetManagerApproved());
    }

    @Test
    void testDefaultValues() {
        MergeRequest request = MergeRequest.builder().build();

        assertEquals(MergeRequestStatus.PENDING, request.getStatus(),
                "Le statut par défaut doit être PENDING");
    }

    @Test
    void testSetters() {
        MergeRequest request = new MergeRequest();
        request.setRejectionReason("Doublon");
        request.setSourceManagerApproved(true);

        assertEquals("Doublon", request.getRejectionReason());
        assertTrue(request.getSourceManagerApproved());
    }
}