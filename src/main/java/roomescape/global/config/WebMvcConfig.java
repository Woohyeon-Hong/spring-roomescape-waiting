package roomescape.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import roomescape.auth.LoginNameArgumentResolver;
import roomescape.auth.NameAuthenticationInterceptor;
import roomescape.global.exception.support.BusinessExceptionMappingJackson2HttpMessageConverter;
import roomescape.rateLimit.RateLimitInterceptor;
import roomescape.rateLimit.TokenBucketRateLimiter;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ObjectMapper objectMapper;
    private final RateLimitInterceptor rateLimitInterceptor;

    public WebMvcConfig(
            ObjectMapper objectMapper,
            @Value("${rate-limit.capacity}") long capacity,
            @Value("${rate-limit.refill-per-second}") double refillPerSec
    ) {
        this.objectMapper = objectMapper;
        this.rateLimitInterceptor = new RateLimitInterceptor(
                new TokenBucketRateLimiter(capacity, refillPerSec, System::nanoTime)
        );
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.addFirst(new BusinessExceptionMappingJackson2HttpMessageConverter(objectMapper));
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/reservations/**", "/reservation-waitings/**", "/payments/**");

        registry.addInterceptor(new NameAuthenticationInterceptor());
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.addFirst(new LoginNameArgumentResolver());
    }
}
