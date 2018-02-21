package com.github.utransnet.simulator.externalapi;

import com.github.utransnet.simulator.externalapi.operations.BaseOperation;

import java.util.List;

/**
 * Created by Artem on 07.02.2018.
 */
public interface Proposal extends ExternalObject {

    boolean approved();

    UserAccount getFeePayer();

    List<String> neededApproves();

    BaseOperation getOperation();

    void addApprove(UserAccount userAccount);
}
