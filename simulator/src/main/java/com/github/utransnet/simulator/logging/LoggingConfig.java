package com.github.utransnet.simulator.logging;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

/**
 * Created by Artem on 15.03.2018.
 */
@Configuration
@Import({
        TransactionLogger.class
})
@EnableAspectJAutoProxy(proxyTargetClass = true)
//@EnableSpringConfigured
public class LoggingConfig {
}
