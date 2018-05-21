package com.github.utransnet.simulator.externalapi.graphenej;

import com.github.utransnet.graphenej.operations.ProposalCreateOperation;
import com.github.utransnet.graphenej.operations.ProposalUpdateOperation;
import com.github.utransnet.graphenej.operations.TransferOperation;
import com.github.utransnet.simulator.externalapi.APIObjectFactory;
import com.github.utransnet.simulator.externalapi.DefaultAssets;
import com.github.utransnet.simulator.externalapi.operations.OperationType;

import java.util.Objects;

/**
 * Created by Artem on 04.04.2018.
 */
public class OperationConverter {

    private final APIObjectFactory objectFactory;
    private final DefaultAssets defaultAssets;


    public OperationConverter(APIObjectFactory objectFactory, DefaultAssets defaultAssets) {
        this.objectFactory = objectFactory;
        this.defaultAssets = defaultAssets;
    }

    BaseOperationGraphene fromGrapheneOp(com.github.utransnet.graphenej.BaseOperation baseOperation) {
        OperationType type = OperationType.fromId(baseOperation.getId());
        switch (type) {

            case TRANSFER:
                TransferOperation transferOperation = (TransferOperation) baseOperation;
                if (Objects.equals(
                        defaultAssets.getMessageAsset().getId(),
                        transferOperation.getAssetAmount().getAsset().getObjectId()
                )) {
                    return new MessageOperationGraphene(transferOperation, objectFactory);
                } else {
                    return new TransferOperationGraphene(transferOperation, objectFactory);
                }

            case PROPOSAL_UPDATE:
                ProposalUpdateOperation proposalUpdateOperation = (ProposalUpdateOperation) baseOperation;
                return new ProposalUpdateOperationGraphene(proposalUpdateOperation, this);

            case PROPOSAL_CREATE:
                ProposalCreateOperation proposalCreateOperation = (ProposalCreateOperation) baseOperation;
                return new ProposalCreateOperationGraphene(
                        objectFactory,
                        proposalCreateOperation,
                        this
                );

            case PROPOSAL_DELETE:
                throw new IllegalArgumentException("Unsuported OperationType: " + type.name());

            case MESSAGE: //Not exist
            default:
                throw new IllegalArgumentException("Unsuported OperationType: " + type.name());
        }
    }
}
