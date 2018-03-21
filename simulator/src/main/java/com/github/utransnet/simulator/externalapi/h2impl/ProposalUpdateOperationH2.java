package com.github.utransnet.simulator.externalapi.h2impl;

import com.github.utransnet.simulator.externalapi.Proposal;
import com.github.utransnet.simulator.externalapi.operations.ProposalUpdateOperation;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

/**
 * Created by Artem on 20.03.2018.
 */
@Getter
@AllArgsConstructor
public class ProposalUpdateOperationH2 extends BaseOperationH2 implements ProposalUpdateOperation {

    private final Proposal proposal;
    private final Set<String> affectedAccounts;

}
