package com.github.utransnet.simulator.actors.factory;

import com.github.utransnet.simulator.externalapi.APIObjectFactory;
import com.github.utransnet.simulator.externalapi.UserAccount;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

/**
 * Created by Artem on 02.02.2018.
 */
public class ActorBuilder<T extends Actor> {

    final protected Class<T> clazz;
    private String id;
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

    public ActorBuilder<T> uTransnetAccount(UserAccount uTransnetAccount) {
        this.uTransnetAccount = uTransnetAccount;
        return this;
    }

    public T build() {
        Assert.notNull(id, "Id of actor must be set");
        T t = newInstance();
        if (uTransnetAccount == null) {
            uTransnetAccount = objectFactory.userAccount(id);
        }
        t.setUTransnetAccount(uTransnetAccount);
        return t;
    }

    protected T newInstance() {
        return getContext().getBean(clazz);
    }



    public String toString() {
        return "Actor.ActorBuilder(id=" + this.id + ", uTransnetAccount=" + this.uTransnetAccount + ")";
    }
}
