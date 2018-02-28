package com.github.utransnet.simulator.externalapi.h2impl;

import com.github.utransnet.simulator.SpringTest;
import com.github.utransnet.simulator.TestWithJpaConfig;
import com.github.utransnet.simulator.externalapi.Asset;
import com.github.utransnet.simulator.externalapi.AssetAmount;
import com.github.utransnet.simulator.externalapi.ExternalAPI;
import com.github.utransnet.simulator.externalapi.UserAccount;
import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import com.github.utransnet.simulator.externalapi.operations.MessageOperation;
import com.github.utransnet.simulator.externalapi.operations.OperationType;
import com.github.utransnet.simulator.externalapi.operations.TransferOperation;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * Created by Artem on 16.02.2018.
 */
public class ExternalAPIH2Test extends SpringTest<ExternalAPIH2Test.Config> {

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    TransferOperationH2Repository transferOperationRepository;

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    APIObjectFactoryH2 apiObjectFactory;

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    MessageOperationRepository messageOperationRepository;

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    ProposalH2Repository proposalRepository;

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    ExternalAPI externalAPI;

    private UserAccount from;
    private UserAccount to;
    private UserAccount approvingAcc;
    private Asset asset;
    private AssetAmount assetAmount;
    private long amount = 10;
    private String msg = "msg";


    private void prepareAccounts() {
        from = apiObjectFactory.userAccount("from-acc");
        to = apiObjectFactory.userAccount("to-acc");
        approvingAcc = apiObjectFactory.userAccount("approving-acc");
        asset = apiObjectFactory.getAsset("test-asset");
        assetAmount = apiObjectFactory.getAssetAmount(asset, amount);
    }

    @Test
    public void sendProposal() throws Exception {
        //TODO: how account should now about proposal?
    }

    @Test
    public void approveProposal() throws Exception {
        //TODO: how account should now about proposal?
    }

    @Test
    public void sendAsset() throws Exception {
        prepareAccounts();
        assertEquals(0, transferOperationRepository.count());
        externalAPI.sendAsset(from, to, assetAmount, msg);
        assertEquals(1, transferOperationRepository.count());

        TransferOperationH2 next = transferOperationRepository.findAll().iterator().next();
        next.setApiObjectFactory(apiObjectFactory);
        assertEquals(from, next.getFrom());
        assertEquals(to, next.getTo());
        assertEquals(assetAmount, next.getAssetAmount());
        assertEquals(msg, next.getMemo());
    }

    @Test
    public void sendMessage() throws Exception {
        prepareAccounts();
        assertEquals(0, messageOperationRepository.count());
        externalAPI.sendMessage(from, to, msg);
        assertEquals(1, messageOperationRepository.count());

        MessageOperationH2 next = messageOperationRepository.findAll().iterator().next();
        next.setApiObjectFactory(apiObjectFactory);
        assertEquals(from, next.getFrom());
        assertEquals(to, next.getTo());
        assertEquals(msg, next.getMessage());
    }

    @Test
    public void getAccountHistory() throws Exception {
        prepareAccounts();
        assertEquals(0, externalAPI.getAccountHistory(from).size());
        externalAPI.sendMessage(from, to, msg);
        assertEquals(1, externalAPI.getAccountHistory(from).size());
        externalAPI.sendMessage(to, from, msg + msg);
        assertEquals(2, externalAPI.getAccountHistory(from).size());

        externalAPI.sendAsset(from, to, assetAmount, msg);
        assertEquals(3, externalAPI.getAccountHistory(from).size());
        externalAPI.sendAsset(to, from, assetAmount, msg + msg);
        assertEquals(4, externalAPI.getAccountHistory(from).size());

        long messagesCount = externalAPI.getAccountHistory(from)
                .stream()
                .filter(o -> o.getOperationType() == OperationType.MESSAGE)
                .map(o -> (MessageOperation) o)
                .filter(messageOperation ->
                        Objects.equals(messageOperation.getFrom(), from) &&
                                Objects.equals(messageOperation.getTo(), to) &&
                                Objects.equals(messageOperation.getMessage(), msg)

                                ||
                                Objects.equals(messageOperation.getFrom(), to) &&
                                        Objects.equals(messageOperation.getTo(), from) &&
                                        Objects.equals(messageOperation.getMessage(), msg + msg)
                ).count();
        assertEquals(2, messagesCount);

        long transfersCount = externalAPI.getAccountHistory(from)
                .stream()
                .filter(o -> o.getOperationType() == OperationType.TRANSFER)
                .map(o -> (TransferOperation) o)
                .filter(messageOperation ->
                        Objects.equals(messageOperation.getFrom(), from) &&
                                Objects.equals(messageOperation.getTo(), to) &&
                                Objects.equals(messageOperation.getMemo(), msg)

                                ||
                                Objects.equals(messageOperation.getFrom(), to) &&
                                        Objects.equals(messageOperation.getTo(), from) &&
                                        Objects.equals(messageOperation.getMemo(), msg + msg)
                ).count();
        assertEquals(2, transfersCount);


        externalAPI.sendAsset(approvingAcc, to, assetAmount, msg);
        // history shouldn't be changed
        assertEquals(4, externalAPI.getAccountHistory(from).size());
    }

    @Test
    public void getAccountMessages() throws Exception {
        prepareAccounts();
        externalAPI.sendMessage(from, to, msg);
        externalAPI.sendMessage(to, from, msg + msg);
        assertEquals(2, externalAPI.getAccountHistory(from).size());
        List<MessageOperation> accountMessages = externalAPI.getAccountMessages(from);


        assertEquals(from, accountMessages.get(0).getFrom());
        assertEquals(to, accountMessages.get(0).getTo());
        assertEquals(msg, accountMessages.get(0).getMessage());
        assertEquals(from, accountMessages.get(1).getTo());
        assertEquals(to, accountMessages.get(1).getFrom());
        assertEquals(msg + msg, accountMessages.get(1).getMessage());
    }

    @Test
    public void createAccount() throws Exception {
        assertEquals("test", externalAPI.createAccount("test").getId());
    }

    @Test
    public void getAccountByName() throws Exception {
        assertEquals("test", externalAPI.getAccountByName("test").getName());
    }

    @Test
    public void getLastOperation() throws Exception {
        prepareAccounts();
        assertFalse(externalAPI.getLastOperation(from).isPresent());
        externalAPI.sendMessage(from, to, msg);
        Optional<? extends BaseOperation> lastOperation1 = externalAPI.getLastOperation(from);
        assertTrue(lastOperation1.isPresent());

        assertEquals(lastOperation1.get().getId(), externalAPI.getLastOperation(from).get().getId());

        externalAPI.sendMessage(from, to, msg);
        assertNotEquals(lastOperation1.get().getId(), externalAPI.getLastOperation(from).get().getId());


        lastOperation1 = externalAPI.getLastOperation(from);
        //noinspection ConstantConditions
        assertEquals(lastOperation1.get().getId(), externalAPI.getLastOperation(from).get().getId());
        externalAPI.sendMessage(approvingAcc, to, msg);
        //noinspection ConstantConditions
        assertEquals(lastOperation1.get().getId(), externalAPI.getLastOperation(from).get().getId());
    }

    @Test
    public void operationsAfter() throws Exception {
        prepareAccounts();
        externalAPI.sendMessage(from, to, msg);
        @SuppressWarnings("ConstantConditions") BaseOperation baseOperation1 = externalAPI.getLastOperation(from).get();
        assertEquals(0, externalAPI.operationsAfter(from, baseOperation1).size());

        externalAPI.sendMessage(from, to, msg);
        @SuppressWarnings("ConstantConditions") BaseOperation baseOperation2 = externalAPI.getLastOperation(from).get();
        assertEquals(1, externalAPI.operationsAfter(from, baseOperation1).size());
        assertEquals(baseOperation2.getId(), externalAPI.operationsAfter(from, baseOperation1).get(0).getId());


        externalAPI.sendMessage(from, to, msg);
        @SuppressWarnings("ConstantConditions") BaseOperation baseOperation3 = externalAPI.getLastOperation(from).get();
        List<? extends BaseOperation> after = externalAPI.operationsAfter(from, baseOperation1);
        assertEquals(2, after.size());
        assertEquals(baseOperation2.getId(), externalAPI.operationsAfter(from, baseOperation1).get(0).getId());
        assertEquals(baseOperation3.getId(), externalAPI.operationsAfter(from, baseOperation1).get(1).getId());

        assertThat(externalAPI.operationsAfter(from, baseOperation1), is(after));

        assertEquals(3, externalAPI.operationsAfter(from, (BaseOperation) null).size());
        assertEquals(3, externalAPI.operationsAfter(from, "").size());
    }

    @Test
    public void listenAccountUpdatesByUserId() throws Exception {
        prepareAccounts();
        final int[] updatesCount = {0};
        externalAPI.listenAccountUpdates(
                "from",
                Stream.of(from).collect(Collectors.toSet()),
                accountUpdateObject -> {
                    updatesCount[0]++;
                    assertEquals(from, accountUpdateObject.getUpdatedAccount());
                }
        );
        externalAPI.listenAccountUpdates(
                "to",
                Stream.of(to).collect(Collectors.toSet()),
                accountUpdateObject -> {
                    updatesCount[0]++;
                    assertEquals(to, accountUpdateObject.getUpdatedAccount());
                }
        );
        externalAPI.sendMessage(from, to, msg);
        assertEquals(2, updatesCount[0]);
    }

    @Test
    public void listenAccountOperationsByUserId() throws Exception {
        prepareAccounts();
        final int[] updatesCount = {0};
        externalAPI.listenAccountOperations(
                "from",
                Stream.of(from).collect(Collectors.toSet()),
                externalObject -> {
                    updatesCount[0]++;
                    assertTrue(externalObject instanceof MessageOperation);
                }
        );
        externalAPI.listenAccountOperations(
                "to",
                Stream.of(to).collect(Collectors.toSet()),
                externalObject -> {
                    updatesCount[0]++;
                    assertTrue(externalObject instanceof MessageOperation);
                }
        );
        externalAPI.sendMessage(from, to, msg);
        assertEquals(2, updatesCount[0]);
    }

    @Test
    public void removeAccountUpdateListener() throws Exception {
        prepareAccounts();
        externalAPI.listenAccountUpdates(
                "from",
                Stream.of(from).collect(Collectors.toSet()),
                accountUpdateObject -> fail("Listener should be deleted")
        );
        externalAPI.removeAccountUpdateListener("from");
        externalAPI.sendMessage(from, to, msg);
    }

    @Test
    public void removeAccountOperationListener() throws Exception {
        prepareAccounts();
        externalAPI.listenAccountOperations(
                "from",
                Stream.of(from).collect(Collectors.toSet()),
                accountUpdateObject -> fail("Listener should be deleted")
        );
        externalAPI.removeAccountOperationListener("from");
        externalAPI.sendMessage(from, to, msg);
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

}