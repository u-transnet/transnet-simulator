package com.github.utransnet.simulator.externalapi.graphenej;

import com.github.utransnet.graphenej.ObjectType;
import com.github.utransnet.graphenej.api.ObjectSubscriptionListener;
import com.github.utransnet.graphenej.api.SubscriptionMessagesHub;
import com.github.utransnet.graphenej.interfaces.NodeErrorListener;
import com.github.utransnet.graphenej.interfaces.SubscriptionListener;
import com.github.utransnet.graphenej.models.DynamicGlobalProperties;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Artem on 06.04.2018.
 */
@Slf4j
public class MessageHub {

    protected String NODE_URL = System.getenv("NODE_WS");
    protected SSLContext context;
    protected WebSocket mWebSocket;
    private NodeErrorListener mErrorListener = error -> log.error(error.message);
    private SubscriptionMessagesHub mMessagesHub;

    private final Map<String, SubscriptionListener> map = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static <T extends Serializable> List<T> convertAndConsume(
            SubscriptionResponse response,
            Class<T> clazz) {
        if (response.params.size() == 2 && response.params.get(1) instanceof ArrayList) {
            ArrayList<Serializable> serializable = (ArrayList) response.params.get(1);

            return serializable.stream()
                    .filter(o -> clazz.isAssignableFrom(o.getClass()))
                    .map(o -> (T) o)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>(0);
    }

    @PostConstruct
    @SneakyThrows
    private void init() {
        context = NaiveSSLContext.getInstance("TLS");
        WebSocketFactory factory = new WebSocketFactory();

        // Set the custom SSL context.
        factory.setSSLContext(context);

        mWebSocket = factory.createSocket(NODE_URL);

        mMessagesHub = new SubscriptionMessagesHub("", "", false, mErrorListener);
        mMessagesHub.addSubscriptionListener(new ObjectSubscriptionListener() {

            // Like keep alive listener on each block every 3 seconds
            @Override
            public ObjectType getInterestObjectType() {
                return ObjectType.DYNAMIC_GLOBAL_PROPERTY_OBJECT;
            }

            @Override
            public void onSubscriptionUpdate(SubscriptionResponse response) {
                log.trace("Received ACCOUNT_STATISTICS_OBJECT");
                convertAndConsume(response, DynamicGlobalProperties.class).forEach(o -> {
                    log.trace("Head block number: " + o.head_block_number);
                    log.trace("Witness: " + o.current_witness);
                });

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

    void addNewListener(String listenerId, SubscriptionListener listener) {
        map.put(listenerId, listener);
        mMessagesHub.addSubscriptionListener(listener);
    }

    void removeListener(String listenerId) {
        SubscriptionListener toRemove = map.remove(listenerId);
        mMessagesHub.removeSubscriptionListener(toRemove);
    }


}
