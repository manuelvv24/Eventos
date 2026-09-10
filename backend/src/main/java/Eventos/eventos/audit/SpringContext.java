package Eventos.eventos.audit;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.context.ApplicationEventPublisher; 

@Component
public class SpringContext implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext ac) {
        context = ac;
    }

    public static <T> T get(Class<T> bean) {
        return context.getBean(bean);
    }

    public static ApplicationEventPublisher getEventPublisher() {
        return context;
    }
}