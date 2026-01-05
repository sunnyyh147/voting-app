// voting-client/src/main/java/com/example/voting/client/model/Session.java
package com.example.voting.client.model;

public class Session {
    public String baseUrl;
    public String token;
    public String username;

    public Session(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean isLoggedIn() {
        return token != null && !token.isBlank();
    }
}
