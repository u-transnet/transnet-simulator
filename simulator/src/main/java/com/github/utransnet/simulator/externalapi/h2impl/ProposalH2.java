package com.github.utransnet.simulator.externalapi.h2impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.utransnet.simulator.externalapi.Proposal;
import com.github.utransnet.simulator.externalapi.UserAccount;
import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Artem on 14.02.2018.
 */
@Entity
public class ProposalH2 implements Proposal {
    @Transient
    @Setter(AccessLevel.PACKAGE)
    protected APIObjectFactoryH2 apiObjectFactory;


    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Getter
    @Setter
    Instant creationDate;

    @ElementCollection(fetch=FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "approves_to_add", joinColumns = @JoinColumn(name = "proposal_h2_id"))
    @Column(name = "user_id")
    private final List<String> approvesToAdd = new ArrayList<>(4);

    @ElementCollection(fetch=FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "approves_added", joinColumns = @JoinColumn(name = "proposal_h2_id"))
    @Column(name = "user_id")
    private final List<String> approved = new ArrayList<>(4);

    private String feePayer;
    private String operationJson;

    public ProposalH2() {
    }

    ProposalH2(
            APIObjectFactoryH2 apiObjectFactory,
            UserAccount proposingAccount,
            UserAccount feePayer,
            BaseOperation operation
    ) {
        this.apiObjectFactory = apiObjectFactory;
        this.feePayer = feePayer.getId();
        approvesToAdd.add(proposingAccount.getId());
        this.operationJson = apiObjectFactory.operationToJson(operation);
    }

    @Override
    public boolean approved() {
        List<String> tmp = new ArrayList<>(approvesToAdd);
        tmp.removeAll(approved);
        return tmp.isEmpty();
    }


    @Override
    public UserAccount getFeePayer() {
        return apiObjectFactory.userAccount(feePayer);
    }


    @Override
    public List<String> neededApproves() {
        List<String> tmp = new ArrayList<>(approvesToAdd);
        tmp.removeAll(approved);
        return tmp;
    }


    @Override
    public BaseOperation getOperation() {
        return apiObjectFactory.operationFromJson(operationJson);
    }

    @Override
    public void addApprove(UserAccount userAccount) {
        approved.add(userAccount.getId());
    }


    @Override
    public String getId() {
        return String.valueOf(id);
    }
}
