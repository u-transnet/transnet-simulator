package com.github.utransnet.simulator.externalapi.h2impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.utransnet.simulator.externalapi.UserAccount;
import com.github.utransnet.simulator.externalapi.operations.MessageOperation;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Artem on 13.02.2018.
 */
@Entity
public class MessageOperationH2 extends BaseOperationH2 implements MessageOperation {


    @Column(name = "from_")
    private String from;
    @Column(name = "to_")
    private String to;

    //    @Lob // if varchar(2048) not enough
    @Column(length = 2048)
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

    @JsonIgnore
    @Override
    public Set<String> getAffectedAccounts() {
        return new HashSet<>(Arrays.asList(from, to));
    }

    @Override
    public String toString() {
        return "MessageOperationH2{" +
                "from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
