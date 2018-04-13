package com.github.utransnet.simulator.externalapi.h2impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.utransnet.simulator.externalapi.APIObjectFactory;
import com.github.utransnet.simulator.externalapi.Asset;
import com.github.utransnet.simulator.externalapi.AssetAmount;
import com.github.utransnet.simulator.externalapi.UserAccount;
import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import lombok.SneakyThrows;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

import java.io.IOException;

/**
 * Created by Artem on 14.02.2018.
 */
public class APIObjectFactoryH2 implements APIObjectFactory {

    private final ApplicationContext context;

    APIObjectFactoryH2(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public Asset getAsset(String idOrSymbol) {
        return new AssetH2(idOrSymbol);
    }

    @Override
    public AssetAmount getAssetAmount(Asset asset, long amount) {
        return new AssetAmountH2(asset, amount);
    }

    @Override
    public UserAccount userAccount(String name) {
        UserAccount userAccount = context.getBean(UserAccount.class);
        if(userAccount instanceof UserAccountH2){
            UserAccountH2 userAccountH2 = (UserAccountH2) userAccount;
            userAccountH2.setName(name);
            return userAccountH2;
        } else {
            throw new IllegalArgumentException("Only H2 implementation can be used with this realization");
        }
    }

    @SneakyThrows({IOException.class})
    BaseOperationH2 operationFromJson(String json) {
        BaseOperationH2 baseOperation = new ObjectMapper().readValue(json, BaseOperationH2.class);
        baseOperation.setApiObjectFactory(this);
        return baseOperation;
    }

    @SneakyThrows({IOException.class})
    String operationToJson(BaseOperation baseOperation) {
        Assert.notNull(baseOperation, "Proposed operation can't be null");
        if(baseOperation instanceof BaseOperationH2){
            BaseOperationH2 baseOperationH2 = (BaseOperationH2) baseOperation;
            return new ObjectMapper().writeValueAsString(baseOperationH2);
        } else {
            throw new IllegalArgumentException("Only H2 implementation can be serialized");
        }
    }
}
