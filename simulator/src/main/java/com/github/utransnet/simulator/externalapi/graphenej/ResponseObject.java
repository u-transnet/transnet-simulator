package com.github.utransnet.simulator.externalapi.graphenej;

import lombok.Getter;
import lombok.Setter;

/**
 * Created by Artem on 02.04.2018.
 */
public class ResponseObject<T> {

    @Getter
    @Setter
    private T result;
}
