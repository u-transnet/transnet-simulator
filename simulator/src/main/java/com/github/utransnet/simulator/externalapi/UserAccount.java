package com.github.utransnet.simulator.externalapi;

import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import com.github.utransnet.simulator.externalapi.operations.MessageOperation;
import com.github.utransnet.simulator.externalapi.operations.TransferOperation;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by Artem on 31.01.2018.
 */
public abstract class UserAccount implements ExternalObject {

    private final ExternalAPI externalAPI;

    protected UserAccount(ExternalAPI externalAPI) {
        this.externalAPI = externalAPI;
    }

    public abstract String getName();

    public void sendAsset(UserAccount to, AssetAmount assetAmount, String memo) {
        externalAPI.sendAsset(this, to, assetAmount, memo);
    }

    public void sendMessage(UserAccount to, String message){
        externalAPI.sendMessage(this, to, message);
    }

    public List<Proposal> getProposals(){
        return externalAPI.getAccountProposals(this);
    }

    public List<Proposal> getProposalsFrom(UserAccount from) {
//        externalAPI.getAccountProposals()
        return null; //TODO
    }

    public List<TransferOperation> getTransfers() {
        return externalAPI.getAccountTransfers(this);
    }

    public List<MessageOperation> getMessages() {
        return externalAPI.getAccountMessages(this);
    }

    public List<TransferOperation> getTransfersFrom(UserAccount from) {
        return getTransfers()
                .stream()
                .filter(transferOperation -> transferOperation.getFrom().equals(from))
                .collect(Collectors.toList());
    }

    public void approveProposal(Proposal proposal) {
        externalAPI.approveProposal(this, proposal);
    }


    public Optional<? extends BaseOperation> getLastOperation() {
        return externalAPI.getLastOperation(this);
    }


    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof UserAccount)) return false;
        final UserAccount other = (UserAccount) o;
        if (!other.canEqual(this)) return false;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        return this$id == null ? other$id == null : this$id.equals(other$id);
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof UserAccount;
    }
}
