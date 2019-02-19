package com.cdut.es_web.config;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by Horizon
 * Time: 上午9:03 2018/3/10
 * Description:
 */
@Configuration
public class MyConfig {

    @Bean
    public TransportClient client() throws UnknownHostException {

        InetSocketTransportAddress node = new InetSocketTransportAddress(InetAddress.getByName("hadoop00"), 9300);

        Settings setting = Settings.builder().put("cluster.name", "horizon").build();
        TransportClient client = new PreBuiltTransportClient(setting);
        client.addTransportAddress(node);

        return client;
    }

}
