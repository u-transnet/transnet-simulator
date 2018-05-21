package com.github.utransnet.simulator.externalapi.h2impl;

import com.github.utransnet.simulator.externalapi.AccountUpdateObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Created by Artem on 21.02.2018.
 */
@AllArgsConstructor
class AccountUpdateListener<T> {
    @Getter
    Set<String> accsToListen;
    Consumer<T> onUpdate;

    void fire(T obj) {
        onUpdate.accept(obj);
    }
}
