package com.github.utransnet.simulator.services;

import com.github.utransnet.simulator.actors.Actor;
import com.github.utransnet.simulator.actors.Client;
import com.github.utransnet.simulator.queue.InputQueue;
import com.github.utransnet.simulator.route.RouteMap;
import com.github.utransnet.simulator.route.Scenario;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.Set;
import java.util.concurrent.Executors;

/**
 * Created by Artem on 31.01.2018.
 */
public class SupervisorImpl implements Supervisor {

    private final InputQueue<RouteMap> routeMapInputQueue;
    private final InputQueue<Client> clientInputQueue;
    private boolean started = false;
    private Set<Actor> actors;

    public SupervisorImpl(InputQueue<RouteMap> routeMapInputQueue, InputQueue<Client> clientInputQueue) {
        this.routeMapInputQueue = routeMapInputQueue;
        this.clientInputQueue = clientInputQueue;
    }

    protected void loadScenario(Scenario scenario) {

    }

    protected void addRouteMap(RouteMap routeMaps) {

    }

    protected void addClient(Client client) {

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
    public void init(Scenario scenario) throws SimulationStartedException {
        if (!started) {
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
