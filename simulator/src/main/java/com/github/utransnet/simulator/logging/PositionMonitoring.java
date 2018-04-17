package com.github.utransnet.simulator.logging;

import com.github.utransnet.simulator.Utils;
import com.github.utransnet.simulator.actors.CheckPoint;
import com.github.utransnet.simulator.actors.RailCar;
import com.github.utransnet.simulator.externalapi.DefaultAssets;
import com.github.utransnet.simulator.externalapi.ExternalAPI;
import com.github.utransnet.simulator.externalapi.UserAccount;
import com.github.utransnet.simulator.externalapi.operations.TransferOperation;
import com.github.utransnet.simulator.route.Scenario;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

/**
 * Created by Artem on 19.03.2018.
 */
@Slf4j
public class PositionMonitoring {
    private final ExternalAPI externalAPI;
    private final DefaultAssets defaultAssets;

    public PositionMonitoring(ExternalAPI externalAPI, DefaultAssets defaultAssets) {
        this.externalAPI = externalAPI;
        this.defaultAssets = defaultAssets;
    }

    public void init(Scenario scenario) {
        scenario.getInfrastructure().forEach(this::addCheckPoint);
        scenario.getRailCars().forEach(this::addRailCar);
    }

    /*public void addClient(Client client) {
        MutableBoolean hasClient = new MutableBoolean(false);
        externalAPI.listenAccountOperations(
                "logger-update-listener-" + client.getId(),
                Utils.setOf(client.getUTransnetAccount()),
                externalObject -> {

                }
        );
    }*/

    private void addRailCar(RailCar railCar) {
        MutableBoolean hasClient = new MutableBoolean(false);
        MutableBoolean onStation = new MutableBoolean(false);
        Mutable<String> actualCheckpoints = new MutableObject<>("");
        externalAPI.listenAccountOperations(
                "logger-update-listener-" + railCar.getId(),
                Utils.setOf(railCar.getUTransnetAccount(), railCar.getReservation()),
                newObject -> {
                    UserAccount stationAccount = railCar.getStationAccount();
                    if (stationAccount != null && onStation.isFalse()) {
                        log.trace(String.format(
                                "<%s>|<%s>: RailCar '%s' entered a station",
                                railCar.getUTransnetAccount().getName(),
                                "enterStation",
                                railCar.getUTransnetAccount().getName()
                        ));
                        onStation.setTrue();
                    } else if (stationAccount == null && onStation.isTrue()) {
                        log.trace(String.format(
                                "<%s>|<%s>: RailCar '%s' left a station",
                                railCar.getUTransnetAccount().getName(),
                                "leaveStation",
                                railCar.getUTransnetAccount().getName()
                        ));
                        onStation.setFalse();
                    }

                    String clientId = railCar.getClientId();
                    if (clientId != null && hasClient.isFalse()) {
                        log.trace(String.format(
                                "<%s>|<%s>: RailCar '%s' took a client",
                                railCar.getUTransnetAccount().getName(),
                                "takeClient",
                                railCar.getUTransnetAccount().getName()
                        ));
                        hasClient.setTrue();
                    } else if (clientId == null && hasClient.isTrue()) {
                        log.trace(String.format(
                                "<%s>|<%s>: RailCar '%s' dropped a client",
                                railCar.getUTransnetAccount().getName(),
                                "dropClient",
                                railCar.getUTransnetAccount().getName()
                        ));
                        hasClient.setFalse();
                    }


                    List<String> enteredCP = railCar.getUTransnetAccount()
                            .getTransfers()
                            .stream()
                            .filter(to -> to.getTo().equals(railCar.getUTransnetAccount()))
                            .filter(to -> Objects.equals(to.getAsset().getSymbol(), defaultAssets.getResourceAsset()))
                            .map(TransferOperation::getFrom)
                            .map(UserAccount::getName)
                            .filter(s -> s.contains("-reserve"))
                            .map(s -> s.replace("-reserve", ""))
                            .collect(Collectors.toList());

                    List<String> leftCP = railCar.getReservation()
                            .getTransfers()
                            .stream()
                            .filter(to -> to.getFrom().equals(railCar.getReservation()))
                            .filter(to -> Objects.equals(to.getAsset().getSymbol(), defaultAssets.getResourceAsset()))
                            .map(TransferOperation::getTo)
                            .map(UserAccount::getName)
                            .collect(Collectors.toList());

                    enteredCP.removeAll(leftCP);

                    String actualCPs = enteredCP.stream().collect(joining(", "));
                    if (!actualCPs.equals(actualCheckpoints.getValue())) {
                        log.trace(String.format(
                                "<%s>|<%s>: RailCar '%s' currently in  '%s' check points",
                                railCar.getUTransnetAccount().getName(),
                                "enterCP",
                                railCar.getUTransnetAccount().getName(),
                                actualCPs
                        ));
                        actualCheckpoints.setValue(actualCPs);
                    }
                }
        );
    }

    private void addCheckPoint(CheckPoint checkPoint) {
        MutableBoolean vacant = new MutableBoolean(true);
        externalAPI.listenAccountOperations(
                "logger-update-listener-" + checkPoint.getId(),
                Utils.setOf(checkPoint.getUTransnetAccount(), checkPoint.getReservation()),
                externalObject -> {
                    UserAccount currentRailCar = checkPoint.getCurrentRailCar();
                    if (currentRailCar != null && vacant.isTrue()) {
                        log.trace(String.format(
                                "<%s>|<%s>: CheckPoint '%s' is busy",
                                checkPoint.getUTransnetAccount().getName(),
                                "cpBusy",
                                checkPoint.getUTransnetAccount().getName()
                        ));
                        vacant.setFalse();
                    } else if (currentRailCar == null && vacant.isFalse()) {
                        log.trace(String.format(
                                "<%s>|<%s>: CheckPoint '%s' is vacant",
                                checkPoint.getUTransnetAccount().getName(),
                                "cpVacant",
                                checkPoint.getUTransnetAccount().getName()
                        ));
                        vacant.setTrue();
                    }
                }
        );
    }
}
