package com.github.utransnet.simulator.actors.factory;

import com.github.utransnet.simulator.actors.task.OperationListener;
import com.github.utransnet.simulator.externalapi.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Artem on 02.02.2018.
 */
public class ActorBuilder<T extends Actor> {

    final protected Class<T> clazz;
    private String id;
    private Set<AssetAmount> balance;
    private UserAccount uTransnetAccount;

    @Getter(AccessLevel.PROTECTED)
    private final ApplicationContext context;
    @Getter(AccessLevel.PROTECTED)
    private final APIObjectFactory objectFactory;

    ActorBuilder(Class<T> clazz, ApplicationContext context, APIObjectFactory objectFactory) {
        this.clazz = clazz;
        this.context = context;
        this.objectFactory = objectFactory;
    }

    public ActorBuilder<T> id(String id) {
        this.id = id;
        return this;
    }

    private Set<AssetAmount> getBalance() {
        if (balance == null) {
            balance = new HashSet<>(16);
        }
        return balance;
    }

    public ActorBuilder<T> balance(Set<AssetAmount> balance) {
        this.balance = balance;
        return this;
    }

    public ActorBuilder<T> addAsset(AssetAmount assetAmount) {
        getBalance().add(assetAmount);
        return this;
    }

    public ActorBuilder<T> uTransnetAccount(UserAccount uTransnetAccount) {
        this.uTransnetAccount = uTransnetAccount;
        return this;
    }

    public T build() {
        Assert.notNull(id, "Id of actor must be set");
        T t = newInstance();
        if (uTransnetAccount == null) {
            uTransnetAccount = objectFactory.createOrGetUserAccount(id);
        }
        t.setUTransnetAccount(uTransnetAccount);
        t.setBalance(balance);
        return t;
    }

    protected T newInstance() {
        return getContext().getBean(clazz);
    }



    public String toString() {
        return "Actor.ActorBuilder(id=" + this.id + ", balance=" + this.balance + ", uTransnetAccount=" + this.uTransnetAccount + ")";
    }
}
