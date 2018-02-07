package com.github.utransnet.simulator.externalapi.operations;

import com.github.utransnet.simulator.externalapi.Asset;
import com.github.utransnet.simulator.externalapi.AssetAmount;
import com.github.utransnet.simulator.externalapi.UserAccount;

/**
 * Created by Artem on 06.02.2018.
 */
public interface MessageOperation extends BaseOperation {

    UserAccount getTo();
    UserAccount getFrom();
    String getMessage();
}
