package com.github.utransnet.simulator.externalapi.h2impl;

import com.github.utransnet.simulator.externalapi.ExternalAPI;
import com.github.utransnet.simulator.externalapi.UserAccount;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by Artem on 13.02.2018.
 */
class UserAccountH2 extends UserAccount {

    @Getter
    @Setter(AccessLevel.PACKAGE)
    private String name;

    UserAccountH2(ExternalAPI externalAPI) {
        super(externalAPI);
    }

    @Override
    public String getId() {
        return getName();
    }
}
