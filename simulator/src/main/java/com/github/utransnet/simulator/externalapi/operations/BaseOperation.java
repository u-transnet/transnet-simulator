package com.github.utransnet.simulator.externalapi.operations;

import com.github.utransnet.simulator.externalapi.ExternalObject;

/**
 * Created by Artem on 02.02.2018.
 */
public interface BaseOperation extends ExternalObject {

    OperationType getOperationType();
}
