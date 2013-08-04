package com.renatn.netty;

import org.jboss.netty.channel.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static com.renatn.netty.TestProtocol.MyRequest;
import static com.renatn.netty.TestProtocol.MyResponse;

/**
 * User: renatn
 * Date: 04.08.13
 * Time: 12:58
 */
public class MyServerHandler extends SimpleChannelHandler {

    private final static Map<String, String> users = new HashMap<String, String>();
    private final static Map<String, String> session = new HashMap<String, String>();

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

                e.getChannel().write(ok("User successfully login.", sessionId));
                break;
            case QUIT:

                try {
                    logout(request.getSessionId());
                } catch (ServiceException ex) {
                    e.getChannel().write(error(ex.getMessage()));
                    return;
                }
                e.getChannel().write(ok("Logout successfully."));
                break;
            case TIME:
                String user = session.get(request.getSessionId());
                if ( user == null) {
                    e.getChannel().write(error("Unauthorized access"));
                    return;
                }

                e.getChannel().write(ok((new Date()).toString(), request.getSessionId()));
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

        String sessionId = UUID.randomUUID().toString();
        session.put(sessionId, username) ;
        return sessionId;

    }

    private void logout(String sessionId)  throws ServiceException {
        String user = session.get(sessionId);
        if (user == null) {
            throw new ServiceException("Unauthorized access");
        }
        session.remove(sessionId);
    }

}

