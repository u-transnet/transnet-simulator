package com.github.utransnet.simulator.externalapi.h2impl;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * Created by Artem on 13.02.2018.
 */
public interface TransferOperationH2Repository extends CrudRepository<TransferOperationH2, String> {
    List<TransferOperationH2> findByToOrFrom(String to, String from);
}
