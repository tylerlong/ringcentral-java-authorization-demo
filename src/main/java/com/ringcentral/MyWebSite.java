package com.ringcentral;

import com.ringcentral.definitions.TokenInfo;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MyWebSite extends AbstractHandler {
    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);
        MyWebSite myWebSite = new MyWebSite();
        server.setHandler(new MyWebSite());
        server.start();
        server.join();
    }

    private static String TOKEN_KEY = "rc-token";
    private static String REDIRECT_URI = "http://localhost:8080/oauth2callback";

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("text/html; charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);

        Cookie[] cookiesArray = request.getCookies();
        List<Cookie> cookies = Arrays.asList(cookiesArray);
        List<Cookie> filteredCookies = cookies.stream().filter(c -> c.getName() == TOKEN_KEY).collect(Collectors.toList());
        RestClient rc = new RestClient(
                System.getenv("RINGCENTRAL_CLIENT_ID"),
                System.getenv("RINGCENTRAL_CLIENT_SECRET"),
                System.getenv("RINGCENTRAL_SERVER_URL")
        );
        if (filteredCookies.size() > 0) {
            String tokenString = filteredCookies.get(0).getValue();
            rc.token = com.alibaba.fastjson.JSON.parseObject(tokenString, TokenInfo.class);
        } else if (request.getRequestURI() != "/oauth2callback") {
            response.getWriter().println("<h2>RingCentral Authorization Code Flow Authentication</h2><a href=\"" + rc.authorizeUri(REDIRECT_URI) + "\">Login RingCentral Account</a>");
        }

//        System.out.println(request.getRequestURI());
//        response.getWriter().println("OK");
        baseRequest.setHandled(true);
    }
}