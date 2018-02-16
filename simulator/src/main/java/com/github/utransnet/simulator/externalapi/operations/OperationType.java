package com.github.utransnet.simulator.externalapi.operations;

/**
 * Created by Artem on 31.01.2018.
 */
public enum OperationType {
    //TODO: set proper id from graphene
    TRANSFER(1, TransferOperation.class),
    MESSAGE(2, MessageOperation.class);

    public int id;
    public Class<? extends BaseOperation> clazz;

    OperationType(int id, Class<? extends BaseOperation> clazz) {
        this.id = id;
        this.clazz = clazz;
    }
}
