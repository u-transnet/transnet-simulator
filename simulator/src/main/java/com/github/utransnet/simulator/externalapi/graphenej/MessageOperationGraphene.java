package com.github.utransnet.simulator.externalapi.graphenej;

import com.github.utransnet.graphenej.operations.TransferOperation;
import com.github.utransnet.simulator.externalapi.APIObjectFactory;
import com.github.utransnet.simulator.externalapi.operations.MessageOperation;
import com.github.utransnet.simulator.externalapi.operations.OperationType;

/**
 * Created by Artem on 05.04.2018.
 */
public class MessageOperationGraphene extends TransferOperationGraphene implements MessageOperation {
    public MessageOperationGraphene(TransferOperation operation, APIObjectFactory objectFactory) {
        super(operation, objectFactory);
    }

    @Override
    public String getMessage() {
        return getMemo();
    }

    @Override
    public OperationType getOperationType() {
        return OperationType.MESSAGE;
    }
}
