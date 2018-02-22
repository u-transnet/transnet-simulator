package com.github.utransnet.simulator.externalapi.h2impl;

import com.github.utransnet.simulator.externalapi.APIObjectFactory;
import com.github.utransnet.simulator.externalapi.ExternalAPI;
import com.github.utransnet.simulator.externalapi.UserAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Created by Artem on 16.02.2018.
 */
@Configuration
@EntityScan(basePackageClasses = {ExternalAPIH2ImplConfig.class, Jsr310JpaConverters.class})
@EnableJpaRepositories(basePackageClasses={ExternalAPIH2ImplConfig.class})
public class ExternalAPIH2ImplConfig {

    @Bean
    @Scope("singleton")
    @Autowired
    APIObjectFactory apiObjectFactory(ApplicationContext context){
        return new APIObjectFactoryH2(context);
    }

    @Bean
    @Scope("singleton")
    @Autowired
    ExternalAPI externalAPI(
            TransferOperationH2Repository transferOperationRepository,
            APIObjectFactory apiObjectFactory,
            MessageOperationRepository messageOperationRepository,
            ProposalH2Repository proposalRepository
    ) {
        return new ExternalAPIH2(
                transferOperationRepository,
                (APIObjectFactoryH2) apiObjectFactory,
                messageOperationRepository,
                proposalRepository
        );
    }

    @Bean
    @Scope("prototype")
    @Autowired
    UserAccount userAccount(ExternalAPI externalAPI) {
        return new UserAccountH2(externalAPI);
    }
}
