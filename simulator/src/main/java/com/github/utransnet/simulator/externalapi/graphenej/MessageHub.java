package com.github.utransnet.simulator.externalapi.graphenej;

import com.github.utransnet.graphenej.ObjectType;
import com.github.utransnet.graphenej.api.SubscriptionMessagesHub;
import com.github.utransnet.graphenej.interfaces.NodeErrorListener;
import com.github.utransnet.graphenej.interfaces.SubscriptionListener;
import com.github.utransnet.graphenej.models.AccountTransactionHistoryObject;
import com.github.utransnet.graphenej.models.SubscriptionResponse;
import com.github.utransnet.graphenej.test.NaiveSSLContext;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by Artem on 06.04.2018.
 */
@Slf4j
public class MessageHub {

    private final Map<String, Consumer<AccountTransactionHistoryObject>> map = new HashMap<>();
    protected String NODE_URL = System.getenv("wss://eu.openledger.info/ws");
    protected SSLContext context;
    protected WebSocket mWebSocket;
    private NodeErrorListener mErrorListener = error -> log.error(error.message);
    private SubscriptionMessagesHub mMessagesHub;

    @PostConstruct
    @SneakyThrows
    private void init() {
        context = NaiveSSLContext.getInstance("TLS");
        WebSocketFactory factory = new WebSocketFactory();

        // Set the custom SSL context.
        factory.setSSLContext(context);

        mWebSocket = factory.createSocket(NODE_URL);

        mMessagesHub = new SubscriptionMessagesHub("", "", true, mErrorListener);
        mMessagesHub.addSubscriptionListener(new SubscriptionListener() {
            @Override
            public ObjectType getInterestObjectType() {
                return ObjectType.ACCOUNT_STATISTICS_OBJECT;
            }

            @Override
            public void onSubscriptionUpdate(SubscriptionResponse response) {
                log.debug("Received ACCOUNT_STATISTICS_OBJECT");
                if (response.params.size() == 2 && response.params.get(1) instanceof ArrayList) {
                    @SuppressWarnings("unchecked")
                    ArrayList<Serializable> serializable = (ArrayList) response.params.get(1);
                    serializable.stream()
                            .filter(o -> o instanceof AccountTransactionHistoryObject)
                            .map(o -> (AccountTransactionHistoryObject) o)
                            .forEach(o -> {
                                map.values().forEach(listener -> listener.accept(o));
                            });
                }
            }
        });
        mWebSocket.addListener(mMessagesHub);

        new Thread(() -> {
            try {
                mWebSocket.connect();

            } catch (WebSocketException e) {
                log.error("Error while connecting to web socket " + NODE_URL, e);
            }
        }).start();
    }

    void addNewListener(String id, Consumer<AccountTransactionHistoryObject> callback) {
        map.put(id, callback);
    }

    void removeListener(String id) {
        map.remove(id);
    }
}
