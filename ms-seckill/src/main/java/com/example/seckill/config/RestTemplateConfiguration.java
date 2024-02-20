package com.example.seckill.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Configuration
public class RestTemplateConfiguration {
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        // MappingJackson2HttpMessageConverter是Spring提供的一个消息转换器，用于处理JSON数据的序列化和反序列化。
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        // 默认情况下，MappingJackson2HttpMessageConverter支持application/json和其他JSON相关的媒体类型。
        // 在这里，通过调用setSupportedMediaTypes方法并传入MediaType.TEXT_PLAIN，修改了它的支持类型，使其能够处理text/plain类型的响应。
        // 这在某些情况下非常有用，比如当你调用的服务以纯文本形式返回JSON字符串时。
        converter.setSupportedMediaTypes(Collections.singletonList(MediaType.TEXT_PLAIN));
        // 最后，这个自定义的消息转换器被添加到RestTemplate的消息转换器列表中，这样RestTemplate就能使用它来处理响应了。
        restTemplate.getMessageConverters().add(converter);
        return restTemplate;
    }
}
