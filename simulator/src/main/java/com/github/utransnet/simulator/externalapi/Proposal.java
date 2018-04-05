package com.github.utransnet.simulator.externalapi;

import com.github.utransnet.simulator.externalapi.operations.BaseOperation;

import java.util.List;

/**
 * Created by Artem on 07.02.2018.
 */
public interface Proposal extends ExternalObject {

    boolean approved();

    @Deprecated
        //TODO: remove method
    UserAccount getFeePayer();

    /**
     * In Our case we use only active approvals, not owner
     *
     * @return
     */
    List<String> neededApprovals();

    /**
     * In Our case we use only active approvals, not owner
     *
     * @return
     */
    List<String> requiredApprovals();

    /**
     * In Our case we use only active approvals, not owner
     *
     * @return
     */
    List<String> availableApprovals();

    /**
     * Propopasl can contain many operations, but in our case we use only one
     *
     * @return
     */
    BaseOperation getOperation();
}
