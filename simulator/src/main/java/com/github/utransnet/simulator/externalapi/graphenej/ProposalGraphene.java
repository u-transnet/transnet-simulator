package com.github.utransnet.simulator.externalapi.graphenej;

import com.github.utransnet.graphenej.GrapheneObject;
import com.github.utransnet.simulator.externalapi.Proposal;
import com.github.utransnet.simulator.externalapi.UserAccount;
import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Artem on 03.04.2018.
 */
public class ProposalGraphene implements Proposal, GrapheneWrapper<com.github.utransnet.graphenej.objects.Proposal> {


    private com.github.utransnet.graphenej.objects.Proposal raw = null;

    private List<String> requiredApprovals;
    private List<String> availableApprovals;

    @Nullable
    @Getter
    @Setter(AccessLevel.PACKAGE)
    UserAccount feePayer;

    @Nullable
    @Getter
    private BaseOperation operation;

    @Getter
    @Setter(AccessLevel.PACKAGE)
    private String id;

    ProposalGraphene(
            String id,
            List<String> requiredApprovals,
            BaseOperation proposedOp
    ) {
        this(id, requiredApprovals, new ArrayList<>(0), proposedOp);
    }

    private ProposalGraphene(
            String id,
            List<String> requiredApprovals,
            List<String> availableApprovals,
            @Nullable BaseOperation proposedOp) {
        this.id = id;
        this.requiredApprovals = requiredApprovals;
        this.availableApprovals = availableApprovals;
        this.operation = proposedOp;
    }

    ProposalGraphene(String id) {
        this(id, Collections.emptyList(), Collections.emptyList(), null);
    }

    ProposalGraphene(com.github.utransnet.graphenej.objects.Proposal raw, OperationConverter operationConverter) {
        this(
                raw.getObjectId(),
                raw.getRequiredActiveApprovals()
                        .getList()
                        .stream()
                        .map(GrapheneObject::getObjectId)
                        .collect(Collectors.toList()),
                raw.getAvailableActiveApprovals()
                        .getList()
                        .stream()
                        .map(GrapheneObject::getObjectId)
                        .collect(Collectors.toList()),
                operationConverter.fromGrapheneOp(raw.getProposedTransaction().getOperations().get(0))
        );
        this.raw = raw;
    }

    @Override
    public boolean approved() {
        return requiredApprovals().isEmpty();
    }


    @Override
    public List<String> neededApprovals() {
        List<String> approvals = new ArrayList<>(requiredApprovals());
        approvals.removeAll(availableApprovals());
        return approvals;
    }

    @Override
    public List<String> requiredApprovals() {
        return requiredApprovals;
    }

    @Override
    public List<String> availableApprovals() {
        return availableApprovals;
    }

    @Override
    public com.github.utransnet.graphenej.objects.Proposal getRaw() {
        return raw;
    }
}
