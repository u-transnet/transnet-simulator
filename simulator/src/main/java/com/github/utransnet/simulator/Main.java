package com.github.utransnet.simulator;

import com.github.utransnet.simulator.queue.InputQueue;
import com.github.utransnet.simulator.route.RouteMap;
import com.github.utransnet.simulator.route.Scenario;
import com.github.utransnet.simulator.services.SimulationStartedException;
import com.github.utransnet.simulator.services.Supervisor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Created by Artem on 01.02.2018.
 */
@Slf4j
@Controller
@EnableAutoConfiguration
@ComponentScan
public class Main {


    private final ApplicationContext context;

    private final Supervisor supervisor;

    private final InputQueue<RouteMap> routeMapInputQueue;

    @Autowired
    public Main(
            ApplicationContext context,
            Supervisor supervisor,
            InputQueue<RouteMap> routeMapInputQueue
            ) {
        this.context = context;
        this.supervisor = supervisor;
        this.routeMapInputQueue = routeMapInputQueue;
    }


    public static void main(String[] args) throws Exception {
        SpringApplication.run(Main.class, args);
    }

    @RequestMapping("/import_route")
    @ResponseStatus(HttpStatus.OK)
    void importRoute(@RequestBody RouteMap routeMap) {
        routeMapInputQueue.offer(routeMap);
    }

    @RequestMapping("/import_config")
    ResponseEntity importConfig(@RequestBody Scenario scenario) {
        try {
            supervisor.init(scenario);
            return new ResponseEntity(HttpStatus.OK);
        } catch (SimulationStartedException e) {
            log.error("Duplicate simulation inti requested", e);
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
    }

}
