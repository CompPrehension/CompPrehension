package org.vstu.compprehension.config;

import domains.DataFlowDTDomain;
import domains.ObjectsScopeDTDomain;
import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.vstu.compprehension.Service.UserService;
import org.vstu.compprehension.config.interceptors.RandomSeedSetInterceptor;
import org.vstu.compprehension.config.logs.LoggableDispatcherServlet;
import org.vstu.compprehension.models.businesslogic.domains.ControlFlowStatementsDTDomain;
import org.vstu.compprehension.models.businesslogic.domains.ControlFlowStatementsDomain;
import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDTDomain;
import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDomain;

import java.util.Locale;

@Configuration @EnableAsync
public class WebConfig implements WebMvcConfigurer {

    /**
     * Optional plain HTTP connector for server-to-server endpoints (e.g. JWKS fetch by Moodle in dev).
     * Set server.http.port in application.properties to enable (e.g. 8090).
     * Leave unset or -1 in production — use only HTTPS there.
     */
    @Value("${server.http.port:-1}")
    private int httpPort;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> httpConnectorCustomizer() {
        return factory -> {
            if (httpPort > 0) {
                var connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
                connector.setPort(httpPort);
                connector.setScheme("http");
                connector.setSecure(false);
                factory.addAdditionalTomcatConnectors(connector);
            }
        };
    }

    @Bean
    public ServletRegistrationBean dispatcherRegistration(@Autowired UserService userService) {
        return new ServletRegistrationBean(dispatcherServlet(userService));
    }

    @Bean(name = DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)
    public DispatcherServlet dispatcherServlet(@Autowired UserService userService) {
        return new LoggableDispatcherServlet(userService);
    }

    @Bean
    public LocaleResolver localeResolver() {
        final CookieLocaleResolver cookieLocaleResolver = new CookieLocaleResolver("JLOCALE");
        cookieLocaleResolver.setDefaultLocale(Locale.ENGLISH);
        cookieLocaleResolver.setCookieSecure(true);
        return cookieLocaleResolver;
    }

    @Bean(name = "messageSource")
    public MessageSource getMessageSource() {
        var messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.addBasenames("classpath:/messages/common-messages");
        messageSource.addBasenames(ControlFlowStatementsDomain.MESSAGES_CONFIG_PATH);
        messageSource.addBasenames(ProgrammingLanguageExpressionDomain.MESSAGES_CONFIG_PATH);
        messageSource.addBasenames(ProgrammingLanguageExpressionDTDomain.MESSAGES_CONFIG_PATH);
        messageSource.addBasenames(ControlFlowStatementsDTDomain.MESSAGES_CONFIG_PATH);
        messageSource.addBasenames(ObjectsScopeDTDomain.MESSAGES_CONFIG_PATH);
        messageSource.addBasenames(DataFlowDTDomain.MESSAGES_CONFIG_PATH);
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }


    @Bean
    public LocaleChangeInterceptor localeInterceptor() {
        LocaleChangeInterceptor localeInterceptor = new LocaleChangeInterceptor();
        localeInterceptor.setParamName("lang");
        return localeInterceptor;
    }

    @Autowired
    private RandomSeedSetInterceptor randomSeedSetInterceptor;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(randomSeedSetInterceptor);
        registry.addInterceptor(localeInterceptor());
    }
}
