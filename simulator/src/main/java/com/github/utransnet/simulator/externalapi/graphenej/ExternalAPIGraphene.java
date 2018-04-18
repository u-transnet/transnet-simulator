package com.github.utransnet.simulator.externalapi.graphenej;

import com.github.utransnet.graphenej.Address;
import com.github.utransnet.graphenej.GrapheneObject;
import com.github.utransnet.graphenej.Transaction;
import com.github.utransnet.graphenej.Util;
import com.github.utransnet.graphenej.api.*;
import com.github.utransnet.graphenej.interfaces.WitnessResponseListener;
import com.github.utransnet.graphenej.models.AccountProperties;
import com.github.utransnet.graphenej.models.AccountTransactionHistoryObject;
import com.github.utransnet.graphenej.models.HistoricalOperation;
import com.github.utransnet.graphenej.models.SubscriptionResponse;
import com.github.utransnet.graphenej.objects.Memo;
import com.github.utransnet.graphenej.operations.*;
import com.github.utransnet.graphenej.test.NaiveSSLContext;
import com.github.utransnet.simulator.externalapi.*;
import com.github.utransnet.simulator.externalapi.exceptions.NotFoundException;
import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import com.github.utransnet.simulator.externalapi.operations.OperationType;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketListener;
import lombok.SneakyThrows;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.bitcoinj.core.ECKey;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toList;

/**
 * Created by Artem on 01.04.2018.
 */
public class ExternalAPIGraphene extends ExternalAPI {

    private final DefaultAssets defaultAssets;
    private final OperationConverter operationConverter;
    private final MessageHub messageHub;
    private final ApplicationContext context;
    private final APIObjectFactory apiObjectFactory;
    private WebSocketFactory factory;
    private ExecutorService threadPool;
    private com.github.utransnet.graphenej.Asset feeAsset;
    private AssetAmount msgAsset;

    ExternalAPIGraphene(
            ApplicationContext context,
            APIObjectFactory apiObjectFactory,
            DefaultAssets defaultAssets,
            OperationConverter operationConverter,
            MessageHub messageHub
    ) {
        this.defaultAssets = defaultAssets;
        this.operationConverter = operationConverter;
        this.messageHub = messageHub;
        this.context = context;
        this.apiObjectFactory = apiObjectFactory;
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

        Transaction transaction = new Transaction(
                Util.hexToBytes("773a625737a4686a40d832569f1691730bba2c1adc8009ee6b341b71fff35c18"),
                feePayerKey,
                null,
                operationList
        );
        TransactionBroadcastSequence listener = new TransactionBroadcastSequence(transaction, feeAsset, responseListener);
        broadcastTransaction(listener, responseObject);
    }

    @SneakyThrows
    private void broadcastTransaction(WebSocketListener listener, final Object lock) {
        //TODO
//        WebSocket mWebSocket = factory.createSocket("wss://eu.openledger.info/ws");
        WebSocket mWebSocket = factory.createSocket(System.getenv("NODE_WS"));


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

        BasicThreadFactory factory = new BasicThreadFactory.Builder()
                .namingPattern("ExternalAPI-%d")
                .daemon(false)
                .build();
        threadPool = Executors.newCachedThreadPool(factory);


        feeAsset = getAssetSymbol(defaultAssets.getFeeAsset());
        msgAsset = new AssetAmountGraphene(castToGraphene(apiObjectFactory.getAsset(defaultAssets.getMessageAsset())), 1);
    }

    @Override
    public void sendProposal(UserAccount from, UserAccount to, UserAccount feePayer, AssetAmount assetAmount, String memo) {
        UserAccountGraphene fromGrapheneAccount = castToGraphene(from);
        UserAccountGraphene toGrapheneAccount = castToGraphene(to);
        com.github.utransnet.graphenej.UserAccount fromRaw = fromGrapheneAccount.getRaw();
        com.github.utransnet.graphenej.UserAccount toRaw = toGrapheneAccount.getRaw();
        com.github.utransnet.graphenej.AssetAmount assetRaw =
                this.<AssetAmountGraphene>castToGraphene(assetAmount).getRaw();

        // Creating memo
        BigInteger nonce = BigInteger.ONE;
        byte[] encryptedMessage = Memo.encryptMessage(
                fromGrapheneAccount.getKey(),
                new Address(ECKey.fromPublicOnly(toGrapheneAccount.getKey().getPubKey())),
                nonce,
                memo
        );
        Memo memoObj = new Memo(
                new Address(ECKey.fromPublicOnly(fromGrapheneAccount.getKey().getPubKey())),
                new Address(ECKey.fromPublicOnly(toGrapheneAccount.getKey().getPubKey())),
                nonce,
                encryptedMessage
        );

        TransferOperation transferOperation = new TransferOperationBuilder()
                .setTransferAmount(assetRaw)
                .setSource(fromRaw)
                .setDestination(toRaw)
                .setMemo(memoObj)
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
            UserAccountGraphene toGrapheneAccount = castToGraphene(to);
            com.github.utransnet.graphenej.UserAccount fromRaw = fromGrapheneAccount.getRaw();
            com.github.utransnet.graphenej.UserAccount toRaw = toGrapheneAccount.getRaw();
            com.github.utransnet.graphenej.AssetAmount assetRaw =
                    ((AssetAmountGraphene) castToGraphene(assetAmount)).getRaw();


            // Creating memo
            BigInteger nonce = BigInteger.ONE;
            byte[] encryptedMessage = Memo.encryptMessage(
                    fromGrapheneAccount.getKey(),
                    new Address(ECKey.fromPublicOnly(toGrapheneAccount.getKey().getPubKey())),
                    nonce,
                    memo
            );
            Memo memoObj = new Memo(
                    new Address(ECKey.fromPublicOnly(fromGrapheneAccount.getKey().getPubKey())),
                    new Address(ECKey.fromPublicOnly(toGrapheneAccount.getKey().getPubKey())),
                    nonce,
                    encryptedMessage
            );

            TransferOperation transferOperation = new TransferOperationBuilder()
                    .setTransferAmount(assetRaw)
                    .setSource(fromRaw)
                    .setDestination(toRaw)
                    .setMemo(memoObj)
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
                .collect(toList());
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
        if (responseObject.getResult() == null) {
            return new ArrayList<>(0);
        }
        return responseObject.getResult()
                .stream()
                .map(proposal -> new ProposalGraphene(proposal, operationConverter))
                .collect(toList());
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
        if (responseObject.getResult() == null) {
            throw new NotFoundException("Account with name '" + name + "' not found");
        }
        UserAccountGraphene userAccount = castToGraphene(getAccountById(responseObject.getResult().id));
        userAccount.setName(responseObject.getResult().name);

        return userAccount;
    }

    @Override
    public UserAccount getAccountById(String id) {
        return apiObjectFactory.userAccount(id);
    }

    @Override
    public List<AssetAmount> getAccountBalances(UserAccount account) {
        ArrayList<com.github.utransnet.graphenej.Asset> assets = new ArrayList<>(0);
        ResponseObject<List<com.github.utransnet.graphenej.AssetAmount>> responseObject = new ResponseObject<>();
        UserAccountGraphene accountGraphene = castToGraphene(account);
        GetAccountBalances getAccountBalances = new GetAccountBalances(
                accountGraphene.getRaw(),
                assets,
                new BlockingResponseListener<>(responseObject)
        );
        broadcastTransaction(getAccountBalances, responseObject);

        if (responseObject.getResult() == null) {
            throw new NotFoundException("Account with name '" + account.getName() + "' not found");
        }

        return responseObject.
                getResult()
                .stream()
                .map(assetAmount -> {
                            AssetAmount assetAmount1 = apiObjectFactory.getAssetAmount(
                                    assetAmount.getAsset().getObjectId(),
                                    assetAmount.getAmount().longValue());
                            AssetGraphene asset = castToGraphene(assetAmount1.getAsset());
                            asset.setSymbol(assetAmount.getAsset().getSymbol());
                            return assetAmount1;
                        }
                )
                .collect(toList());
    }

    @Override
    public Optional<? extends BaseOperation> getLastOperation(UserAccount account) {
        List<? extends BaseOperation> accountHistory = getAccountHistory(
                account,
                GetAccountHistory.NEWEST_TRANSACTION_ID,
                GetAccountHistory.OLDEST_TRANSACTION_ID,
                1
        );
        if (accountHistory.size() > 0 && accountHistory.get(0) != null) {
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
        if (responseObject.getResult() != null) {
            responseObject.getResult().forEach(historicalOperation -> {
                BaseOperationGraphene baseOperationGraphene = operationConverter.fromGrapheneOp(
                        historicalOperation.getOperation()
                );
                if (baseOperationGraphene != null) {
                    baseOperationGraphene.setId(historicalOperation.getId());
                    if (baseOperationGraphene.getOperationType() == OperationType.PROPOSAL_CREATE) {
                        if (historicalOperation.result.length == 2) {
                            String proposalId = (String) historicalOperation.result[1];
                            ((ProposalCreateOperationGraphene) baseOperationGraphene).getProposal().setId(proposalId);
                        }
                    }
                }
                history.add(baseOperationGraphene);
            });
        }

        return history;
    }


    @Override
    public void listenAccountUpdatesByUserId(String listenerId, Set<String> accsToListen, Consumer<AccountUpdateObject> onUpdate) {
        messageHub.addNewListener(listenerId, new AccountSubscriptionListener() {
            @Override
            public List<String> getInterestedAccountNames() {
                return new ArrayList<>(accsToListen);
            }

            @Override
            public void onSubscriptionUpdate(SubscriptionResponse subscriptionResponse) {
                MessageHub.convertAndConsume(subscriptionResponse, AccountTransactionHistoryObject.class)
                        .forEach(accountTransactionHistoryObject -> {
                            // filter accounts, because notifications delivered by chuncks
                            if (accsToListen.contains(accountTransactionHistoryObject.getAccount())) {
//                            GrapheneObject object = getObject(accountTransactionHistoryObject.getOperation_id());
                                //TODO: create AccountUpdateObject
                                onUpdate.accept(null);
                            }
                        });
            }
        });
    }

    @Override
    public void removeAccountUpdateListener(String listenerId) {
        messageHub.removeListener(listenerId);
    }

    @Override
    public void listenAccountOperationsByUserId(String listenerId, Set<String> accsToListen, Consumer<ExternalObject> onUpdate) {
        messageHub.addNewListener(listenerId, new AccountSubscriptionListener() {
            @Override
            public List<String> getInterestedAccountNames() {
                return new ArrayList<>(accsToListen);
            }

            @Override
            public void onSubscriptionUpdate(SubscriptionResponse subscriptionResponse) {
                MessageHub.convertAndConsume(subscriptionResponse, AccountTransactionHistoryObject.class)
                        .forEach(accountTransactionHistoryObject -> {
                            // filter accounts, because notifications delivered by chuncks
                            if (accsToListen.contains(accountTransactionHistoryObject.getAccount())) {
//                            GrapheneObject object = getObject(accountTransactionHistoryObject.getOperation_id());
                                //TODO: get object caused update
                                /*if (object != null && object.getObjectType() == ObjectType.OPERATION_HISTORY_OBJECT) {
                                    HistoricalOperation operation = (HistoricalOperation) object;
                                }*/
                                onUpdate.accept(null);
                            }
                        });
            }
        });
    }

    @Override
    public void removeAccountOperationListener(String listenerId) {
        messageHub.removeListener(listenerId);
    }

    @Nullable
    GrapheneObject getObject(String id) {
        List<GrapheneObject> objects = getObjects(Collections.singletonList(id));
        if (!objects.isEmpty()) {
            return objects.get(0);
        } else {
            return null;
        }
    }

    List<GrapheneObject> getObjects(List<String> ids) {
        ResponseObject<List<GrapheneObject>> responseObject = new ResponseObject<>();
        GetObjects listener = new GetObjects(
                ids,
                new BlockingResponseListener<>(responseObject)
        );
        broadcastTransaction(listener, responseObject);

        if (responseObject.getResult() == null) {
            return new ArrayList<>(0);
        } else {
            return responseObject.getResult();
        }
    }

    List<com.github.utransnet.graphenej.Asset> getAssetsSymbols(List<String> assets) {
        ResponseObject<List<com.github.utransnet.graphenej.Asset>> responseObject = new ResponseObject<>();
        LookupAssetSymbols listener = new LookupAssetSymbols(
                new ArrayList<>(assets),
                new BlockingResponseListener<>(responseObject)
        );
        broadcastTransaction(listener, responseObject);

        if (responseObject.getResult() == null) {
            return new ArrayList<>(0);
        } else {
            return responseObject.getResult();
        }
    }

    @Nullable
    com.github.utransnet.graphenej.Asset getAssetSymbol(String assetId) {
        List<com.github.utransnet.graphenej.Asset> objects = getAssetsSymbols(Collections.singletonList(assetId));
        if (!objects.isEmpty()) {
            return objects.get(0);
        } else {
            return null;
        }
    }

}
