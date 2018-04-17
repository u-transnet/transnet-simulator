package com.github.utransnet.simulator.externalapi.graphenej;

import com.github.utransnet.graphenej.interfaces.WitnessResponseListener;
import com.github.utransnet.graphenej.models.BaseResponse;
import com.github.utransnet.graphenej.models.WitnessResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Artem on 02.04.2018.
 */
@Slf4j
public class BlockingResponseListener<T> implements WitnessResponseListener {

    private final ResponseObject<T> responseObject;

    public BlockingResponseListener(ResponseObject<T> responseObject) {
        this.responseObject = responseObject;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSuccess(WitnessResponse response) {
        log.trace("onSuccess");
        if (response.result != null) {
            log.trace(response.result.toString());
        }
        responseObject.setResult((T) response.result);
        synchronized (responseObject) {
            responseObject.notifyAll();
        }
    }

    @Override
    public void onError(BaseResponse.Error error) {
        log.error("onError. Msg: " + error.data.message);
        synchronized (responseObject) {
            responseObject.notifyAll();
        }
    }
}
