package org.acme.domain.user;

import java.util.UUID;

public class User {
    UUID ID;
    String name;
    String username;
    String password;

    public User(UUID ID, String name, String username, String password) {
        this.ID = ID;
        this.name = name;
        this.username = username;
        this.password = password;
    }

}

