package com.renatn.netty;

import org.jboss.netty.channel.*;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static com.renatn.netty.TestProtocol.MyRequest;
import static com.renatn.netty.TestProtocol.MyResponse;

/**
 * User: renatn
 * Date: 04.08.13
 * Time: 12:58
 */
public class MyServerHandler extends SimpleChannelHandler {

    private final static Logger logger = Logger.getLogger(MyServerHandler.class.getName());

    private final static Map<String, String> users = new HashMap<String, String>();

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        MyRequest request = (MyRequest) e.getMessage();
        switch (request.getRequestType()) {
            case REGISTER:
                MyRequest.Register register = request.getRegister();
                try {
                    register(register.getUsername(), register.getPassword());
                } catch (ServiceException ex) {
                    e.getChannel().write(error(ex.getMessage()));
                    return;
                }

                e.getChannel().write(ok("User registered successfully."));
                break;
            case LOGIN:
                MyRequest.Login login = request.getLogin();

                String sessionId;
                try {
                    sessionId = login(login.getUsername(), login.getPassword());
                } catch (ServiceException ex) {
                    e.getChannel().write(error(ex.getMessage()));
                    return;
                }

                MyResponse ok = ok("User successfully login.", sessionId);
                e.getChannel().write(ok);
                break;

            default:
                e.getChannel().write(error("Unsupported request type."));
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        e.getCause().printStackTrace();

        Channel channel = e.getChannel();
        channel.close();

    }

    private MyResponse ok(String message) {
        MyResponse.Builder builder = MyResponse.newBuilder();
        builder.setStatus(MyResponse.StatusCode.OK).setResponse(message);
       return builder.build();
    }

    private MyResponse ok(String message, String sessionId) {
        MyResponse.Builder builder = MyResponse.newBuilder();
        builder.setStatus(MyResponse.StatusCode.OK).setResponse(message).setSessionId(sessionId);
        return builder.build();
    }

    private MyResponse error(String message) {
        MyResponse.Builder errorBuilder = MyResponse.newBuilder();
        errorBuilder.setStatus(MyResponse.StatusCode.ERROR).setResponse(message);
        return errorBuilder.build();
    }


    private void register(String username, String password) throws ServiceException {
        String user = users.get(username);
        if (user != null) {
            throw new ServiceException("User already registered");
        }

        users.put(username, password);
    }

    private String login(String username, String password) throws ServiceException {
        String hash = users.get(username);
        if (hash == null || !hash.equals(password) ) {
            throw new ServiceException("Invalid username or password");
        }

        // TODO insert to session
        return "42";

    }

}

