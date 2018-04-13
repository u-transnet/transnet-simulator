package com.github.utransnet.simulator.route;

import com.github.utransnet.simulator.externalapi.AssetAmount;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Artem on 28.02.2018.
 */
@Getter
@Setter
public class SerializedUserInfo {
    String id;
    String name;
    String wif;

    @Nullable
    String reservationWif;

    List<AssetAmount> balance = new ArrayList<>(4);
}
