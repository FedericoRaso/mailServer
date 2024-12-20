package com.example.mailserver.model;

/**
 *
 * protocols used in data transfer with the server
 *
 */
public enum Protocol {
    LOGIN,
    LOGOUT,
    SEND,
    DELETE,
    FORWARD,
    REPLYALL,
    REPLY,
    REFRESH
}
