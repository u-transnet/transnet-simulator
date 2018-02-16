package com.github.utransnet.simulator.externalapi.h2impl;

import com.fasterxml.jackson.annotation.*;
import com.github.utransnet.simulator.externalapi.Asset;
import com.github.utransnet.simulator.externalapi.AssetAmount;
import com.github.utransnet.simulator.externalapi.UserAccount;
import com.github.utransnet.simulator.externalapi.operations.OperationType;
import com.github.utransnet.simulator.externalapi.operations.TransferOperation;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Created by Artem on 13.02.2018.
 */
@Entity
public class TransferOperationH2 extends BaseOperationH2 implements TransferOperation {

    @Column(name = "from_")
    private String from;
    @Column(name = "to_")
    private String to;
    private String asset;
    private long amount;
    private String memo;


    TransferOperationH2() {
    }

    TransferOperationH2(APIObjectFactoryH2 apiObjectFactory, UserAccount from, UserAccount to, Asset asset, long amount, String memo) {
        super(apiObjectFactory);
        this.to = to.getId();
        this.from = from.getId();
        this.memo = memo;
        this.asset = asset.getId();
        this.amount = amount;
    }

    TransferOperationH2(APIObjectFactoryH2 apiObjectFactory, UserAccount from, UserAccount to, AssetAmount assetAmount, String memo) {
        this(apiObjectFactory, from, to, assetAmount.getAsset(), assetAmount.getAmount(), memo);
    }

    @JsonIgnore
    @Override
    public UserAccount getTo() {
        return apiObjectFactory.userAccount(to);
    }

    @JsonIgnore
    @Override
    public UserAccount getFrom() {
        return apiObjectFactory.userAccount(from);
    }

    @JsonIgnore
    @Override
    public AssetAmount getAssetAmount() {
        return apiObjectFactory.getAssetAmount(getAsset(), amount);
    }

    @JsonIgnore
    @Override
    public Asset getAsset() {
        return apiObjectFactory.getAsset(asset);
    }

    @Override
    public long getAmount() {
        return amount;
    }

    @Override
    public String getMemo() {
        return memo;
    }

    @JsonIgnore
    @Override
    public OperationType getOperationType() {
        return OperationType.TRANSFER;
    }

    @JsonCreator
    TransferOperationH2(
            @JsonProperty("fromStr") String from,
            @JsonProperty("toStr") String to,
            @JsonProperty("assetStr") String asset,
            @JsonProperty("amount") long amount,
            @JsonProperty("memo") String memo
    ) {
        this.from = from;
        this.to = to;
        this.asset = asset;
        this.amount = amount;
        this.memo = memo;
    }

    @JsonGetter
    String getFromStr() {
        return from;
    }

    @JsonGetter
    String getToStr() {
        return to;
    }

    @JsonGetter
    String getAssetStr() {
        return asset;
    }
}
