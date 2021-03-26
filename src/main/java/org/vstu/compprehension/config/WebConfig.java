package org.vstu.compprehension.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.script.ScriptTemplateConfigurer;
import org.springframework.web.servlet.view.script.ScriptTemplateViewResolver;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void configureViewResolvers(ViewResolverRegistry viewResolverRegistry) {
        viewResolverRegistry.scriptTemplate().prefix("/templates/").suffix(".html");
    }


    @Bean
    public ScriptTemplateViewResolver viewResolver() {
        ScriptTemplateViewResolver viewResolver = new ScriptTemplateViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");
        return viewResolver;
    }


    @Bean
    public ScriptTemplateConfigurer scriptTemplateConfigurer() {
        ScriptTemplateConfigurer scriptTemplateConfigurer = new ScriptTemplateConfigurer("nashorn");
        scriptTemplateConfigurer.setScripts("/js/polyfill.js", "/static/js/bundle-server.js");
        scriptTemplateConfigurer.setRenderFunction("render");
        scriptTemplateConfigurer.setSharedEngine(false);
        return scriptTemplateConfigurer;
    }
}
