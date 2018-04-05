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

    protected UserAccountGraphene(ExternalAPI externalAPI) {
        super(externalAPI);
    }

    void setKey(String wif) {
        key = DumpedPrivateKey.fromBase58(null, wif).getKey();
    }


    @Override
    public com.github.utransnet.graphenej.UserAccount getRaw() {
        return new com.github.utransnet.graphenej.UserAccount(id, name);
    }
}
