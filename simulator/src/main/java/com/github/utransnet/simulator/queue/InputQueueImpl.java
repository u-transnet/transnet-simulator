package com.github.utransnet.simulator.queue;

import java.util.AbstractQueue;
import java.util.stream.Stream;

/**
 * Created by Artem on 01.02.2018.
 */
public class InputQueueImpl<T> implements InputQueue<T> {

    private AbstractQueue<T> queue;

    public InputQueueImpl(AbstractQueue<T> queue) {
        this.queue = queue;
    }

    @Override
    public boolean offer(T data) {
        return queue.offer(data);
    }

    @Override
    public T poll() {
        return queue.poll();
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public Stream<T> stream() {
        return queue.stream();
    }
}
