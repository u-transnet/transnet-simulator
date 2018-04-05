package com.github.utransnet.simulator.externalapi.operations;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Created by Artem on 31.01.2018.
 */
public enum OperationType {
    TRANSFER(0, TransferOperation.class),
    MESSAGE(50, MessageOperation.class),
    PROPOSAL_CREATE(22, ProposalCreateOperation.class),
    PROPOSAL_UPDATE(23, ProposalUpdateOperation.class),
    PROPOSAL_DELETE(24, ProposalDeleteOperation.class);

    public int id;
    public Class<? extends BaseOperation> clazz;

    OperationType(int id, Class<? extends BaseOperation> clazz) {
        this.id = id;
        this.clazz = clazz;
    }

    public static OperationType fromId(int id) {
        Optional<OperationType> first = Stream.of(OperationType.values()).filter(type -> type.id == id).findFirst();
        if (!first.isPresent()) {
            throw new IllegalArgumentException("Unknown id '" + id + "' of OperationType");
        }
        return first.get();
    }
}
