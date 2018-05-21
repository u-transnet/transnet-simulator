package com.github.utransnet.simulator.externalapi.operations;

import com.github.utransnet.simulator.externalapi.ExternalObject;

import java.util.Optional;
import java.util.Set;

/**
 * Created by Artem on 02.02.2018.
 */
public interface BaseOperation extends ExternalObject {

    @SuppressWarnings("unchecked")
    static <T extends BaseOperation> Optional<T> convert(BaseOperation operation, OperationType operationType) {
        if (operationType.clazz.isAssignableFrom(operation.getClass())) {
            return Optional.of((T) operation);
        }
        return Optional.empty();
    }

    String getId();

    OperationType getOperationType();

    Set<String> getAffectedAccounts();
}
