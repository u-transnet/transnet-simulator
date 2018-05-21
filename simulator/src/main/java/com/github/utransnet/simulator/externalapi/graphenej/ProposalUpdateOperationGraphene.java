package com.github.utransnet.simulator.externalapi.graphenej;

import com.github.utransnet.graphenej.UserAccount;
import com.github.utransnet.simulator.externalapi.operations.ProposalUpdateOperation;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by Artem on 04.04.2018.
 */
public class ProposalUpdateOperationGraphene extends BaseOperationGraphene implements ProposalUpdateOperation {

    @Getter
    private ProposalGraphene proposal;

    @Getter
    private Set<String> affectedAccounts;


    ProposalUpdateOperationGraphene(com.github.utransnet.graphenej.operations.ProposalUpdateOperation proposalUpdateOperation, OperationConverter operationConverter) {
        if (proposalUpdateOperation.getProposal().getProposedTransaction() != null) {
            this.proposal = new ProposalGraphene(proposalUpdateOperation.getProposal(), operationConverter);
        } else {
            this.proposal = new ProposalGraphene(proposalUpdateOperation.getProposal().getObjectId());
        }
        affectedAccounts = new HashSet<>();
        affectedAccounts.addAll(proposal.requiredApprovals());
        if (proposal.getOperation() != null) {
            affectedAccounts.addAll(proposal.getOperation().getAffectedAccounts());
        }
        affectedAccounts.addAll(
                proposalUpdateOperation
                        .getActiveApprovalsToAdd()
                        .getList()
                        .stream()
                        .map(UserAccount::getName)
                        .collect(Collectors.toSet())
        );
        affectedAccounts.addAll(
                proposalUpdateOperation
                        .getActiveApprovalsToRemove()
                        .getList()
                        .stream()
                        .map(UserAccount::getName)
                        .collect(Collectors.toSet())
        );
    }
}
