package com.github.utransnet.simulator.externalapi.operations;

import com.github.utransnet.simulator.externalapi.Proposal;

/**
 * Created by Artem on 19.02.2018.
 */
public interface ProposalCreateOperation extends BaseOperation {

    Proposal getProposal();
    @Override
    default OperationType getOperationType() {
        return OperationType.PROPOSAL_CREATE;
    }
}
