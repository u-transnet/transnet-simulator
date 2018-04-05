package com.github.utransnet.simulator.externalapi.graphenej;

import com.github.utransnet.simulator.externalapi.APIObjectFactory;
import com.github.utransnet.simulator.externalapi.DefaultAssets;
import com.github.utransnet.simulator.externalapi.ExternalAPI;
import com.github.utransnet.simulator.externalapi.UserAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;

/**
 * Created by Artem on 02.04.2018.
 */
@Configuration
@Profile("graphene")
public class ExternalAPIGrapheneConfig {

    @Bean
    @Scope("singleton")
    @Autowired
    APIObjectFactory apiObjectFactory(ApplicationContext context) {
        return new APIObjectFactoryGraphene(context);
    }

    @Bean
    @Scope("singleton")
    @Autowired
    DefaultAssets defaultAssets(APIObjectFactory apiObjectFactory) {
        return new DefaultAssets(apiObjectFactory);
    }

    @Bean
    @Scope("singleton")
    @Autowired
    OperationConverter operationConverter(APIObjectFactory apiObjectFactory, DefaultAssets defaultAssets) {
        return new OperationConverter(apiObjectFactory, defaultAssets);
    }

    @Bean
    @Scope("singleton")
    @Autowired
    ExternalAPI externalAPI(
            APIObjectFactory apiObjectFactory,
            DefaultAssets defaultAssets,
            OperationConverter operationConverter
    ) {
        return new ExternalAPIGraphene(
                apiObjectFactory,
                defaultAssets,
                operationConverter
        );
    }

    @Bean
    @Scope("prototype")
    @Autowired
    UserAccount userAccount(ExternalAPI externalAPI) {
        return new UserAccountGraphene(externalAPI);
    }
}
