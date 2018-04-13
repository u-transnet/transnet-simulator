package com.github.utransnet.simulator.externalapi.graphenej;

import com.github.utransnet.simulator.externalapi.*;
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
    MessageHub messageHub() {
        return new MessageHub();
    }

    @Bean
    @Scope("singleton")
    PrivateKeysSharedPool privateKeysSharedPool() {
        return new PrivateKeysSharedPool();
    }

    @Bean
    @Scope("singleton")
    @Autowired
    APIObjectFactory apiObjectFactory(ApplicationContext context) {
        return new APIObjectFactoryGraphene(context);
    }

    @Bean
    @Scope("singleton")
    @Autowired
    OperationConverter operationConverter(
            APIObjectFactory apiObjectFactory,
            DefaultAssets defaultAssets,
            PrivateKeysSharedPool privateKeysSharedPool
    ) {
        return new OperationConverter(apiObjectFactory, defaultAssets, privateKeysSharedPool);
    }

    @Bean
    @Scope("singleton")
    @Autowired
    ExternalAPI externalAPI(
            ApplicationContext context,
            APIObjectFactory apiObjectFactory,
            DefaultAssets defaultAssets,
            OperationConverter operationConverter,
            MessageHub messageHub
    ) {
        return new ExternalAPIGraphene(
                context,
                apiObjectFactory,
                defaultAssets,
                operationConverter,
                messageHub
        );
    }

    @Bean
    @Scope("prototype")
    @Autowired
    Asset asset(ExternalAPI externalAPI) {
        return new AssetGraphene((ExternalAPIGraphene) externalAPI);
    }

    @Bean
    @Scope("prototype")
    @Autowired
    UserAccount userAccount(ExternalAPI externalAPI, PrivateKeysSharedPool privateKeysSharedPool) {
        return new UserAccountGraphene(externalAPI, privateKeysSharedPool);
    }
}
