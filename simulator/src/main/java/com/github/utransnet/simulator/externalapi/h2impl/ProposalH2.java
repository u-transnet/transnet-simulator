package com.github.utransnet.simulator.externalapi.h2impl;

import com.github.utransnet.simulator.externalapi.Proposal;
import com.github.utransnet.simulator.externalapi.UserAccount;
import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.jetbrains.annotations.Nullable;

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
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Getter
    @Setter
    Instant creationDate;

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "approves_added", joinColumns = @JoinColumn(name = "proposal_h2_id"))
    private final List<String> approvesAdded;

    @ElementCollection(fetch=FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "approves_to_add", joinColumns = @JoinColumn(name = "proposal_h2_id"))
    private final List<String> approvesToAdd;


    @Nullable
    private String feePayer;

    @Column(length = 1024)
    private String operationJson;

    ProposalH2() {
        approvesToAdd = new ArrayList<>(4);
        approvesAdded = new ArrayList<>(4);
    }

    ProposalH2(
            APIObjectFactoryH2 apiObjectFactory,
            UserAccount proposingAccount,
            @Nullable UserAccount feePayer,
            BaseOperation operation
    ) {
        this();
        this.apiObjectFactory = apiObjectFactory;
        this.feePayer = (feePayer != null) ? feePayer.getId() : null;
        this.operationJson = apiObjectFactory.operationToJson(operation);
        approvesToAdd.add(proposingAccount.getId());
    }

    protected ProposalH2(
            APIObjectFactoryH2 apiObjectFactory,
            Long id,
            Instant creationDate,
            List<String> approvesAdded,
            List<String> approvesToAdd,
            String operationJson
    ) {
        this.apiObjectFactory = apiObjectFactory;
        this.id = id;
        this.creationDate = creationDate;
        this.approvesAdded = approvesAdded;
        this.approvesToAdd = approvesToAdd;
        this.operationJson = operationJson;
    }

    @Override
    public boolean approved() {
        List<String> tmp = new ArrayList<>(approvesToAdd);
        tmp.removeAll(approvesAdded);
        return tmp.isEmpty();
    }


    @Nullable
    @Override
    public UserAccount getFeePayer() {
        if (feePayer != null) {
            return apiObjectFactory.userAccount(feePayer);
        } else {
            return null;
        }
    }


    @Override
    public List<String> neededApprovals() {
        List<String> tmp = new ArrayList<>(approvesToAdd);
        tmp.removeAll(approvesAdded);
        return tmp;
    }

    @Override
    public List<String> requiredApprovals() {
        return approvesToAdd;
    }

    @Override
    public List<String> availableApprovals() {
        return approvesAdded;
    }


    @Override
    public BaseOperation getOperation() {
        return apiObjectFactory.operationFromJson(operationJson);
    }


    void addApprove(UserAccount userAccount) {
        approvesAdded.add(userAccount.getId());
    }


    @Override
    public String getId() {
        return String.valueOf(id);
    }

    void clear() {
        approvesAdded.clear();
        approvesToAdd.clear();
    }

    @Override
    public String toString() {
        return "ProposalH2{" +
                "approvesAdded=" + approvesAdded +
                ", approvesToAdd=" + approvesToAdd +
                ", feePayer='" + feePayer + '\'' +
                ", operationJson='" + operationJson + '\'' +
                '}';
    }

    ProposalH2 copyWithoutFeePayer() {
        return new ProposalH2(
                apiObjectFactory,
                id,
                creationDate,
                approvesAdded,
                approvesToAdd,
                operationJson
        );
    }
}
