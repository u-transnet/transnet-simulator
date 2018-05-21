package com.github.utransnet.simulator.externalapi.graphenej;

import com.github.utransnet.graphenej.operations.TransferOperation;
import com.github.utransnet.simulator.externalapi.APIObjectFactory;
import com.github.utransnet.simulator.externalapi.operations.ProposalCreateOperation;
import lombok.Getter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Artem on 03.04.2018.
 */
public class ProposalCreateOperationGraphene extends BaseOperationGraphene implements ProposalCreateOperation {

    @Getter
    private ProposalGraphene proposal;

    ProposalCreateOperationGraphene(
            APIObjectFactory apiObjectFactory,
            com.github.utransnet.graphenej.operations.ProposalCreateOperation proposalCreateOperation,
            OperationConverter operationConverter
    ) {
        //TODO: add support for other operations, until only transfer operation
        TransferOperation op = (TransferOperation) proposalCreateOperation.getProposedOps().get(0).op;
        this.proposal = new ProposalGraphene(
                "",
                Collections.singletonList(op.getFrom().getObjectId()),
                operationConverter.fromGrapheneOp(op)
        );
        proposal.setFeePayer(apiObjectFactory.userAccount(proposalCreateOperation.getFeePayingAccount().getObjectId()));
    }

    @Override
    public Set<String> getAffectedAccounts() {
        Set<String> accounts = new HashSet<>();
        accounts.addAll(proposal.requiredApprovals());
        if (proposal.getOperation() != null) {
            accounts.addAll(proposal.getOperation().getAffectedAccounts());
        }
        return accounts;
    }
}
