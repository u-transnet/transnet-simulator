package com.github.utransnet.simulator;

/**
 * Created by Artem on 09.02.2018.
 */

import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;

public abstract class SpringTest<T> {
    private ApplicationContext context;

@Before
    public void initSpringTest() {
        context = new AnnotationConfigApplicationContext(getRootConfig());
        context.getAutowireCapableBeanFactory().autowireBean(this);
        MockitoAnnotations.initMocks(this);
        Mockito.validateMockitoUsage();
    }

    public abstract Class<T> getRootConfig();
}