package com.github.utransnet.simulator;

import com.github.utransnet.simulator.externalapi.h2impl.ProposalH2Repository;
import com.github.utransnet.simulator.externalapi.h2impl.ProposalH2Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Created by Artem on 15.02.2018.
 */
@Configuration
@EnableAutoConfiguration
@EntityScan(
        basePackageClasses = {TestWithJpaConfig.class, Jsr310JpaConverters.class}
)
@EnableJpaRepositories(basePackageClasses={TestWithJpaConfig.class})
public class TestWithJpaConfig {
}
