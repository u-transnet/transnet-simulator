package com.github.utransnet.simulator.testservices;

import com.github.utransnet.simulator.externalapi.*;
import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import com.github.utransnet.simulator.externalapi.operations.OperationType;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Created by Artem on 12.02.2018.
 */
public class ExternalAPIEmptyImpl extends ExternalAPI {
    @Override
    public void sendProposal(UserAccount from, UserAccount to, UserAccount proposalCreator, Asset asset, long amount) {

    }

    @Override
    public void approveProposal(UserAccount feePayer, Proposal proposal) {

    }

    @Override
    public void sendAsset(UserAccount from, UserAccount to, AssetAmount assetAmount, String memo) {

    }

    @Override
    public void sendMessage(UserAccount from, UserAccount to, String message) {

    }

    @Override
    public List<BaseOperation> getAccountHistory(UserAccount account, OperationType operationType) {
        return new LinkedList<>();
    }

    @Override
    public UserAccount createAccount(String name) {
        return null;
    }

    @Override
    public UserAccount getAccountByName(String name) {
        return null;
    }

    @Override
    public Optional<BaseOperation> getLastOperation(UserAccount account) {
        return Optional.empty();
    }

    @Override
    public List<BaseOperation> operationsBefore(String operationId) {
        return new LinkedList<>();
    }

    @Override
    public List<BaseOperation> operationsBefore(BaseOperation operation) {
        return new LinkedList<>();
    }

    @Override
    public UserAccount getUserAccountByName(String name) {
        return null;
    }

}
