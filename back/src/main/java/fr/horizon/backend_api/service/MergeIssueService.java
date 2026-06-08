package fr.horizon.backend_api.service;


import fr.horizon.backend_api.exception.IssueNotFoundException;
import fr.horizon.backend_api.model.Issue;
import fr.horizon.backend_api.model.dto.MergeRequestDto;
import fr.horizon.backend_api.repository.IssueRepository;
import fr.horizon.backend_api.repository.MergeRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor

public class MergeIssueService {

    private final IssueRepository issueRepository;
    private final MergeRequestRepository mergeRequestRepository;


    public void mergeIssues(Long issueAid, Long issueBid){
        Issue issueA = issueRepository.findById(issueAid)
                .orElseThrow(() -> new IssueNotFoundException(issueAid));

        Issue issueB  = issueRepository.findById(issueBid)
                .orElseThrow(() -> new IssueNotFoundException(issueBid));

        // recupérer les champs différents et les foutres dans l'autre
    }


    public List<MergeRequestDto> getManagerRequestDto(){

    }

    public void respondToMergeRequest(boolean respond, )

}
