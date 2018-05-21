package com.github.utransnet.simulator.externalapi.operations;

import com.github.utransnet.simulator.externalapi.Proposal;

/**
 * Created by Artem on 19.02.2018.
 */
public interface ProposalDeleteOperation extends BaseOperation {

    String getProposalId();
    @Override
    default OperationType getOperationType() {
        return OperationType.PROPOSAL_DELETE;
    }
}
