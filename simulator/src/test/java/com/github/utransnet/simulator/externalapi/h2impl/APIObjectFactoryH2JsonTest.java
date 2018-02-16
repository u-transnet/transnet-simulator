package com.github.utransnet.simulator.externalapi.h2impl;

import com.github.utransnet.simulator.SpringTest;
import com.github.utransnet.simulator.TestWithJpaConfig;
import com.github.utransnet.simulator.externalapi.Asset;
import com.github.utransnet.simulator.externalapi.UserAccount;
import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import com.github.utransnet.simulator.externalapi.operations.MessageOperation;
import com.github.utransnet.simulator.externalapi.operations.OperationType;
import com.github.utransnet.simulator.externalapi.operations.TransferOperation;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.junit.Assert.*;

/**
 * Created by Artem on 15.02.2018.
 */
public class APIObjectFactoryH2JsonTest extends SpringTest<APIObjectFactoryH2JsonTest.Config> {

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    APIObjectFactoryH2 objectFactory;

    @Test
    public void operationFromJson() throws Exception {
        String json = "{" +
                "\"class\":\"com.github.utransnet.simulator.externalapi.h2impl.MessageOperationH2\"," +
                "\"fromStr\":\"sender\"," +
                "\"toStr\":\"receiver\"," +
                "\"message\":\"msg\"," +
                "\"id\":\"0\"," +
                "\"creationDate\":null" +
                "}";
        BaseOperation operation = objectFactory.operationFromJson(json);
        assertEquals(OperationType.MESSAGE, operation.getOperationType());
        assertEquals(MessageOperationH2.class, operation.getClass());
        MessageOperation messageOperation = (MessageOperation) operation;
        assertEquals("sender", messageOperation.getFrom().getId());
        assertEquals("receiver", messageOperation.getTo().getId());
        assertEquals("msg", messageOperation.getMessage());
    }

    @Test
    public void operationToJson() throws Exception {
        String sample = "{" +
                "\"class\":\"com.github.utransnet.simulator.externalapi.h2impl.MessageOperationH2\"," +
                "\"fromStr\":\"sender\"," +
                "\"toStr\":\"receiver\"," +
                "\"message\":\"msg\"," +
                "\"id\":\"null\"," +
                "\"creationDate\":null" +
                "}";
        MessageOperation messageOperation = new MessageOperationH2(
                objectFactory,
                objectFactory.userAccount("sender"),
                objectFactory.userAccount("receiver"),
                "msg"
        );

        String json = objectFactory.operationToJson(messageOperation);
        assertEquals(sample, json);
    }

    @Test
    public void testMessageOperationSerialization() throws Exception {
        MessageOperation before = new MessageOperationH2(
                objectFactory,
                objectFactory.userAccount("sender"),
                objectFactory.userAccount("receiver"),
                "msg"
        );

        String json = objectFactory.operationToJson(before);

        BaseOperation operation = objectFactory.operationFromJson(json);
        assertEquals(OperationType.MESSAGE, operation.getOperationType());
        assertEquals(MessageOperationH2.class, operation.getClass());

        MessageOperation after = (MessageOperation) operation;
        assertEquals(before.getFrom(), after.getFrom());
        assertEquals(before.getTo(), after.getTo());
        assertEquals(before.getMessage(), after.getMessage());
    }

    @Test
    public void testTransferOperationSerialization() throws Exception {
        TransferOperation before = new TransferOperationH2(
                objectFactory,
                objectFactory.userAccount("sender"),
                objectFactory.userAccount("receiver"),
                objectFactory.getAsset("test-asset"),
                10,
                "test-memo"
        );

        String json = objectFactory.operationToJson(before);

        BaseOperation operation = objectFactory.operationFromJson(json);
        assertEquals(OperationType.TRANSFER, operation.getOperationType());
        assertEquals(TransferOperationH2.class, operation.getClass());

        TransferOperation after = (TransferOperation) operation;
        assertEquals(before.getFrom(), after.getFrom());
        assertEquals(before.getTo(), after.getTo());
        assertEquals(before.getAsset().getId(), after.getAsset().getId());
        assertEquals(before.getAmount(), after.getAmount());
        assertEquals(before.getMemo(), after.getMemo());
    }

    @Override
    public Class<Config> getRootConfig() {
        return Config.class;
    }

    @Configuration
    static class Config {
        @Bean
        @Autowired
        APIObjectFactoryH2 apiObjectFactoryH2(ApplicationContext context) {
            return new APIObjectFactoryH2(context) {
                @Override
                public UserAccount userAccount(String name) {
                    return new UserAccount(null) {
                        @Override
                        public String getName() {
                            return name;
                        }

                        @Override
                        public String getId() {
                            return name;
                        }
                    };
                }
            };
        }
    }

}