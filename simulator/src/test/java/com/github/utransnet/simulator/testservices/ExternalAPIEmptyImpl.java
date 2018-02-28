package com.github.utransnet.simulator.testservices;

import com.github.utransnet.simulator.externalapi.*;
import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import com.github.utransnet.simulator.externalapi.operations.OperationType;

import java.util.*;
import java.util.function.Consumer;

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

    @Override
    public List<Proposal> getAccountProposals(UserAccount account) {
        return null;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public UserAccount createAccount(String name) {
        return new UserAccount(this) {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getId() {
                return name;
            }
        };
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public UserAccount getAccountByName(String name) {
        return createAccount(name);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public UserAccount getAccountById(String name) {
        return createAccount(name);
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

    @Override
    public void listenAccountUpdatesByUserId(String listenerId, Set<String> accsToListen, Consumer<AccountUpdateObject> onUpdate) {

    }

    @Override
    public void removeAccountUpdateListener(String listenerId) {

    }

    @Override
    public void listenAccountOperationsByUserId(String listenerId, Set<String> accsToListen, Consumer<ExternalObject> onUpdate) {

    }

    @Override
    public void removeAccountOperationListener(String listenerId) {

    }

}
