package com.github.utransnet.simulator.externalapi;

import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import com.github.utransnet.simulator.externalapi.operations.OperationType;
import com.github.utransnet.simulator.externalapi.operations.ProposalCreateOperation;
import com.github.utransnet.simulator.externalapi.operations.TransferOperation;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by Artem on 31.01.2018.
 */
public abstract class ExternalAPI {
    public abstract void sendProposal(UserAccount from, UserAccount to, UserAccount proposalCreator, Asset asset, long amount);
    public abstract void approveProposal(UserAccount feePayer, Proposal proposal);


    public abstract void sendAsset(UserAccount from, UserAccount to, AssetAmount assetAmount, String memo);
    public abstract void sendMessage(UserAccount from, UserAccount to, String message);

    public abstract List<BaseOperation> getAccountHistory(UserAccount account, OperationType operationType);

    @SuppressWarnings("unchecked")
    protected <T extends BaseOperation> List<T> filterHistory(UserAccount account, OperationType operationType, Class<T> clazz) {
        return getAccountHistory(account, OperationType.TRANSFER)
                .stream()
                .filter(baseOperation -> baseOperation.getClass().equals(clazz))
                .map(baseOperation -> (T) baseOperation)
                .collect(Collectors.toList());
    }
    public List<TransferOperation> getAccountTransfers(UserAccount account) {
        return filterHistory(account, OperationType.TRANSFER, TransferOperation.class);
    };

    public List<Proposal> getAccountProposals(UserAccount account) {
        return null; //TODO
    }

    public abstract UserAccount createAccount(String name);
    public abstract UserAccount getAccountByName(String name);

    public abstract Optional<BaseOperation> getLastOperation(UserAccount account);
    public abstract List<BaseOperation> operationsBefore(String operationId);
    public abstract List<BaseOperation> operationsBefore(BaseOperation operation);

    public abstract UserAccount getUserAccountByName(String name);
}
