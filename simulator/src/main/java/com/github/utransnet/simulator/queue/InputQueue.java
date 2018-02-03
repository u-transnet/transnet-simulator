package com.github.utransnet.simulator.queue;

import java.util.stream.Stream;

/**
 * Created by Artem on 01.02.2018.
 */
public interface InputQueue<T> {
    boolean offer(T data);

    T poll();

    int size();

    Stream<T> stream();
}
