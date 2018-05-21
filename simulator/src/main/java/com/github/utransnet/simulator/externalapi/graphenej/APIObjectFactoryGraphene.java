package com.github.utransnet.simulator.externalapi.graphenej;

import com.github.utransnet.simulator.externalapi.APIObjectFactory;
import com.github.utransnet.simulator.externalapi.Asset;
import com.github.utransnet.simulator.externalapi.AssetAmount;
import com.github.utransnet.simulator.externalapi.UserAccount;
import org.springframework.context.ApplicationContext;

/**
 * Created by Artem on 05.04.2018.
 */
public class APIObjectFactoryGraphene implements APIObjectFactory {


    private final ApplicationContext context;

    APIObjectFactoryGraphene(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public Asset getAsset(String id) {
        return new AssetGraphene(id);
    }

    @Override
    public AssetAmount getAssetAmount(Asset asset, long amount) {
        return new AssetAmountGraphene((AssetGraphene) asset, amount);
    }

    @Override
    public UserAccount userAccount(String id) {
        UserAccount userAccount = context.getBean(UserAccount.class);
        if (userAccount instanceof UserAccountGraphene) {
            UserAccountGraphene userAccountGraphene = (UserAccountGraphene) userAccount;
            userAccountGraphene.setId(id);
            return userAccountGraphene;
        } else {
            throw new IllegalArgumentException("Only Graphene implementation can be used with this realization");
        }
    }
}
