package com.github.utransnet.simulator.externalapi.h2impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;

/**
 * Created by Artem on 13.02.2018.
 */
@MappedSuperclass
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "class")
@JsonIgnoreProperties("operationType")
@EqualsAndHashCode(of = "id")
public abstract class BaseOperationH2 implements BaseOperation {

    @Transient
    @JsonIgnore
    @Setter(AccessLevel.PACKAGE)
    protected APIObjectFactoryH2 apiObjectFactory;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected Long id;

    @Getter
    @Setter
    Instant creationDate;


    BaseOperationH2() {
    }

    BaseOperationH2(APIObjectFactoryH2 apiObjectFactory) {
        this.apiObjectFactory = apiObjectFactory;
    }

    @Override
    public String getId() {
        return String.valueOf(id);
    }


}
