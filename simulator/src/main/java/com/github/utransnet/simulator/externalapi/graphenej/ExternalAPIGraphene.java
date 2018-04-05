package com.github.utransnet.simulator.externalapi.graphenej;

import com.github.utransnet.graphenej.Transaction;
import com.github.utransnet.graphenej.api.GetAccountByName;
import com.github.utransnet.graphenej.api.GetAccountHistory;
import com.github.utransnet.graphenej.api.GetProposedTransactions;
import com.github.utransnet.graphenej.api.TransactionBroadcastSequence;
import com.github.utransnet.graphenej.interfaces.WitnessResponseListener;
import com.github.utransnet.graphenej.models.AccountProperties;
import com.github.utransnet.graphenej.models.HistoricalOperation;
import com.github.utransnet.graphenej.operations.*;
import com.github.utransnet.graphenej.test.NaiveSSLContext;
import com.github.utransnet.simulator.externalapi.*;
import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import com.github.utransnet.simulator.externalapi.operations.OperationType;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketListener;
import lombok.SneakyThrows;
import org.bitcoinj.core.ECKey;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by Artem on 01.04.2018.
 */
public class ExternalAPIGraphene extends ExternalAPI {

    private final APIObjectFactory apiObjectFactory;
    private final DefaultAssets defaultAssets;
    private final OperationConverter operationConverter;
    private WebSocketFactory factory;
    private ExecutorService threadPool;
    private com.github.utransnet.graphenej.Asset feeAsset;
    private AssetAmount msgAsset;

    ExternalAPIGraphene(APIObjectFactory apiObjectFactory, DefaultAssets defaultAssets, OperationConverter operationConverter) {
        this.apiObjectFactory = apiObjectFactory;
        this.defaultAssets = defaultAssets;
        feeAsset = new com.github.utransnet.graphenej.Asset(defaultAssets.getFeeAsset().getId());
        msgAsset = new AssetAmountGraphene(castToGraphene(defaultAssets.getMessageAsset()), 1);
        this.operationConverter = operationConverter;
    }


    private void sendTransaction(
            com.github.utransnet.graphenej.BaseOperation baseOperation,
            ECKey feePayerKey
    ) {
        // Adding operations to the operation list
        ArrayList<com.github.utransnet.graphenej.BaseOperation> operationList = new ArrayList<>();
        operationList.add(baseOperation);

        final ResponseObject<String> responseObject = new ResponseObject<>();

        WitnessResponseListener responseListener = new BlockingResponseListener<>(responseObject);

        Transaction transaction = new Transaction(feePayerKey, null, operationList);
        TransactionBroadcastSequence listener = new TransactionBroadcastSequence(transaction, feeAsset, responseListener);
        broadcastTransaction(listener, responseObject);
    }

    @SneakyThrows
    private void broadcastTransaction(WebSocketListener listener, final Object lock) {
        //TODO
        WebSocket mWebSocket = factory.createSocket("wss://eu.openledger.info/ws");


        mWebSocket.addListener(listener);

        mWebSocket.connect();
        synchronized (lock) {
            lock.wait();
        }
        mWebSocket.sendClose();
    }

    @SuppressWarnings("unchecked")
    private <T extends GrapheneWrapper> T castToGraphene(Object obj) {
        Assert.isInstanceOf(
                GrapheneWrapper.class,
                obj,
                "Object '" + obj.getClass().getCanonicalName() +
                        "' should implement '" + GrapheneWrapper.class.getName() + "' interface"
        );
        return (T) obj;
    }

    @PostConstruct
    @SneakyThrows(NoSuchAlgorithmException.class)
    private void init() {
        // Setting up a secure websocket connection.
        SSLContext context = NaiveSSLContext.getInstance("TLS");
        factory = new WebSocketFactory();
        factory.setSSLContext(context);

        threadPool = Executors.newCachedThreadPool();
    }

    @Override
    public void sendProposal(UserAccount from, UserAccount to, UserAccount feePayer, AssetAmount assetAmount, String memo) {
        com.github.utransnet.graphenej.UserAccount fromRaw = this.<UserAccountGraphene>castToGraphene(from).getRaw();
        com.github.utransnet.graphenej.UserAccount toRaw = this.<UserAccountGraphene>castToGraphene(to).getRaw();
        com.github.utransnet.graphenej.AssetAmount assetRaw =
                this.<AssetAmountGraphene>castToGraphene(assetAmount).getRaw();

        TransferOperation transferOperation = new TransferOperationBuilder()
                .setTransferAmount(assetRaw)
                .setSource(fromRaw)
                .setDestination(toRaw)
                .build();

        UserAccountGraphene feePayerGrapheneAccount = castToGraphene(feePayer);
        ProposalCreateOperation proposalCreateOperation = new ProposalCreateOperationBuilder()
                .setFeePayingAccount(feePayerGrapheneAccount.getRaw())
                .addProposedOp(transferOperation)
//                .setReviewPeriodSeconds(10000)
                .setExpirationTime(15000)
                .build();

        sendTransaction(proposalCreateOperation, feePayerGrapheneAccount.getKey());
    }

    @Override
    public void approveProposal(UserAccount approvingAccount, Proposal proposal) {
        UserAccountGraphene grapheneAccount = castToGraphene(approvingAccount);
        ProposalUpdateOperation proposalUpdateOperation = new ProposalUpdateOperationBuilder()
                .setFeePayingAccount(grapheneAccount.getRaw())
                .setProposalId(proposal.getId())
                .addActiveApprovalsToAdd(grapheneAccount.getRaw())
                .build();

        sendTransaction(proposalUpdateOperation, grapheneAccount.getKey());
    }

    @Override
    public void sendAsset(UserAccount from, UserAccount to, AssetAmount assetAmount, String memo) {
        threadPool.execute(() -> {
            UserAccountGraphene fromGrapheneAccount = castToGraphene(from);
            com.github.utransnet.graphenej.UserAccount fromRaw = fromGrapheneAccount.getRaw();
            com.github.utransnet.graphenej.UserAccount toRaw = ((UserAccountGraphene) castToGraphene(to)).getRaw();
            com.github.utransnet.graphenej.AssetAmount assetRaw =
                    ((AssetAmountGraphene) castToGraphene(assetAmount)).getRaw();

            TransferOperation transferOperation = new TransferOperationBuilder()
                    .setTransferAmount(assetRaw)
                    .setSource(fromRaw)
                    .setDestination(toRaw)
                    .build();

            sendTransaction(transferOperation, fromGrapheneAccount.getKey());
        });
    }

    @Override
    public void sendMessage(UserAccount from, UserAccount to, String message) {
        sendAsset(from, to, msgAsset, message);
    }

    @Override
    public List<? extends BaseOperation> getAccountHistory(UserAccount account, OperationType operationType) {
        //TODO add filter in graphenej
        return getAccountHistory(account)
                .stream()
                .filter(o -> o.getOperationType() == operationType)
                .collect(Collectors.toList());
    }

    @Override
    public List<? extends BaseOperation> getAccountHistory(UserAccount account) {
        return getAccountHistory(
                account,
                GetAccountHistory.NEWEST_TRANSACTION_ID,
                GetAccountHistory.OLDEST_TRANSACTION_ID,
                10
        );
    }

    @Override
    public List<Proposal> getAccountProposals(UserAccount account) {
        ResponseObject<List<com.github.utransnet.graphenej.objects.Proposal>> responseObject = new ResponseObject<>();
        GetProposedTransactions listener = new GetProposedTransactions(
                new com.github.utransnet.graphenej.UserAccount(account.getId()),
                new BlockingResponseListener<>(responseObject)
        );
        broadcastTransaction(listener, responseObject);

        return responseObject.getResult()
                .stream()
                .map(proposal -> new ProposalGraphene(proposal, operationConverter))
                .collect(Collectors.toList());
    }

    @Override
    public UserAccount createAccount(String name) {
        //TODO: implement
        return null;
    }

    @Override
    public UserAccount getAccountByName(String name) {
        ResponseObject<AccountProperties> responseObject = new ResponseObject<>();
        GetAccountByName getAccountByName = new GetAccountByName(
                name,
                new BlockingResponseListener<>(responseObject)
        );

        broadcastTransaction(getAccountByName, responseObject);

        UserAccountGraphene userAccount = castToGraphene(apiObjectFactory.userAccount(responseObject.getResult().id));
        userAccount.setName(responseObject.getResult().name);

        return userAccount;
    }

    @Override
    public UserAccount getAccountById(String id) {
        return apiObjectFactory.userAccount(id);
    }

    @Override
    public Optional<? extends BaseOperation> getLastOperation(UserAccount account) {
        List<? extends BaseOperation> accountHistory = getAccountHistory(
                account,
                GetAccountHistory.NEWEST_TRANSACTION_ID,
                GetAccountHistory.OLDEST_TRANSACTION_ID,
                1
        );
        if (accountHistory.size() > 0) {
            return Optional.of(accountHistory.get(0));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public List<? extends BaseOperation> operationsAfter(UserAccount account, String operationId) {
        return getAccountHistory(
                account,
                GetAccountHistory.NEWEST_TRANSACTION_ID,
                Long.parseLong(operationId),
                999999
        );
    }


    private List<? extends BaseOperation> getAccountHistory(
            UserAccount account,
            long newestOperationIs,
            long oldestOperationId,
            int limit
    ) {
        ResponseObject<List<HistoricalOperation>> responseObject = new ResponseObject<>();
        GetAccountHistory getAccountHistory = new GetAccountHistory(
                new com.github.utransnet.graphenej.UserAccount(account.getId()),
                newestOperationIs,
                oldestOperationId,
                limit,
                new BlockingResponseListener<>(responseObject)
        );

        broadcastTransaction(getAccountHistory, responseObject);

        ArrayList<BaseOperationGraphene> history = new ArrayList<>(10);
        responseObject.getResult().forEach(historicalOperation -> {
            BaseOperationGraphene baseOperationGraphene = operationConverter.fromGrapheneOp(
                    historicalOperation.getOperation()
            );
            if (baseOperationGraphene.getOperationType() == OperationType.PROPOSAL_CREATE) {
                if (historicalOperation.result.length == 2) {
                    String proposalId = (String) historicalOperation.result[1];
                    ((ProposalCreateOperationGraphene) baseOperationGraphene).getProposal().setId(proposalId);
                }
            }
            history.add(baseOperationGraphene);
        });

        return history;
    }


    @Override
    public void listenAccountUpdatesByUserId(String listenerId, Set<String> accsToListen, Consumer<AccountUpdateObject> onUpdate) {

    }

    @Override
    public void removeAccountUpdateListener(String listenerId) {

    }

    @Override
    public void listenAccountOperationsByUserId(String listenerId, Set<String> accsToListen, Consumer<ExternalObject> onUpdate) {

    }

    @Override
    public void removeAccountOperationListener(String listenerId) {

    }

}
