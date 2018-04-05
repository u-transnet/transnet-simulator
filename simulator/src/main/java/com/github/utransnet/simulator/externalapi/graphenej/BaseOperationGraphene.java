package com.github.utransnet.simulator.externalapi.graphenej;

import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by Artem on 03.04.2018.
 */
abstract class BaseOperationGraphene implements BaseOperation {


    @Getter
    @Setter
    String id;
}
