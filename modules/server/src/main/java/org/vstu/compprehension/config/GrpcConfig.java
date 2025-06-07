package org.vstu.compprehension.config;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NegotiationType;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.vstu.compprehension.bkt.grpc.BktServiceGrpc;

@Configuration
public class GrpcConfig {

    @Bean
    public ManagedChannel bktChannel(
            @Value("${bkt.host:localhost}") String host,
            @Value("${bkt.port:50051}") int port
    ) {
        return NettyChannelBuilder.forAddress(host, port)
                .negotiationType(NegotiationType.PLAINTEXT)
                .maxInboundMessageSize(4 * 1024 * 1024)
                .build();
    }

    @Bean
    public BktServiceGrpc.BktServiceBlockingStub bktStub(ManagedChannel ch) {
        return BktServiceGrpc.newBlockingStub(ch);
    }
}
