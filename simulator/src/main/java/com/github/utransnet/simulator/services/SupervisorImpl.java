package com.github.utransnet.simulator.services;

import com.github.utransnet.simulator.actors.*;
import com.github.utransnet.simulator.actors.factory.Actor;
import com.github.utransnet.simulator.actors.factory.ActorFactory;
import com.github.utransnet.simulator.externalapi.APIObjectFactory;
import com.github.utransnet.simulator.externalapi.ExternalAPI;
import com.github.utransnet.simulator.externalapi.UserAccount;
import com.github.utransnet.simulator.queue.InputQueue;
import com.github.utransnet.simulator.route.RouteMap;
import com.github.utransnet.simulator.route.Scenario;
import com.github.utransnet.simulator.route.ScenarioContainer;
import com.github.utransnet.simulator.route.SerializedUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.HashSet;
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

    private final String supervisorId = "supervisor";
    private UserAccount supervisorAccount;


    private boolean started = false;
    private Set<Actor> actors;

    public SupervisorImpl(
            InputQueue<RouteMap> routeMapInputQueue,
            InputQueue<Client> clientInputQueue,
            ActorFactory actorFactory,
            ExternalAPI externalAPI,
            APIObjectFactory apiObjectFactory
    ) {
        this.routeMapInputQueue = routeMapInputQueue;
        this.clientInputQueue = clientInputQueue;
        this.actorFactory = actorFactory;
        this.externalAPI = externalAPI;
        this.apiObjectFactory = apiObjectFactory;
        actors = new HashSet<>(128);
    }

    protected Scenario loadScenario(ScenarioContainer scenarioContainer) {
        log.debug("loading scenario");
        Scenario scenario = new Scenario();
        scenario.setRouteMapPrice(scenarioContainer.routeMapPrice);

        SerializedUserInfo logistInfo = scenarioContainer.logist;
        Logist logist = actorFactory.createLogistBuilder()
                .id(logistInfo.getId())
                .uTransnetAccount(initUserAccount(logistInfo))
                .build();

        logist.setRouteMapPrice(scenarioContainer.routeMapPrice.getAmount());
        scenario.setLogist(logist);
        actors.add(logist);

        scenarioContainer.clients.forEach(userInfo -> {
            Client client = actorFactory.createClientBuilder()
                    .id(userInfo.getId())
                    .uTransnetAccount(initUserAccount(userInfo))
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
                    .id(userInfo.getId())
                    .uTransnetAccount(uTransnetAccount)
                    .build();
            checkPoint.setLogist(logist.getUTransnetAccount());

            if (infrastructureInfo.isStation()) {
                Station station = actorFactory.createStationBuilder()
                        .id(userInfo.getId())
                        .uTransnetAccount(externalAPI.getAccountById(userInfo.getId()))
                        .build();
                station.setCheckPoint(checkPoint);
                actors.add(station);
            }

            scenario.addActor(checkPoint);
            actors.add(checkPoint);
        });

        scenarioContainer.railCars.forEach(serializedRailCarInfo -> {
            RailCar railCar = actorFactory.createRailCarBuilder()
                    .id(serializedRailCarInfo.getUserInfo().getId())
                    .uTransnetAccount(initUserAccount(serializedRailCarInfo.getUserInfo()))
                    .build();
            scenario.addActor(railCar);
            actors.add(railCar);

            UserAccount station = externalAPI.getAccountById(serializedRailCarInfo.getStartPointId());
            UserAccount reservation = externalAPI.getAccountByName(station.getId() + "-reserve");
            // pay to checkpoint for new RailCar
            String stubRouteMapId = "free-" + railCar.getId();
            // inform about RouteMap
            logist.getUTransnetAccount().sendMessage(station, stubRouteMapId);
            externalAPI.sendProposal(
                    supervisorAccount,
                    station,
                    supervisorAccount,
                    supervisorAccount,
                    apiObjectFactory.getAssetAmount("UTT", 10),
                    stubRouteMapId
            );
            supervisorAccount.sendAsset(
                    station,
                    apiObjectFactory.getAssetAmount("UTT", 2),
                    stubRouteMapId
            );
            // ask entrance
            externalAPI.sendProposal(
                    reservation,
                    railCar.getUTransnetAccount(),
                    station,
                    railCar.getUTransnetAccount(),
                    apiObjectFactory.getAssetAmount("RA", 10),
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
        UserAccount account = externalAPI.getAccountById(userInfo.getId());
        userInfo.getBalance().forEach(assetAmount -> supervisorAccount.sendAsset(account, assetAmount, "init"));
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
            supervisorAccount = externalAPI.getAccountById(supervisorId);
            Scenario scenario = loadScenario(scenarioContainer);

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
