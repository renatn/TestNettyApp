package com.renatn.netty;

import org.jboss.netty.channel.*;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;


import static com.renatn.netty.TestProtocol.*;
/**
 * User: renatn
 * Date: 04.08.13
 * Time: 15:47
 */
public class MyClientHandler extends SimpleChannelUpstreamHandler {

    private final static Logger logger = Logger.getLogger(MyClientHandler.class.getName());

    private final BlockingQueue<MyResponse> answer = new LinkedBlockingQueue<MyResponse>();

    private volatile Channel channel;

    public String login(String username, String password) {
        MyRequest.Login.Builder loginBuilder = MyRequest.Login.newBuilder();
        loginBuilder.setUsername(username).setPassword(password);

        MyRequest.Builder request = MyRequest.newBuilder();
        request.setRequestType(RequestType.LOGIN);
        request.setLogin(loginBuilder.build());

        MyResponse response = sendRequest(request.build());
        return response.getSessionId();
    }

    public String register(String userName, String password) {

        MyRequest.Register.Builder registerBuilder =  MyRequest.Register.newBuilder();
        registerBuilder.setUsername(userName).setPassword(password);

        MyRequest.Builder request = MyRequest.newBuilder();
        request.setRequestType(RequestType.REGISTER);
        request.setRegister(registerBuilder.build());

        return sendRequest(request.build()).getResponse();
    }

    public String time(String sessionId) {
        MyRequest.Builder request = MyRequest.newBuilder();
        request.setRequestType(RequestType.TIME);
        request.setSessionId(sessionId);
        return sendRequest(request.build()).getResponse();
    }

    public String quit(String sessionId) {
        MyRequest.Builder request = MyRequest.newBuilder();
        request.setRequestType(RequestType.QUIT);
        request.setSessionId(sessionId);
        return sendRequest(request.build()).getResponse();
    }

    private MyResponse sendRequest(Object request) {
        channel.write(request);

        MyResponse response;
        boolean interrupted = false;
        while (true) {
            try {
                logger.info("Waiting response...");
                response = answer.take();
                break;
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }

        if (interrupted) {
            Thread.currentThread().interrupt();
        }

        return response;

    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        logger.info("Client channel opened...");
        channel = e.getChannel();
        super.channelOpen(ctx, e);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        boolean offer = answer.offer((MyResponse) e.getMessage());
        if (!offer) {
            logger.warning("Cannot add message to queue");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        logger.warning(e.getCause().toString());
        e.getChannel().close();
    }


}
