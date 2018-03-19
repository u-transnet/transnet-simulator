package com.github.utransnet.simulator.externalapi.h2impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.utransnet.simulator.externalapi.Asset;
import com.github.utransnet.simulator.externalapi.AssetAmount;
import com.github.utransnet.simulator.externalapi.UserAccount;
import com.github.utransnet.simulator.externalapi.operations.TransferOperation;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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

    @JsonIgnore
    @Override
    public Set<String> getAffectedAccounts() {
        return new HashSet<>(Arrays.asList(from, to));
    }

    @Override
    public String toString() {
        return "TransferOperationH2{" +
                "from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", asset='" + asset + '\'' +
                ", amount=" + amount +
                ", memo='" + memo + '\'' +
                '}';
    }
}
