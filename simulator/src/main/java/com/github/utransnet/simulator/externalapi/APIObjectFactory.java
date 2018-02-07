package com.github.utransnet.simulator.externalapi;

import com.github.utransnet.simulator.externalapi.operations.ProposalCreateOperation;

/**
 * Created by Artem on 02.02.2018.
 */
public interface APIObjectFactory {

    Asset createAsset(String id);
    AssetAmount createAssetAmount(Asset asset, long amount);
    default AssetAmount createAssetAmount(String id, long amount) {
        return createAssetAmount(createAsset(id), amount);
    }


    Proposal convertProposalOperationToProposalObject(ProposalCreateOperation proposalCreateOperation);
}
