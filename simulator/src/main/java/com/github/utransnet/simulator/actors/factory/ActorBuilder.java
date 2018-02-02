package com.github.utransnet.simulator.actors.factory;

import com.github.utransnet.simulator.actors.Actor;
import com.github.utransnet.simulator.actors.Logist;
import com.github.utransnet.simulator.externalapi.*;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Artem on 02.02.2018.
 */
public abstract class ActorBuilder<T extends Actor> {

    protected Class<T> clazz;
    private String id;
    private Set<AssetAmount> balance;
    private UserAccount uTransnetAccount;
    private String lastOperationId;
    private Set<OperationListener> operationListeners;

    @Getter(AccessLevel.PROTECTED)
    private final ApplicationContext context;
    private final APIObjectFactory objectFactory;

    ActorBuilder(ApplicationContext context, APIObjectFactory objectFactory) {
        this.objectFactory = objectFactory;
        this.context = context;
    }

    public ActorBuilder id(String id) {
        this.id = id;
        return this;
    }

    private Set<AssetAmount> getBalance() {
        if (balance == null) {
            balance = new HashSet<>(16);
        }
        return balance;
    }

    public ActorBuilder balance(Set<AssetAmount> balance) {
        this.balance = balance;
        return this;
    }

    public ActorBuilder addAsset(AssetAmount assetAmount) {
        getBalance().add(assetAmount);
        return this;
    }

    public ActorBuilder addAsset(Asset asset, long amount) {
        getBalance().add(objectFactory.createAssetAmount(asset, amount));
        return this;
    }

    public ActorBuilder uTransnetAccount(UserAccount uTransnetAccount) {
        this.uTransnetAccount = uTransnetAccount;
        return this;
    }

    public T build() {
        return newInstance();
    }

    protected T newInstance() {
        return getContext().getBean(clazz);
    }



    public String toString() {
        return "Actor.ActorBuilder(id=" + this.id + ", balance=" + this.balance + ", uTransnetAccount=" + this.uTransnetAccount + ", lastOperationId=" + this.lastOperationId + ", operationListeners=" + this.operationListeners + ")";
    }
}
