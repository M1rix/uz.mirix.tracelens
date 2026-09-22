package uz.mirix.tracelens;

import jakarta.servlet.DispatcherType;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.client.RestClient;
import uz.mirix.tracelens.internal.LoggingTraceReporter;
import uz.mirix.tracelens.internal.TraceLensClientHttpRequestInterceptor;
import uz.mirix.tracelens.internal.TraceLensDataSourceBeanPostProcessor;
import uz.mirix.tracelens.internal.TraceLensReactiveWebFilter;
import uz.mirix.tracelens.internal.TraceLensRedisConnectionFactoryBeanPostProcessor;
import uz.mirix.tracelens.internal.TraceLensRestTemplateBeanPostProcessor;
import uz.mirix.tracelens.internal.TraceLensServletFilter;
import uz.mirix.tracelens.internal.TraceLensWebClientCustomizer;
import uz.mirix.tracelens.internal.TraceLifecycle;

import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties(TraceLensProperties.class)
@ConditionalOnProperty(prefix = "tracelens", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TraceLensAutoConfiguration {

    @Bean
    public LoggingTraceReporter traceLensLoggingReporter(TraceLensProperties properties) {
        return new LoggingTraceReporter(properties);
    }

    @Bean
    public TraceLifecycle traceLensLifecycle(
        TraceLensProperties properties,
        List<TraceReporter> reporters
    ) {
        return new TraceLifecycle(properties, reporters);
    }

    @Bean
    public TraceLensClientHttpRequestInterceptor traceLensClientHttpRequestInterceptor(
        TraceLensProperties properties
    ) {
        return new TraceLensClientHttpRequestInterceptor(properties);
    }

    @Bean
    public RestTemplateCustomizer traceLensRestTemplateCustomizer(
        TraceLensClientHttpRequestInterceptor interceptor,
        TraceLensProperties properties
    ) {
        return restTemplate -> {
            if (!properties.getHttpClient().isEnabled()) {
                return;
            }
            boolean present = restTemplate.getInterceptors().stream()
                .anyMatch(candidate -> candidate instanceof TraceLensClientHttpRequestInterceptor);
            if (!present) {
                restTemplate.getInterceptors().add(interceptor);
            }
        };
    }

    @Bean
    @ConditionalOnClass(RestClient.class)
    public RestClientCustomizer traceLensRestClientCustomizer(
        TraceLensClientHttpRequestInterceptor interceptor,
        TraceLensProperties properties
    ) {
        return builder -> {
            if (properties.getHttpClient().isEnabled()) {
                builder.requestInterceptor(interceptor);
            }
        };
    }

    @Bean
    public static TraceLensRestTemplateBeanPostProcessor traceLensRestTemplateBeanPostProcessor(
        TraceLensProperties properties,
        TraceLensClientHttpRequestInterceptor interceptor
    ) {
        return new TraceLensRestTemplateBeanPostProcessor(properties, interceptor);
    }

    @Bean
    @ConditionalOnClass(name = "javax.sql.DataSource")
    public static TraceLensDataSourceBeanPostProcessor traceLensDataSourceBeanPostProcessor(
        TraceLensProperties properties
    ) {
        return new TraceLensDataSourceBeanPostProcessor(properties);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    static class ServletConfiguration {

        @Bean
        FilterRegistrationBean<TraceLensServletFilter> traceLensServletFilter(
            TraceLensProperties properties,
            TraceLifecycle lifecycle
        ) {
            FilterRegistrationBean<TraceLensServletFilter> registration = new FilterRegistrationBean<>();
            registration.setFilter(new TraceLensServletFilter(properties, lifecycle));
            registration.setName("traceLensServletFilter");
            registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 40);
            registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC);
            return registration;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = {
        "org.springframework.web.reactive.function.client.WebClient",
        "reactor.core.publisher.Mono"
    })
    static class WebClientConfiguration {

        @Bean
        org.springframework.boot.web.reactive.function.client.WebClientCustomizer traceLensWebClientCustomizer(
            TraceLensProperties properties
        ) {
            return new TraceLensWebClientCustomizer(properties);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @ConditionalOnClass(name = "org.springframework.web.server.WebFilter")
    static class ReactiveServerConfiguration {

        @Bean
        org.springframework.web.server.WebFilter traceLensReactiveWebFilter(
            TraceLensProperties properties,
            TraceLifecycle lifecycle
        ) {
            return new TraceLensReactiveWebFilter(properties, lifecycle);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.data.redis.connection.RedisConnectionFactory")
    static class RedisConfiguration {

        @Bean
        static TraceLensRedisConnectionFactoryBeanPostProcessor traceLensRedisConnectionFactoryBeanPostProcessor(
            TraceLensProperties properties
        ) {
            return new TraceLensRedisConnectionFactoryBeanPostProcessor(properties);
        }
    }
}
