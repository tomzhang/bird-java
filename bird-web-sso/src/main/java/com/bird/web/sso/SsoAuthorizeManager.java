package com.bird.web.sso;

import com.bird.core.Check;
import com.bird.web.common.utils.CookieHelper;
import com.bird.web.sso.client.ClientInfo;
import com.bird.web.sso.client.IUserClientStore;
import com.bird.web.sso.permission.IUserPermissionChecker;
import com.bird.web.sso.ticket.TicketInfo;
import com.bird.web.sso.ticket.ITicketProtector;
import com.bird.web.sso.ticket.ITicketSessionStore;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by liuxx on 2017/5/18.
 */
public class SsoAuthorizeManager {
    private String cookieName;
    private Integer expire = 60; //单位：分

    /**
     * 票据加密器
     */
    @Inject
    private ITicketProtector protector;

    /**
     * 票据信息存储器
     */
    @Inject
    private ITicketSessionStore sessionStore;

    /**
     * 登录，将token写入cookie
     * @param ticketInfo 票据信息
     * @return token
     */
    public String login(HttpServletResponse response,TicketInfo ticketInfo) {
        if (ticketInfo.getExpireTime() == null) {
            Date creationTime = ticketInfo.getCreationTime();
            long expire = creationTime.getTime() + this.expire * 60 * 1000L;
            ticketInfo.setExpireTime(new Date(expire));
        }

        if (sessionStore != null) {
            return sessionStore.storeTicket(ticketInfo);
        }
        String token = protector.protect(ticketInfo);

        //用户中心写入Cookie
        Cookie cookie = new Cookie(this.cookieName, token.trim());
        cookie.setMaxAge(this.expire * 60);// 设置为有效期
        cookie.setPath("/");
        response.addCookie(cookie);

        return token;
    }

    /**
     * 注销
     * 1、清除SessionStore；2、清除Cookie
     * @return
     */
    public void logout(HttpServletRequest request,HttpServletResponse response) {
        String token = "";
        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(this.cookieName)) {
                token = cookie.getValue();
                break;
            }
        }

        if (!StringUtils.isEmpty(token)) {
            //清除SessionStore
            if (sessionStore != null) {
                sessionStore.removeTicket(token);
            }

            //清除Cookie
            Cookie cookie = new Cookie(this.cookieName, "");
            cookie.setMaxAge(0);// 设置有效期为0
            cookie.setPath("/");
            response.addCookie(cookie);
        }
    }

    /**
     * 刷新票据信息
     * @param request
     * @param ticketInfo
     */
    public void refreshToken(HttpServletRequest request, TicketInfo ticketInfo) {
        Check.NotNull(ticketInfo, "ticketInfo");

        //先从header中获取token
        String token = request.getHeader(this.cookieName);
        if (StringUtils.isBlank(token)) {
            //header中没有token,则从cookie中获取
            Cookie[] cookies = request.getCookies();
            if (cookies == null) return;
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(this.cookieName)) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        sessionStore.refreshTicket(token, ticketInfo, this.expire * 60 * 1000);
    }


    public String getCookieName() {
        return cookieName;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }

    public ITicketProtector getProtector() {
        return protector;
    }

    public void setProtector(ITicketProtector protector) {
        this.protector = protector;
    }

    public ITicketSessionStore getSessionStore() {
        return sessionStore;
    }

    public void setSessionStore(ITicketSessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    public Integer getExpire() {
        return expire;
    }

    public void setExpire(Integer expire) {
        this.expire = expire;
    }
}