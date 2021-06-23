package com.atguigu.gmall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter(){

        CorsConfiguration corsConfiguration = new CorsConfiguration(); //初始化Cors配置对象
        corsConfiguration.addAllowedOrigin("http://manager.gmall.com");// 允许域
        corsConfiguration.addAllowedOrigin("http://localhost:1000");
        corsConfiguration.addAllowedHeader("*");  // 允许头信息
        corsConfiguration.addAllowedMethod("*"); //允许请求头方式
        corsConfiguration.setAllowCredentials(true);//设置允许携带cookie
        //添加映射路径 拦截所有请求
        UrlBasedCorsConfigurationSource configurationSource = new UrlBasedCorsConfigurationSource();
        configurationSource.registerCorsConfiguration("/**",corsConfiguration);
        return new CorsWebFilter(configurationSource);
    }
}
