// voting-client/src/main/java/com/example/voting/client/util/ClientJson.java
package com.example.voting.client.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class ClientJson {
    public static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private ClientJson() {}
}
