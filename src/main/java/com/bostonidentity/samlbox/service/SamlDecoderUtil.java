package com.bostonidentity.samlbox.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class SamlDecoderUtil {

    public static String decodeSaml(String input) throws Exception {
        if (input.startsWith("SAMLRequest=") || input.startsWith("SAMLResponse=")) {
            return decodeHttpRedirectBinding(input);
        }
        return decodeBase64(input);
    }

    private static String decodeHttpRedirectBinding(String input) throws Exception {
        String[] parts = input.split("=");
        String encoded = URLDecoder.decode(parts[1], StandardCharsets.UTF_8.name());
        byte[] decoded = Base64.getDecoder().decode(encoded);
        byte[] inflated = inflate(decoded);
        return new String(inflated, StandardCharsets.UTF_8);
    }

    private static String decodeBase64(String input) {
        byte[] decoded = Base64.getDecoder().decode(input);
        return new String(decoded, StandardCharsets.UTF_8);
    }

    private static byte[] inflate(byte[] input) throws DataFormatException, IOException {
        Inflater inflater = new Inflater(true);
        inflater.setInput(input);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(input.length);
        byte[] buffer = new byte[1024];

        while (!inflater.finished()) {
            int count = inflater.inflate(buffer);
            outputStream.write(buffer, 0, count);
        }
        outputStream.close();
        return outputStream.toByteArray();
    }
}
