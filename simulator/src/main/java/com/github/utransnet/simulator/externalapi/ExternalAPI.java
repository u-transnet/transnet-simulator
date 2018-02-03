package com.github.utransnet.simulator.externalapi;

import com.github.utransnet.simulator.actors.Actor;

import java.util.List;

/**
 * Created by Artem on 31.01.2018.
 */
public interface ExternalAPI {
    void sendProposal(UserAccount from, UserAccount to, UserAccount proposalCreator, Asset asset, long amount);
    void sendAsset(UserAccount from, UserAccount to, AssetAmount assetAmount);
    void sendAsset(UserAccount from, UserAccount to, Asset asset, long amount);

    List<BaseOperation> getAccountHistory(UserAccount account, OperationType operationType);
    void getAccountProposals(UserAccount account);
    void getAccountTransfers(UserAccount account);

    UserAccount createAccount(String name);

    BaseOperation getLastOperation(UserAccount account);
    List<BaseOperation> operationsBefore(String operationId);
    List<BaseOperation> operationsBefore(BaseOperation operation);
}
