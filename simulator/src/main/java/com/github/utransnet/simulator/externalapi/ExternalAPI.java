package com.github.utransnet.simulator.externalapi;

import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import com.github.utransnet.simulator.externalapi.operations.OperationType;
import com.github.utransnet.simulator.externalapi.operations.ProposalCreateOperation;
import com.github.utransnet.simulator.externalapi.operations.TransferOperation;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Artem on 31.01.2018.
 */
public abstract class ExternalAPI {
    abstract void sendProposal(UserAccount from, UserAccount to, UserAccount proposalCreator, Asset asset, long amount);
    abstract void approveProposal(UserAccount feePayer, Proposal proposal);


    abstract void sendAsset(UserAccount from, UserAccount to, AssetAmount assetAmount, String memo);
    abstract void sendMessage(UserAccount from, UserAccount to, String message);

    abstract List<BaseOperation> getAccountHistory(UserAccount account, OperationType operationType);

    @SuppressWarnings("unchecked")
    protected <T extends BaseOperation> List<T> filterHistory(UserAccount account, OperationType operationType, Class<T> clazz) {
        return getAccountHistory(account, OperationType.TRANSFER)
                .stream()
                .filter(baseOperation -> baseOperation.getClass().equals(clazz))
                .map(baseOperation -> (T) baseOperation)
                .collect(Collectors.toList());
    }
    List<TransferOperation> getAccountTransfers(UserAccount account) {
        return filterHistory(account, OperationType.TRANSFER, TransferOperation.class);
    };

    List<Proposal> getAccountProposals(UserAccount account) {
        return null; //TODO
    }

    abstract UserAccount createAccount(String name);
    public abstract UserAccount getAccountByName(String name);

    abstract BaseOperation getLastOperation(UserAccount account);
    abstract List<BaseOperation> operationsBefore(String operationId);
    abstract List<BaseOperation> operationsBefore(BaseOperation operation);

    abstract UserAccount getUserAccountByName(String name);
}
