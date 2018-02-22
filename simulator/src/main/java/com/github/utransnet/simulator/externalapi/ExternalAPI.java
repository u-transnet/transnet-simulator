package com.github.utransnet.simulator.externalapi;

import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import com.github.utransnet.simulator.externalapi.operations.MessageOperation;
import com.github.utransnet.simulator.externalapi.operations.OperationType;
import com.github.utransnet.simulator.externalapi.operations.TransferOperation;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * Created by Artem on 31.01.2018.
 */
public abstract class ExternalAPI {

    //TODO: add memo
    public abstract void sendProposal(
            UserAccount from,
            UserAccount to,
            UserAccount proposingAccount,
            UserAccount feePayer,
            Asset asset,
            long amount
    );
    public abstract void approveProposal(UserAccount approvingAccount, Proposal proposal);


    public abstract void sendAsset(UserAccount from, UserAccount to, AssetAmount assetAmount, String memo);
    public abstract void sendMessage(UserAccount from, UserAccount to, String message);

    public abstract List<? extends BaseOperation> getAccountHistory(UserAccount account, OperationType operationType);
    public abstract List<? extends BaseOperation> getAccountHistory(UserAccount account);

    @SuppressWarnings("unchecked")
    protected <T extends BaseOperation> List<T> filterHistory(UserAccount account, OperationType operationType) {
        return getAccountHistory(account, operationType)
                .stream()
                .filter(baseOperation -> baseOperation.getClass().equals(operationType.clazz))
                .map(baseOperation -> (T) baseOperation)
                .collect(toList());
    }
    public List<TransferOperation> getAccountTransfers(UserAccount account) {
        return filterHistory(account, OperationType.TRANSFER);
    }
    public List<MessageOperation> getAccountMessages(UserAccount account) {
        return filterHistory(account, OperationType.MESSAGE);
    }

    public List<Proposal> getAccountProposals(UserAccount account) {
        return null; //TODO
    }

    public abstract UserAccount createAccount(String name);
    public abstract UserAccount getAccountByName(String name);
    public abstract UserAccount getAccountById(String name);

    public abstract Optional<? extends BaseOperation> getLastOperation(UserAccount account);
    public abstract List<? extends BaseOperation> operationsAfter(UserAccount account, String operationId);
    public List<? extends BaseOperation> operationsAfter(UserAccount account, @Nullable BaseOperation operation) {
        if (operation != null){
            return operationsAfter(account, operation.getId());
        } else {
            return operationsAfter(account, "");
        }
    }


    //region listeners
    public abstract void listenAccountUpdatesByUserId(
            String listenerId,
            Set<String> accsToListen,
            Consumer<AccountUpdateObject> onUpdate
    );
    public void listenAccountUpdates(
            String listenerId,
            Set<UserAccount> accsToListen,
            Consumer<AccountUpdateObject> onUpdate
    ){
        listenAccountUpdatesByUserId(
                listenerId,
                accsToListen.stream().map(UserAccount::getId).collect(toSet()),
                onUpdate
        );
    }
    public abstract void removeAccountUpdateListener(String listenerId);

    public abstract void listenAccountOperationsByUserId(
            String listenerId,
            Set<String> accsToListen,
            Consumer<ExternalObject> onUpdate
    );
    public void listenAccountOperations(
            String listenerId,
            Set<UserAccount> accsToListen,
            Consumer<ExternalObject> onUpdate){
        listenAccountOperationsByUserId(
                listenerId,
                accsToListen.stream().map(UserAccount::getId).collect(toSet()),
                onUpdate
        );
    }
    public abstract void removeAccountOperationListener(String listenerId);

    //endregion
}
