package com.github.utransnet.simulator.externalapi;

import com.github.utransnet.simulator.externalapi.operations.TransferOperation;
import com.sun.javafx.geom.transform.BaseTransform;

import java.util.List;

/**
 * Created by Artem on 31.01.2018.
 */
public abstract class UserAccount implements ExternalObject {

    private final ExternalAPI externalAPI;

    protected UserAccount(ExternalAPI externalAPI) {
        this.externalAPI = externalAPI;
    }

    abstract String getName();

    public void sendAsset(UserAccount to, AssetAmount assetAmount, String memo) {
        externalAPI.sendAsset(this, to, assetAmount, memo);
    }

    public void sendMessage(UserAccount to, String message){
        externalAPI.sendMessage(this, to, message);
    }

    public List<Proposal> getProposals(){
        return externalAPI.getAccountProposals(this);
    }

    public List<BaseTransform> getTransactionsFrom(UserAccount from) {
        return null; //TODO
    }

    public List<Proposal> getProposalsFrom(UserAccount from) {
        return null; //TODO
    }

    public List<TransferOperation> getTransfersFrom(UserAccount from) {
        return null; //TODO
    }

    public void approveProposal(Proposal proposal) {
        externalAPI.approveProposal(this, proposal);
    }


}
