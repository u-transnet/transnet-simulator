package com.github.utransnet.simulator.logging;

import com.github.utransnet.simulator.Utils;
import com.github.utransnet.simulator.actors.CheckPoint;
import com.github.utransnet.simulator.actors.RailCar;
import com.github.utransnet.simulator.externalapi.ExternalAPI;
import com.github.utransnet.simulator.externalapi.Proposal;
import com.github.utransnet.simulator.externalapi.UserAccount;
import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import com.github.utransnet.simulator.externalapi.operations.OperationType;
import com.github.utransnet.simulator.externalapi.operations.TransferOperation;
import com.github.utransnet.simulator.route.Scenario;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.mutable.MutableBoolean;

/**
 * Created by Artem on 19.03.2018.
 */
@Slf4j
public class PositionMonitoring {
    private final ExternalAPI externalAPI;

    public PositionMonitoring(ExternalAPI externalAPI) {
        this.externalAPI = externalAPI;
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
        externalAPI.listenAccountOperations(
                "logger-update-listener-" + railCar.getId(),
                Utils.setOf(railCar.getUTransnetAccount(), railCar.getReservation()),
                externalObject -> {
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

                    Proposal last = railCar.getUTransnetAccount()
                            .getProposals()
                            .stream()
                            .filter(proposal -> {
                                BaseOperation operation = proposal.getOperation();
                                if (operation.getOperationType() == OperationType.TRANSFER) {
                                    if (((TransferOperation) operation).getAsset().getId().equals("RA")) {
                                        return true;
                                    }
                                }
                                return false;
                            })
                            .reduce((p1, p2) -> p2).orElse(null);
                    if (last != null) {
                        BaseOperation operation = last.getOperation();
                        if (operation.getOperationType() == OperationType.TRANSFER) {
                            TransferOperation transferOperation = (TransferOperation) operation;
                            if (transferOperation.getFrom().equals(railCar.getReservation())) {
                                if (last.approved()) {
                                    log.trace(String.format(
                                            "<%s>|<%s>: RailCar '%s' entered check point '%s'",
                                            railCar.getUTransnetAccount().getName(),
                                            "enterCP",
                                            railCar.getUTransnetAccount().getName(),
                                            transferOperation.getTo().getName()
                                    ));
                                }
                            } else if (transferOperation.getTo().equals(railCar.getUTransnetAccount())) {
                                if (last.approved()) {
                                    log.trace(String.format(
                                            "<%s>|<%s>: RailCar '%s' left check point '%s'",
                                            railCar.getUTransnetAccount().getName(),
                                            "leaveCP",
                                            railCar.getUTransnetAccount().getName(),
                                            transferOperation.getFrom().getName()
                                    ));
                                }
                            }
                        }
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
