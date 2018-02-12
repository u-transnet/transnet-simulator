package com.github.utransnet.simulator.testservices;

import com.github.utransnet.simulator.externalapi.*;
import com.github.utransnet.simulator.externalapi.operations.ProposalCreateOperation;

/**
 * Created by Artem on 09.02.2018.
 */
public class APIObjectFactoryTestImpl implements APIObjectFactory {

    private final ExternalAPI externalAPI;

    public APIObjectFactoryTestImpl(ExternalAPI externalAPI) {
        this.externalAPI = externalAPI;
    }

    @Override
    public Asset createAsset(String id) {
        return () -> id;
    }

    @Override
    public AssetAmount createAssetAmount(Asset asset, long amount) {
        return new AssetAmount() {
            @Override
            public Asset getAsset() {
                return asset;
            }

            @Override
            public long getAmount() {
                return amount;
            }
        };
    }

    @Override
    public UserAccount createUserAccount(String name) {
        return new UserAccount(externalAPI) {
            @Override
            public String getName() {
                return getId();
            }

            @Override
            public String getId() {
                return name;
            }
        };
    }

    @Override
    public UserAccount createOrGetUserAccount(String name) {
        return createUserAccount(name);
    }

    @Override
    public Proposal convertProposalOperationToProposalObject(ProposalCreateOperation proposalCreateOperation) {
        return null;
    }
}
