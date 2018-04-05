package com.github.utransnet.simulator;

import com.fasterxml.jackson.core.JsonParseException;
import com.github.utransnet.simulator.externalapi.APIObjectFactory;
import com.github.utransnet.simulator.externalapi.ExternalAPI;
import com.github.utransnet.simulator.externalapi.UserAccount;
import com.github.utransnet.simulator.externalapi.operations.BaseOperation;
import com.github.utransnet.simulator.queue.InputQueue;
import com.github.utransnet.simulator.route.RouteMap;
import com.github.utransnet.simulator.route.RouteMapFactory;
import com.github.utransnet.simulator.route.ScenarioContainer;
import com.github.utransnet.simulator.services.SimulationStartedException;
import com.github.utransnet.simulator.services.Supervisor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Created by Artem on 01.02.2018.
 */
@Slf4j
@Controller
@EnableAutoConfiguration
@ComponentScan
@EntityScan(
        basePackageClasses = {Main.class, Jsr310JpaConverters.class}
)
@Import(AppConfig.class)
public class Main {


    private final ApplicationContext context;

    private final Supervisor supervisor;

    private final InputQueue<RouteMap> routeMapInputQueue;

    private final RouteMapFactory routeMapFactory;

    @Autowired
    public Main(
            ApplicationContext context,
            Supervisor supervisor,
            InputQueue<RouteMap> routeMapInputQueue,
            RouteMapFactory routeMapFactory,
            APIObjectFactory objectFactory,
            ExternalAPI externalAPI
            ) {
        this.context = context;
        this.supervisor = supervisor;
        this.routeMapInputQueue = routeMapInputQueue;
        this.routeMapFactory = routeMapFactory;

        UserAccount userAccount = objectFactory.userAccount("1.2.638651");
        List<? extends BaseOperation> accountHistory = externalAPI.getAccountHistory(userAccount);
        log.info(accountHistory.toString());
    }


    public static void main(String[] args) throws Exception {
        SpringApplication.run(Main.class, args);
    }

    @RequestMapping("/import_route")
    ResponseEntity importRoute(@RequestBody String routeMapJson) {
        RouteMap routeMap;
        try {
            routeMap = routeMapFactory.fromJsonForce(routeMapJson);
            routeMapInputQueue.offer(routeMap);
            return new ResponseEntity(HttpStatus.OK);
        } catch (JsonParseException e) {
            log.error("Incorrect RouteMap", e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping("/import_config")
    ResponseEntity importConfig(@RequestBody ScenarioContainer scenario) {
        try {
            supervisor.init(scenario);
            return new ResponseEntity(HttpStatus.OK);
        } catch (SimulationStartedException e) {
            log.error("Duplicate simulation init requested", e);
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
    }

}
