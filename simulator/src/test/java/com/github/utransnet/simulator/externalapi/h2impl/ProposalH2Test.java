package com.github.utransnet.simulator.externalapi.h2impl;

import com.github.utransnet.simulator.SpringTest;
import com.github.utransnet.simulator.TestWithJpaConfig;
import com.github.utransnet.simulator.externalapi.UserAccount;
import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import com.github.utransnet.simulator.externalapi.operations.MessageOperation;
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
public class ProposalH2Test extends SpringTest<ProposalH2Test.Config> {

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    ProposalH2Repository proposalH2Repository;

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    APIObjectFactoryH2 objectFactory;

    @Test
    public void testSaveRestore() throws Exception {
        assertEquals(0, proposalH2Repository.count());

        UserAccount account = objectFactory.userAccount("test");
        MessageOperation operation = new MessageOperationH2(objectFactory, account, account, "");
        ProposalH2 proposal = new ProposalH2(objectFactory, account, account, operation);
        proposalH2Repository.save(proposal);

        ProposalH2 saved = proposalH2Repository.findAll().iterator().next();
        saved.setApiObjectFactory(objectFactory);
        assertEquals(proposal.getFeePayer(), saved.getFeePayer());

        BaseOperation savedOperation = saved.getOperation();
        assertEquals(operation.getOperationType(), savedOperation.getOperationType());
        MessageOperationH2 messageOperation = (MessageOperationH2) savedOperation;
        assertEquals(operation.getFrom(), messageOperation.getFrom());
        assertEquals(operation.getTo(), messageOperation.getTo());
        assertEquals(operation.getMessage(), messageOperation.getMessage());

        proposalH2Repository.save(saved);
        assertEquals(1, proposalH2Repository.count());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullProposedOperation() throws Exception {
        UserAccount account = objectFactory.userAccount("test");
        //noinspection ConstantConditions
        new ProposalH2(objectFactory, account, account, null);
    }

    @Test
    public void testApproving() throws Exception {
        UserAccount account = objectFactory.userAccount("creator");
        UserAccount approver = objectFactory.userAccount("approver");
        MessageOperationH2 operation = new MessageOperationH2(objectFactory, account, account, "");
        ProposalH2 proposal = new ProposalH2(objectFactory, approver, account, operation);
        proposalH2Repository.save(proposal);

        ProposalH2 saved1 = proposalH2Repository.findAll().iterator().next();

        assertEquals(1, saved1.neededApproves().size());
        assertEquals(approver.getId(), saved1.neededApproves().get(0));
        assertFalse(saved1.approved());
        proposalH2Repository.save(saved1);

        ProposalH2 saved2 = proposalH2Repository.findAll().iterator().next();
        saved2.addApprove(approver);
        assertEquals(0, saved2.neededApproves().size());
        assertTrue(saved2.approved());

        assertEquals(1, proposalH2Repository.count());
    }

    @Override
    public Class<Config> getRootConfig() {
        return Config.class;
    }


    @Configuration
    @Import({
            TestWithJpaConfig.class
    })
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