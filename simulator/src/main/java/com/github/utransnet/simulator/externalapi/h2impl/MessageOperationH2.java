package com.github.utransnet.simulator.externalapi.h2impl;

import com.fasterxml.jackson.annotation.*;
import com.github.utransnet.simulator.externalapi.APIObjectFactory;
import com.github.utransnet.simulator.externalapi.UserAccount;
import com.github.utransnet.simulator.externalapi.operations.MessageOperation;
import com.github.utransnet.simulator.externalapi.operations.OperationType;
import lombok.EqualsAndHashCode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * Created by Artem on 13.02.2018.
 */
@Entity
public class MessageOperationH2 extends BaseOperationH2 implements MessageOperation {


    @Column(name = "from_")
    private String from;
    @Column(name = "to_")
    private String to;
    private String message;

    MessageOperationH2() {
    }

    MessageOperationH2(APIObjectFactoryH2 apiObjectFactory, UserAccount from, UserAccount to, String message) {
        super(apiObjectFactory);
        this.from = from.getId();
        this.to = to.getId();
        this.message = message;
    }

    @JsonCreator
    MessageOperationH2(
            @JsonProperty("fromStr") String from,
            @JsonProperty("toStr") String to,
            @JsonProperty("message") String message
    ) {
        this.from = from;
        this.to = to;
        this.message = message;
    }

    @JsonGetter
    String getToStr() {
        return to;
    }

    @JsonGetter
    String getFromStr() {
        return from;
    }

    @JsonIgnore
    @Override
    public UserAccount getTo() {
        return apiObjectFactory.userAccount(to);
    }

    @JsonIgnore
    @Override
    public UserAccount getFrom() {
        return apiObjectFactory.userAccount(from);
    }

    @Override
    public String getMessage() {
        return message;
    }
}
