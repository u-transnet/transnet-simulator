package com.github.utransnet.simulator.externalapi.graphenej;

import com.github.utransnet.graphenej.AssetAmount;
import com.github.utransnet.graphenej.UserAccount;
import com.github.utransnet.graphenej.errors.ChecksumException;
import com.github.utransnet.graphenej.objects.Memo;
import com.github.utransnet.simulator.Utils;
import com.github.utransnet.simulator.externalapi.APIObjectFactory;
import com.github.utransnet.simulator.externalapi.Asset;
import com.github.utransnet.simulator.externalapi.operations.TransferOperation;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

/**
 * Created by Artem on 04.04.2018.
 */
@Slf4j
public class TransferOperationGraphene extends BaseOperationGraphene implements TransferOperation {

    private final APIObjectFactory objectFactory;
    private UserAccount to;
    private UserAccount from;
    private AssetAmount assetAmount;
    private Memo memo;
    private String decodedMemo = null;


    TransferOperationGraphene(
            com.github.utransnet.graphenej.operations.TransferOperation operation,
            APIObjectFactory objectFactory,
            PrivateKeysSharedPool privateKeysSharedPool
    ) {
        this.objectFactory = objectFactory;
        to = operation.getTo();
        from = operation.getFrom();
        assetAmount = operation.getAssetAmount();
        memo = operation.getMemo();
        if (memo.getNonce() != null) {
            try {
                decodedMemo = Memo.decryptMessage(
                        privateKeysSharedPool.get(from.getObjectId()),
                        memo.getDestination(),
                        memo.getNonce(),
                        memo.getByteMessage()
                );
                log.debug(decodedMemo);
            } catch (ChecksumException e) {
                log.error("Error while decrypting memo", e);
            } catch (Exception e) {
                log.error("Error while decrypting memo", e);
            }
        }
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
        return objectFactory.getAssetAmount(assetAmount.getAsset().getObjectId(), assetAmount.getAmount().longValue());
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
        if (decodedMemo != null) {
            return decodedMemo;
        } else {
            return memo.getPlaintextMessage();
        }
    }

    @Override
    public Set<String> getAffectedAccounts() {
        return Utils.setOf(to.getObjectId(), from.getObjectId());
    }
}
