package com.github.utransnet.simulator.externalapi.h2impl;

import com.github.utransnet.simulator.externalapi.Proposal;
import com.github.utransnet.simulator.externalapi.operations.ProposalCreateOperation;
import lombok.Getter;

import java.util.Set;

/**
 * Created by Artem on 28.02.2018.
 */
class ProposalCreateOperationH2 extends BaseOperationH2 implements ProposalCreateOperation {

    @Getter
    private Proposal proposal;
    @Getter
    private Set<String> affectedAccounts;

    ProposalCreateOperationH2(ProposalH2 proposal, Set<String> affectedAccounts) {
        this.proposal = proposal;
        this.affectedAccounts = affectedAccounts;
        creationDate = proposal.creationDate;
    }
}
