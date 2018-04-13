package com.github.utransnet.simulator.externalapi.graphenej;

import com.github.utransnet.simulator.externalapi.ExternalAPI;
import com.github.utransnet.simulator.externalapi.UserAccount;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;

/**
 * Created by Artem on 01.04.2018.
 */
public class UserAccountGraphene extends UserAccount implements GrapheneWrapper<com.github.utransnet.graphenej.UserAccount> {
    @Getter(AccessLevel.PACKAGE)
    private ECKey key;
    @Getter
    @Setter(AccessLevel.PACKAGE)
    private String id;
    @Getter
    @Setter(AccessLevel.PACKAGE)
    private String name = null;

    private final PrivateKeysSharedPool privateKeysSharedPool;

    protected UserAccountGraphene(ExternalAPI externalAPI, PrivateKeysSharedPool privateKeysSharedPool) {
        super(externalAPI);
        this.privateKeysSharedPool = privateKeysSharedPool;
    }

    @Override
    public void setKey(String wif) {
        super.setKey(wif);
        this.key = DumpedPrivateKey.fromBase58(null, wif).getKey();
        privateKeysSharedPool.put(getId(), key);
    }


    @Override
    public com.github.utransnet.graphenej.UserAccount getRaw() {
        return new com.github.utransnet.graphenej.UserAccount(id, name);
    }
}
