package com.github.utransnet.simulator.actors.factory;

import com.github.utransnet.simulator.SpringTest;
import com.github.utransnet.simulator.TestWithJpaConfig;
import com.github.utransnet.simulator.actors.task.DelayedAction;
import com.github.utransnet.simulator.actors.task.EventListener;
import com.github.utransnet.simulator.actors.task.OperationEvent;
import com.github.utransnet.simulator.actors.task.OperationListener;
import com.github.utransnet.simulator.externalapi.APIObjectFactory;
import com.github.utransnet.simulator.externalapi.ExternalAPI;
import com.github.utransnet.simulator.externalapi.h2impl.ExternalAPIH2ImplConfig;
import com.github.utransnet.simulator.externalapi.operations.MessageOperation;
import com.github.utransnet.simulator.externalapi.operations.OperationType;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.junit.Assert.*;

/**
 * Created by Artem on 22.02.2018.
 */
public class ActorListenersTest extends SpringTest<ActorListenersTest.Config> {

    private final String testMsg = "test msg";
    @Autowired
    ExternalAPI externalAPI;
    @Autowired
    APIObjectFactory apiObjectFactory;

    @Test
    public void addOperationListener() throws Exception {
        Actor actor = new Actor(externalAPI);
        actor.setUTransnetAccount(externalAPI.createAccount("test-acc"));


        // test message receiving
        final boolean[] operationFired = {false};
        actor.addOperationListener(new OperationListener(
                "test-receive",
                OperationType.MESSAGE,
                operation -> {
                    assertEquals(OperationType.MESSAGE, operation.getOperationType());
                    assertTrue(OperationType.MESSAGE.clazz.isAssignableFrom(operation.getClass()));
                    assertEquals(testMsg, ((MessageOperation)operation).getMessage());
                    operationFired[0] = true;
                }
                ));
        assertFalse(operationFired[0]);
        actor.update(1);
        assertFalse(operationFired[0]);
        externalAPI.sendMessage(
                externalAPI.createAccount("sender"),
                actor.getUTransnetAccount(),
                testMsg
        );
        assertFalse(operationFired[0]);
        actor.update(1);
        assertTrue(operationFired[0]);


        // test message sending
        operationFired[0] = false;
        externalAPI.sendMessage(
                actor.getUTransnetAccount(),
                externalAPI.createAccount("sender"),
                testMsg
        );
        actor.update(1);
        assertTrue(operationFired[0]);

        // test ignoring other type
        operationFired[0] = false;
        externalAPI.sendAsset(
                actor.getUTransnetAccount(),
                externalAPI.createAccount("receiver"),
                apiObjectFactory.getAssetAmount("t", 2),
                testMsg
        );
        actor.update(1);
        assertFalse(operationFired[0]);

        // test ignoring with other participants of operation
        operationFired[0] = false;
        externalAPI.sendMessage(
                externalAPI.createAccount("sender"),
                externalAPI.createAccount("receiver"),
                testMsg
        );
        actor.update(1);
        assertFalse(operationFired[0]);
    }

    @Test
    public void removeOperationListener() throws Exception {
        Actor actor = new Actor(externalAPI);
        actor.setUTransnetAccount(externalAPI.createAccount("test-acc"));
        actor.addOperationListener(new OperationListener(
                "test-receive",
                OperationType.MESSAGE,
                operation -> fail()
        ));
        actor.removeOperationListener("test-receive");
        externalAPI.sendMessage(
                externalAPI.createAccount("sender"),
                actor.getUTransnetAccount(),
                testMsg
        );
        actor.update(1);
    }

    @Test
    public void addEventListener() throws Exception {
        Actor actor = new Actor(externalAPI);
        actor.setUTransnetAccount(externalAPI.createAccount("test-acc"));
        final boolean[] eventFired = {false};
        actor.addEventListener(new EventListener(
                "test",
                OperationEvent.Type.MESSAGE,
                operationEvent -> {
                    if(operationEvent.getEventType() == OperationEvent.Type.MESSAGE){
                        MessageOperation message = ((OperationEvent.MessageEvent)operationEvent).getObject();
                        assertEquals(testMsg, message.getMessage());
                        eventFired[0] = true;
                    } else {
                        fail();
                    }
                }
        ));
        externalAPI.sendMessage(
                externalAPI.createAccount("sender"),
                actor.getUTransnetAccount(),
                testMsg
        );
        actor.update(1);
        assertTrue(eventFired[0]);

        eventFired[0] = false;
        externalAPI.sendAsset(
                actor.getUTransnetAccount(),
                externalAPI.createAccount("receiver"),
                apiObjectFactory.getAssetAmount("t", 2),
                testMsg
        );
        actor.update(1);
        assertFalse(eventFired[0]);
    }

    @Test
    public void removeEventListener() throws Exception {
        Actor actor = new Actor(externalAPI);
        actor.setUTransnetAccount(externalAPI.createAccount("test-acc"));
        actor.addEventListener(new EventListener(
                "test",
                OperationEvent.Type.MESSAGE,
                operationEvent -> fail()
        ));
        actor.removeEventListener("test");
        externalAPI.sendMessage(
                externalAPI.createAccount("sender"),
                actor.getUTransnetAccount(),
                testMsg
        );
        actor.update(1);
    }

    @Test
    public void addDelayedAction() throws Exception {
        Actor actor = new Actor(externalAPI);
        actor.setUTransnetAccount(externalAPI.createAccount("test-acc"));
        final int[] called = {0};
        actor.addDelayedAction(new DelayedAction(actor, "test", 10, () -> called[0]++));
        assertEquals(0, called[0]);
        actor.update(5);
        assertEquals(0, called[0]); // to early for action
        actor.update(5);
        assertEquals(1, called[0]); // exact time
        actor.update(20);
        assertEquals(1, called[0]); // action called omce
    }

    @Test
    public void removeDelayedAction() throws Exception {
        Actor actor = new Actor(externalAPI);
        actor.setUTransnetAccount(externalAPI.createAccount("test-acc"));
        actor.addDelayedAction(new DelayedAction(actor, "test", 10, Assert::fail));
        actor.removeDelayedAction("test");
        actor.update(20);
    }

    @Test
    public void checkNewOperations() throws Exception {
        Actor4Test actor = new Actor4Test(externalAPI);
        actor.setUTransnetAccount(externalAPI.createAccount("test-acc"));
        externalAPI.sendMessage(
                externalAPI.createAccount("sender"),
                actor.getUTransnetAccount(),
                testMsg
        );
        assertTrue(actor.checkNewOperations());
        assertFalse(actor.checkNewOperations());
        externalAPI.sendMessage(
                externalAPI.createAccount("sender"),
                actor.getUTransnetAccount(),
                testMsg
        );
        assertTrue(actor.checkNewOperations());
        assertFalse(actor.checkNewOperations());


        externalAPI.sendMessage(
                externalAPI.createAccount("sender"),
                externalAPI.createAccount("receiver"),
                testMsg
        );
        assertFalse(actor.checkNewOperations());
    }

    @Test
    public void checkNewOperationsBeforeFirstOperation() throws Exception {
        Actor4Test actor = new Actor4Test(externalAPI);
        actor.setUTransnetAccount(externalAPI.createAccount("test-acc"));
        assertFalse(actor.checkNewOperations());
    }

    @Test
    public void checkNewOperationsAfterFirstOperation() throws Exception {
        Actor4Test actor = new Actor4Test(externalAPI);
        actor.setUTransnetAccount(externalAPI.createAccount("test-acc"));
        externalAPI.sendMessage(
                externalAPI.createAccount("sender"),
                actor.getUTransnetAccount(),
                testMsg
        );
        assertTrue(actor.checkNewOperations());
    }

    @Override
    public Class<Config> getRootConfig() {
        return Config.class;
    }

    @Configuration
    @Import({
            TestWithJpaConfig.class,
            ExternalAPIH2ImplConfig.class
    })
    public static class Config {

    }

    public static class Actor4Test extends Actor {

        public Actor4Test(ExternalAPI externalAPI) {
            super(externalAPI);
        }

        public boolean checkNewOperations() {
            return super.checkNewOperations();
        }
    }

}