package com.github.utransnet.simulator.testservices;

import com.github.utransnet.simulator.externalapi.*;
import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import com.github.utransnet.simulator.externalapi.operations.OperationType;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Created by Artem on 12.02.2018.
 */
public class ExternalAPIEmptyImpl extends ExternalAPI {
    @Override
    public void sendProposal(
            UserAccount from,
            UserAccount to,
            UserAccount proposingAccount,
            UserAccount feePayer,
            Asset asset, long amount) {

    }

    @Override
    public void approveProposal(UserAccount approvingAccount, Proposal proposal) {

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
    public List<BaseOperation> getAccountHistory(UserAccount account) {
        return new ArrayList<>(0);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public UserAccount createAccount(String name) {
        return null;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public UserAccount getAccountByName(String name) {
        return null;
    }

    @Override
    public Optional<BaseOperation> getLastOperation(UserAccount account) {
        return Optional.empty();
    }

    @Override
    public List<BaseOperation> operationsAfter(UserAccount account, String operationId) {
        return new ArrayList<>(0);
    }

    @Override
    public List<BaseOperation> operationsAfter(UserAccount account, BaseOperation operation) {
        return new ArrayList<>(0);
    }

}
