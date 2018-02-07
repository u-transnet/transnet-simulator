package com.github.utransnet.simulator.externalapi.operations;

/**
 * Created by Artem on 31.01.2018.
 */
public enum OperationType {
    //TODO: set proper id from graphene
    TRANSFER(1),
    MESSAGE(2),
    PROPOSAL_CREATE_OPERATION(3),
    PROPOSAL_UPDATE_OPERATION(4),
    PROPOSAL_DELETE_OPERATION(5);

    int id;

    OperationType(int id) {
        this.id = id;
    }
}
