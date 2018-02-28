package com.github.utransnet.simulator.services;

import com.github.utransnet.simulator.actors.CheckPoint;
import com.github.utransnet.simulator.actors.Client;
import com.github.utransnet.simulator.actors.Logist;
import com.github.utransnet.simulator.actors.RailCar;
import com.github.utransnet.simulator.actors.factory.Actor;
import com.github.utransnet.simulator.actors.factory.ActorFactory;
import com.github.utransnet.simulator.externalapi.UserAccount;
import com.github.utransnet.simulator.queue.InputQueue;
import com.github.utransnet.simulator.route.RouteMap;
import com.github.utransnet.simulator.route.Scenario;
import com.github.utransnet.simulator.route.ScenarioContainer;
import com.github.utransnet.simulator.route.SerializedUserInfo;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.Set;
import java.util.concurrent.Executors;

/**
 * Created by Artem on 31.01.2018.
 */
public class SupervisorImpl implements Supervisor {

    private final InputQueue<RouteMap> routeMapInputQueue;
    private final InputQueue<Client> clientInputQueue;
    private final ActorFactory actorFactory;

    private UserAccount userAccount;

    private boolean started = false;
    private Set<Actor> actors;

    public SupervisorImpl(
            InputQueue<RouteMap> routeMapInputQueue,
            InputQueue<Client> clientInputQueue,
            ActorFactory actorFactory) {
        this.routeMapInputQueue = routeMapInputQueue;
        this.clientInputQueue = clientInputQueue;
        this.actorFactory = actorFactory;
    }

    protected Scenario loadScenario(ScenarioContainer scenarioContainer) {
        Scenario scenario = new Scenario();

        SerializedUserInfo logistInfo = scenarioContainer.logist;
        Logist logist = actorFactory.createLogistBuilder()
                .id(logistInfo.getId())
                .uTransnetAccount(initUserAccount(logistInfo))
                .build();

        scenario.setLogist(logist);

        scenarioContainer.clients.forEach(userInfo -> {
            Client client = actorFactory.createClientBuilder()
                    .id(userInfo.getId())
                    .uTransnetAccount(initUserAccount(userInfo))
                    .build();
            scenario.addActor(client);
        });

        scenarioContainer.infrastructure.forEach(userInfo -> {
            CheckPoint checkPoint = actorFactory.createCheckPointBuilder()
                    .id(userInfo.getId())
                    .uTransnetAccount(initUserAccount(userInfo))
                    .build();
            scenario.addActor(checkPoint);
        });

        scenarioContainer.railCars.forEach(serializedRailCarInfo -> {
            RailCar railCar = actorFactory.createRailCarBuilder()
                    .id(serializedRailCarInfo.getUserInfo().getId())
                    .uTransnetAccount(initUserAccount(serializedRailCarInfo.getUserInfo()))
                    .build();
            scenario.addActor(railCar);
        });
        return scenario;
    }

    protected void addRouteMap(RouteMap routeMaps) {

    }

    protected void addClient(Client client) {

    }

    private void setLogist(Logist logist) {

    }

    protected UserAccount initUserAccount(SerializedUserInfo userInfo) {

        return null;
    }

    protected void update(int seconds) {
        actors.forEach(actor -> actor.update(seconds));
    }


    private void run() {
        long currentTime = System.currentTimeMillis();
        long lastTime = currentTime;
        while (true) {
            currentTime = System.currentTimeMillis();
            int elapsed = (int) (currentTime - lastTime);
            checkInput();
            update(elapsed);
            lastTime = currentTime;
        }
    }

    @Override
    public void init(ScenarioContainer scenarioContainer) throws SimulationStartedException {
        if (!started) {
            Scenario scenario = loadScenario(scenarioContainer);
            prepareInfrastructure(scenario);
            BasicThreadFactory factory = new BasicThreadFactory.Builder()
                    .namingPattern("Main-Loop-%d")
                    .daemon(false)
                    .priority(Thread.MAX_PRIORITY)
                    .build();
            Executors.newSingleThreadExecutor(factory).submit(this::run);
        } else {
            throw new SimulationStartedException();
        }
    }

    protected void prepareInfrastructure(Scenario scenario) {

    }

    protected void checkInput() {
        Client newClient = clientInputQueue.poll();
        while (newClient != null) {
            addClient(newClient);
        }
    }
}
