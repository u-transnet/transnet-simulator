package com.github.utransnet.simulator.graphene;

import com.github.utransnet.simulator.Stub;
import com.github.utransnet.simulator.actors.Actor;
import com.github.utransnet.simulator.actors.BaseInfObject;

/**
 * Created by Artem on 31.01.2018.
 */
public interface GrapheneAPI {
    void sendProposal(Actor from, Actor to, Actor proposalCreator, long amount, Asset asset);
    void sendAsset(Actor from, Actor to, long amount, Asset asset);

    void getAccountHistory(Actor account, OperationType operationType);
    void getAccountProposals(Actor account);

    void createAccount(String name);
}
