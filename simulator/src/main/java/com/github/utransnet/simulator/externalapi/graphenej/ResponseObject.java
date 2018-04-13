package com.github.utransnet.simulator.externalapi.graphenej;

import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;

/**
 * Created by Artem on 02.04.2018.
 */
class ResponseObject<T> {

    @Getter
    @Setter
    @Nullable
    private T result;
}
