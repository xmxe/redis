package com.wdhcr.socket.controller;

import com.wdhcr.socket.component.WebSocketServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

@RestController
public class SocketController {


    @Autowired
    private WebSocketServer webSocketServer;


    @GetMapping("/socket/{id}")
    public String socket(@PathVariable("id") String id, HttpSession session) {
        String s = "测试测试";
        webSocketServer.sendMessage(id, s);
        return s;
    }

}
