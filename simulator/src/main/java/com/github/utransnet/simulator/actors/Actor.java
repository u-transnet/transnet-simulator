package com.github.utransnet.simulator.actors;

import com.github.utransnet.simulator.graphene.AssetAmount;
import com.github.utransnet.simulator.graphene.UserAccount;
import lombok.Getter;

import java.util.Set;

/**
 * Created by Artem on 31.01.2018.
 */
public abstract class Actor {

    public abstract void update(int seconds);

    @Getter
    private String id;

    Set<AssetAmount> balance;

    private UserAccount uTransnetAccount;

}
