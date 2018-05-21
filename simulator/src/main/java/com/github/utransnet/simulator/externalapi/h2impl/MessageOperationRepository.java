package com.github.utransnet.simulator.externalapi.h2impl;

import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import org.springframework.data.repository.CrudRepository;

import java.util.Collection;
import java.util.List;

/**
 * Created by Artem on 13.02.2018.
 */
public interface MessageOperationRepository extends CrudRepository<MessageOperationH2, String> {
    List<MessageOperationH2> findByToOrFrom(String to, String from);
    List<MessageOperationH2> findByTo(String to);
    List<MessageOperationH2> findByFrom(String from);
}
