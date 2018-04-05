package com.github.utransnet.simulator.externalapi.graphenej;

import com.github.utransnet.graphenej.AssetAmount;
import com.github.utransnet.graphenej.UserAccount;
import com.github.utransnet.graphenej.objects.Memo;
import com.github.utransnet.simulator.Utils;
import com.github.utransnet.simulator.externalapi.APIObjectFactory;
import com.github.utransnet.simulator.externalapi.Asset;
import com.github.utransnet.simulator.externalapi.operations.TransferOperation;

import java.util.Set;

/**
 * Created by Artem on 04.04.2018.
 */
public class TransferOperationGraphene extends BaseOperationGraphene implements TransferOperation {

    private final APIObjectFactory objectFactory;
    private UserAccount to;
    private UserAccount from;
    private AssetAmount assetAmount;
    private Memo memo;


    TransferOperationGraphene(
            com.github.utransnet.graphenej.operations.TransferOperation operation,
            APIObjectFactory objectFactory
    ) {
        this.objectFactory = objectFactory;
        to = operation.getTo();
        from = operation.getFrom();
        assetAmount = operation.getAssetAmount();
        memo = operation.getMemo();
    }


    @Override
    public com.github.utransnet.simulator.externalapi.UserAccount getTo() {
        return objectFactory.userAccount(to.getObjectId());
    }

    @Override
    public com.github.utransnet.simulator.externalapi.UserAccount getFrom() {
        return objectFactory.userAccount(from.getObjectId());
    }

    @Override
    public com.github.utransnet.simulator.externalapi.AssetAmount getAssetAmount() {
        return new AssetAmountGraphene(assetAmount);
    }

    @Override
    public Asset getAsset() {
        return getAssetAmount().getAsset();
    }

    @Override
    public long getAmount() {
        return getAssetAmount().getAmount();
    }

    @Override
    public String getMemo() {
        //todo decrypt memo
        return memo.getPlaintextMessage();
    }

    @Override
    public Set<String> getAffectedAccounts() {
        return Utils.setOf(to.getObjectId(), from.getObjectId());
    }
}
