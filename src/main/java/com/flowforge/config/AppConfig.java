package com.flowforge.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowforge.cache.PrecomputeCache;
import com.flowforge.engine.agent.LlmPlanner;
import com.flowforge.engine.agent.ProposalVerifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(FlowForgeProperties.class)
public class AppConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOrigins("*").allowedMethods("GET", "POST");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
    }

    @Bean
    @ConditionalOnProperty(prefix = "flowforge.redis", name = "enabled", havingValue = "true")
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public PrecomputeCache precomputeCache(
            FlowForgeProperties properties,
            ObjectMapper mapper,
            @org.springframework.beans.factory.annotation.Autowired(required = false) StringRedisTemplate redis
    ) {
        return new PrecomputeCache(
                redis,
                mapper,
                properties.getRedis().isEnabled(),
                properties.getRedis().getKeyPrefix(),
                properties.getRedis().getTtlSeconds()
        );
    }

    @Bean
    public LlmPlanner llmPlanner(FlowForgeProperties properties, ObjectMapper mapper) {
        return new LlmPlanner(
                properties.getAgent().getOpenaiApiKey(),
                properties.getAgent().getOpenaiBaseUrl(),
                properties.getAgent().getModel(),
                mapper
        );
    }

    @Bean
    public ProposalVerifier proposalVerifier(FlowForgeProperties properties) {
        return new ProposalVerifier(
                properties.getAgent().getMaxFillDropPp(),
                properties.getAgent().getMaxCostIncreasePct()
        );
    }
}
