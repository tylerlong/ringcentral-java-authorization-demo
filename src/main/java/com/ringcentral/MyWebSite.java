package com.ringcentral;

import com.ringcentral.definitions.TokenInfo;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.Gson;

public class MyWebSite extends AbstractHandler {
    private static String TOKEN_KEY = "rc-token";
    private static String REDIRECT_URI = "http://localhost:8080/oauth2callback";

    private Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);
        server.setHandler(new MyWebSite());
        server.start();
        server.join();
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("text/html; charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);

        Cookie[] cookiesArray = request.getCookies();
        if(cookiesArray == null) {
          cookiesArray = new Cookie[0];
        }
        List<Cookie> cookies = Arrays.asList(cookiesArray);
        List<Cookie> filteredCookies = cookies.stream().filter(c -> c.getName().equals(TOKEN_KEY)).collect(Collectors.toList());
        RestClient rc = new RestClient(
                System.getenv("RINGCENTRAL_CLIENT_ID"),
                System.getenv("RINGCENTRAL_CLIENT_SECRET"),
                System.getenv("RINGCENTRAL_SERVER_URL")
        );
        String requestUri = request.getRequestURI();
        if (filteredCookies.size() > 0) {
            String base64String = filteredCookies.get(0).getValue();
            byte[] decodedBytes = Base64.getDecoder().decode(base64String);
            String tokenString = new String(decodedBytes);
            rc.token = gson.fromJson(tokenString, TokenInfo.class);
        } else if (!requestUri.equals("/oauth2callback")) {
            response.getWriter().println("<h2>RingCentral Authorization Code Flow Authentication</h2><a href=\"" + rc.authorizeUri(REDIRECT_URI) + "\">Login RingCentral Account</a>");
            return;
        }
        System.out.println(requestUri);
        switch (requestUri) {
            case "/":
                response.getWriter().println("<b><a href=\"/logout\">Logout</a></b>\n" +
                        "                            <h2>Call APIs</h2>\n" +
                        "                            <ul>\n" +
                        "                                <li><a href=\"/test?api=extension\" target=\"_blank\">Read Extension Info</a></li>\n" +
                        "                                <li><a href=\"/test?api=extension-call-log\" target=\"_blank\">Read Extension Call Log</a></li>\n" +
                        "                                <li><a href=\"/test?api=account-call-log\" target=\"_blank\">Read Account Call Log</a></li>\n" +
                        "                            </ul>");
                break;
            case "/oauth2callback":
                String code = request.getParameter("code");
                try {
                    rc.authorize(code, REDIRECT_URI);
                } catch (RestException e) {
                    e.printStackTrace();
                }
                String base64String = Base64.getEncoder().encodeToString(gson.toJson(rc.token).getBytes());
                Cookie cookie2 = new Cookie(TOKEN_KEY, base64String);
                cookie2.setMaxAge(999999999);
                response.addCookie(cookie2);
                response.sendRedirect("/");
                break;
            case "/test":
                String api = request.getParameter("api");
                String result = "";
                try{
                    switch (api) {
                        case "extension":
                            result = gson.toJson(rc.restapi().account().extension().list());
                            break;
                        case "extension-call-log":
                            result = gson.toJson(rc.restapi().account().extension().callLog().list());
                            break;
                        case "account-call-log":
                            result = gson.toJson(rc.restapi().account().callLog().list());
                            break;
                    }
                }catch(RestException re) {

                }

                response.getWriter().println("<pre>" + result + "</pre>");
                break;
            case "/logout":
                try {
                    rc.revoke();
                } catch (RestException e) {
                    e.printStackTrace();
                }
                Cookie cookie = new Cookie(TOKEN_KEY, "");
                cookie.setMaxAge(0);
                response.addCookie(cookie);
                response.sendRedirect("/");
                break;
            default:
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                break;
        }
    }
}