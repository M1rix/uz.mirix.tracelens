package uz.mirix.tracelens;

import jakarta.servlet.DispatcherType;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.client.RestClient;
import uz.mirix.tracelens.internal.*;
import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties(TraceLensProperties.class)
public class TraceLensAutoConfiguration {
    @Bean public LoggingTraceReporter traceLensLoggingReporter(TraceLensProperties p){ return new LoggingTraceReporter(p); }
    @Bean public TraceLifecycle traceLensLifecycle(TraceLensProperties p,List<TraceReporter> r){ return new TraceLifecycle(p,r); }
    @Bean public TraceLensClientHttpRequestInterceptor traceLensClientHttpRequestInterceptor(TraceLensProperties p){ return new TraceLensClientHttpRequestInterceptor(p); }
    @Bean public RestTemplateCustomizer traceLensRestTemplateCustomizer(TraceLensClientHttpRequestInterceptor i,TraceLensProperties p){ return t->{ if(!p.isEnabled()||!p.getHttpClient().isEnabled()) return; boolean present=t.getInterceptors().stream().anyMatch(e->e instanceof TraceLensClientHttpRequestInterceptor); if(!present)t.getInterceptors().add(i); }; }
    @Bean @ConditionalOnClass(RestClient.class) public RestClientCustomizer traceLensRestClientCustomizer(TraceLensClientHttpRequestInterceptor i,TraceLensProperties p){ return b->{ if(p.isEnabled()&&p.getHttpClient().isEnabled()) b.requestInterceptor(i); }; }
    @Bean public static TraceLensRestTemplateBeanPostProcessor traceLensRestTemplateBeanPostProcessor(TraceLensProperties p,TraceLensClientHttpRequestInterceptor i){ return new TraceLensRestTemplateBeanPostProcessor(p,i); }
    @Bean @ConditionalOnClass(name="javax.sql.DataSource") public static TraceLensDataSourceBeanPostProcessor traceLensDataSourceBeanPostProcessor(TraceLensProperties p){ return new TraceLensDataSourceBeanPostProcessor(p); }

    @Configuration(proxyBeanMethods=false) @ConditionalOnWebApplication(type=ConditionalOnWebApplication.Type.SERVLET)
    static class ServletConfiguration {
        @Bean FilterRegistrationBean<TraceLensServletFilter> traceLensServletFilter(TraceLensProperties p,TraceLifecycle l){ FilterRegistrationBean<TraceLensServletFilter> r=new FilterRegistrationBean<>(); r.setFilter(new TraceLensServletFilter(p,l)); r.setName("traceLensServletFilter"); r.setOrder(Ordered.HIGHEST_PRECEDENCE+40); r.setDispatcherTypes(DispatcherType.REQUEST,DispatcherType.ASYNC); return r; }
    }
    @Configuration(proxyBeanMethods=false) @ConditionalOnClass(name={"org.springframework.web.reactive.function.client.WebClient","reactor.core.publisher.Mono"})
    static class WebClientConfiguration { @Bean org.springframework.boot.web.reactive.function.client.WebClientCustomizer traceLensWebClientCustomizer(TraceLensProperties p){ return new TraceLensWebClientCustomizer(p); } }
    @Configuration(proxyBeanMethods=false) @ConditionalOnWebApplication(type=ConditionalOnWebApplication.Type.REACTIVE) @ConditionalOnClass(name="org.springframework.web.server.WebFilter")
    static class ReactiveServerConfiguration { @Bean org.springframework.web.server.WebFilter traceLensReactiveWebFilter(TraceLensProperties p,TraceLifecycle l){ return new TraceLensReactiveWebFilter(p,l); } }
    @Configuration(proxyBeanMethods=false) @ConditionalOnClass(name="org.springframework.data.redis.connection.RedisConnectionFactory")
    static class RedisConfiguration { @Bean static TraceLensRedisConnectionFactoryBeanPostProcessor traceLensRedisConnectionFactoryBeanPostProcessor(TraceLensProperties p){ return new TraceLensRedisConnectionFactoryBeanPostProcessor(p); } }
}
