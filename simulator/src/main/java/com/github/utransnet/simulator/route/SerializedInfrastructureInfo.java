package com.github.utransnet.simulator.route;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by Artem on 28.02.2018.
 */
@Getter
@Setter
@AllArgsConstructor
public class SerializedInfrastructureInfo {
    SerializedUserInfo userInfo;
    boolean station;
}
