package com.github.utransnet.simulator.route;

import lombok.Getter;
import lombok.Setter;

/**
 * Created by Artem on 28.02.2018.
 */
@Getter
@Setter
public class SerializedRailCarInfo {
    SerializedUserInfo userInfo;
    String startPointId;
}