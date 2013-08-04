package com.renatn.netty;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * User: renatn
 * Date: 04.08.13
 * Time: 14:50
 */

public class MyServerTest {

    private ClientBootstrap bootstrap;
    private Channel channel;

    @Before
    public void setUp() {
        bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool()
        ));

        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline() throws Exception {
                ChannelPipeline pipeline = Channels.pipeline();
                pipeline.addLast("frameDecoder", new ProtobufVarint32FrameDecoder());
                pipeline.addLast("protobufDecoder", new ProtobufDecoder(TestProtocol.MyResponse.getDefaultInstance()));
                pipeline.addLast("frameEncoder", new ProtobufVarint32LengthFieldPrepender());
                pipeline.addLast("protobufEncoder", new ProtobufEncoder());
                pipeline.addLast("handler", new MyClientHandler());
                return pipeline;
            }
        });

        ChannelFuture connectFuture = bootstrap.connect(new InetSocketAddress(8080));
        channel = connectFuture.awaitUninterruptibly().getChannel();
    }

    @After
    public void tearDown() {
        channel.close().awaitUninterruptibly();
        bootstrap.releaseExternalResources();
    }

    @Test
    public void shouldRegister() throws Exception {
        MyClientHandler handler = channel.getPipeline().get(MyClientHandler.class);
        String response = handler.register("foo", "bar");
        System.out.println("Response: " + response);
    }

    @Test
    public void shouldLogin() throws Exception {
        MyClientHandler handler = channel.getPipeline().get(MyClientHandler.class);
        String sessionId = handler.login("foo", "bar");
        System.out.println("Session ID: " + sessionId);
    }

    @Test
    public void shouldGetTime() throws Exception {
        MyClientHandler handler = channel.getPipeline().get(MyClientHandler.class);
        String sessionId = handler.login("foo", "bar");
        System.out.println("Login successfully. Requesting current time....");
        String time = handler.time(sessionId);
        System.out.println("Now: " + time);
    }

    @Test
    public void shouldNotGetTime() throws Exception {
        MyClientHandler handler = channel.getPipeline().get(MyClientHandler.class);
        String time = handler.time("abcd");
        System.out.println("Now: " + time);
    }

    @Test
    public void shouldLogout() throws Exception {
        MyClientHandler handler = channel.getPipeline().get(MyClientHandler.class);
        String sessionId = handler.login("foo", "bar");
        System.out.println("Login successfully. Doing logout....");
        String response = handler.quit(sessionId);
        System.out.println("Result: " + response);
    }

    @Test
    public void shouldNotLogout() throws Exception {
        MyClientHandler handler = channel.getPipeline().get(MyClientHandler.class);
        String response = handler.quit("qwertyuion");
        System.out.println("Result: " + response);
    }

}
