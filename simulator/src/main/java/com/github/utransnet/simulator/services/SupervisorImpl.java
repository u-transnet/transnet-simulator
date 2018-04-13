package com.github.utransnet.simulator.services;

import com.github.utransnet.simulator.actors.*;
import com.github.utransnet.simulator.actors.factory.Actor;
import com.github.utransnet.simulator.actors.factory.ActorFactory;
import com.github.utransnet.simulator.externalapi.*;
import com.github.utransnet.simulator.logging.PositionMonitoring;
import com.github.utransnet.simulator.queue.InputQueue;
import com.github.utransnet.simulator.route.RouteMap;
import com.github.utransnet.simulator.route.Scenario;
import com.github.utransnet.simulator.route.ScenarioContainer;
import com.github.utransnet.simulator.route.SerializedUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Created by Artem on 31.01.2018.
 */
@Slf4j
public class SupervisorImpl implements Supervisor {

    private final InputQueue<RouteMap> routeMapInputQueue;
    private final InputQueue<Client> clientInputQueue;
    private final ActorFactory actorFactory;
    private final ExternalAPI externalAPI;
    private final APIObjectFactory apiObjectFactory;
    private final PositionMonitoring positionMonitoring;
    private final DefaultAssets defaultAssets;

    private final String supervisorId = "simulator-supervisor";
    private UserAccount supervisorAccount;

    private AssetAmount startingMsgAssetAmount;
    private AssetAmount startingFeeAssetAmount;

    private boolean started = false;
    private Set<Actor> actors;

    public SupervisorImpl(
            InputQueue<RouteMap> routeMapInputQueue,
            InputQueue<Client> clientInputQueue,
            ActorFactory actorFactory,
            ExternalAPI externalAPI,
            APIObjectFactory apiObjectFactory,
            PositionMonitoring positionMonitoring,
            DefaultAssets defaultAssets
    ) {
        this.routeMapInputQueue = routeMapInputQueue;
        this.clientInputQueue = clientInputQueue;
        this.actorFactory = actorFactory;
        this.externalAPI = externalAPI;
        this.apiObjectFactory = apiObjectFactory;
        this.positionMonitoring = positionMonitoring;
        this.defaultAssets = defaultAssets;
        actors = new HashSet<>(128);
    }

    @PostConstruct
    private void postConstruct() {
        startingFeeAssetAmount = apiObjectFactory.getAssetAmount(defaultAssets.getFeeAsset(), 10000000);
        startingMsgAssetAmount = apiObjectFactory.getAssetAmount(defaultAssets.getMessageAsset(), 100);
    }

    protected Scenario loadScenario(ScenarioContainer scenarioContainer) {
        log.debug("loading scenario");
        Scenario scenario = new Scenario();
        scenario.setRouteMapPrice(scenarioContainer.routeMapPrice);

        SerializedUserInfo logistInfo = scenarioContainer.logist;
        UserAccount logistAccount = initUserAccount(logistInfo);
        Logist logist = actorFactory.createLogistBuilder()
                .id(logistAccount.getId())
                .uTransnetAccount(logistAccount)
                .build();

        logist.setRouteMapPrice(scenarioContainer.routeMapPrice.getAmount());
        scenario.setLogist(logist);
        actors.add(logist);

        scenarioContainer.clients.forEach(userInfo -> {
            UserAccount clientAccount = initUserAccount(userInfo);
            Client client = actorFactory.createClientBuilder()
                    .id(clientAccount.getId())
                    .uTransnetAccount(clientAccount)
                    .build();
            client.setLogistName(scenario.getLogist().getId());
            client.setRouteMapPrice(scenario.getRouteMapPrice());
            scenario.addActor(client);
            actors.add(client);
        });

        scenarioContainer.infrastructure.forEach(infrastructureInfo -> {
            SerializedUserInfo userInfo = infrastructureInfo.getUserInfo();
            UserAccount uTransnetAccount = initUserAccount(userInfo);
            CheckPoint checkPoint = actorFactory.createCheckPointBuilder()
                    .id(uTransnetAccount.getId())
                    .uTransnetAccount(uTransnetAccount)
                    .build();
            checkPoint.setLogist(logist.getUTransnetAccount());
            if (userInfo.getReservationWif() != null) {
                checkPoint.setReservationWif(userInfo.getReservationWif());
            }

            if (infrastructureInfo.isStation()) {
                Station station = actorFactory.createStationBuilder()
                        .id(uTransnetAccount.getId())
                        .uTransnetAccount(externalAPI.getAccountByName(userInfo.getName()))
                        .build();
                station.setCheckPoint(checkPoint);
                actors.add(station);
            }

            scenario.addActor(checkPoint);
            actors.add(checkPoint);
        });

        scenarioContainer.railCars.forEach(serializedRailCarInfo -> {
            SerializedUserInfo userInfo = serializedRailCarInfo.getUserInfo();
            UserAccount uTransnetAccount = initUserAccount(userInfo);
            RailCar railCar = actorFactory.createRailCarBuilder()
                    .id(uTransnetAccount.getId())
                    .uTransnetAccount(uTransnetAccount)
                    .build();

            if (userInfo.getReservationWif() != null) {
                railCar.setReservationWif(userInfo.getReservationWif());
            }

            scenario.addActor(railCar);
            actors.add(railCar);

            Optional<CheckPoint> startingActor = scenario.getInfrastructure()
                    .stream()
                    .filter(checkPoint -> Objects.equals(
                            checkPoint.getUTransnetAccount().getName(),
                            serializedRailCarInfo.getStartPointName()
                    )).findFirst();
            if (!startingActor.isPresent()) {
                throw new RuntimeException("Not found starting point actor: " + serializedRailCarInfo.getStartPointName());
            }
            UserAccount station = startingActor.get().getUTransnetAccount();
            UserAccount reservation = startingActor.get().getReservation();
            // pay to checkpoint for new RailCar
            String stubRouteMapId = "free-" + railCar.getId();
            // inform about RouteMap
            logist.getUTransnetAccount().sendMessage(station, stubRouteMapId);
            externalAPI.sendProposal(
                    supervisorAccount,
                    station,
                    supervisorAccount,
                    apiObjectFactory.getAssetAmount(defaultAssets.getMainAsset(), 10),
                    stubRouteMapId
            );
            supervisorAccount.sendAsset(
                    station,
                    apiObjectFactory.getAssetAmount(defaultAssets.getMainAsset(), 2),
                    stubRouteMapId
            );
            // ask entrance
            externalAPI.sendProposal(
                    reservation,
                    railCar.getUTransnetAccount(),
                    railCar.getUTransnetAccount(),
                    apiObjectFactory.getAssetAmount(defaultAssets.getResourceAsset(), 10),
                    stubRouteMapId
            );

            // tell station about vacant RailCar
            railCar.getUTransnetAccount().sendMessage(station, "FREE");
        });

        supervisorAccount.getProposals().forEach(proposal -> supervisorAccount.approveProposal(proposal));
        return scenario;
    }

    protected void addRouteMap(RouteMap routeMaps) {

    }

    protected void addClient(Client client) {

    }

    protected UserAccount initUserAccount(SerializedUserInfo userInfo) {
        UserAccount account = externalAPI.getAccountByName(userInfo.getName());
        account.setKey(userInfo.getWif());
        userInfo.getBalance().forEach(assetAmount -> supervisorAccount.sendAsset(account, assetAmount, "init"));
        supervisorAccount.sendAsset(account, startingMsgAssetAmount, "init");
        supervisorAccount.sendAsset(account, startingFeeAssetAmount, "init");
        return account;
    }

    protected void update(int seconds) {
        actors.forEach(actor -> actor.update(seconds));
    }


    private void run() {
        log.info("Starting simulation");
        long currentTime = System.currentTimeMillis();
        long lastTime = currentTime;
        while (true) {
            currentTime = System.currentTimeMillis();
            int elapsed = (int) (currentTime - lastTime);
            checkInput();
            update(elapsed);
//            log.debug("Elapsed milliseconds: " + elapsed);
            lastTime = currentTime;
        }
    }

    @Override
    public void init(ScenarioContainer scenarioContainer) throws SimulationStartedException {
        if (!started) {
            supervisorAccount = externalAPI.getAccountByName(supervisorId);
            supervisorAccount.setKey("5J5kmCnAxYnTpYcScMnLHcFoT79iCsHLwbNuBADP5Wu1FqzEx3J");
            Scenario scenario = loadScenario(scenarioContainer);
            positionMonitoring.init(scenario);

            String stations = actors.stream()
                    .filter(actor -> actor instanceof Station)
                    .map(actor -> actor.getUTransnetAccount().getName())
                    .collect(Collectors.joining(", "));
            log.info("Stations: " + stations);

            String checkpoints = scenario.getInfrastructure()
                    .stream()
                    .map(actor -> actor.getUTransnetAccount().getName())
                    .collect(Collectors.joining(", "));
            log.info("CheckPoints: " + checkpoints);

            BasicThreadFactory factory = new BasicThreadFactory.Builder()
                    .namingPattern("Main-Loop-%d")
                    .daemon(false)
                    .priority(Thread.MAX_PRIORITY)
                    .build();
            Executors.newSingleThreadExecutor(factory).submit(this::run);
            started = true;
        } else {
            throw new SimulationStartedException();
        }
    }

    protected void checkInput() {
        Client newClient = clientInputQueue.poll();
        while (newClient != null) {
            addClient(newClient);
        }
    }
}
