package com.ferit.temp_reader.crypto;

import android.os.Build;
import android.os.Message;
import android.support.annotation.RequiresApi;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class SHA256Encryptor {
    private final String saltString;

    public SHA256Encryptor(){
        this.saltString = "4739dgksl939JD39";
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public String encryptMessage(String message) throws NoSuchAlgorithmException {
        byte[] saltAsByteArray = saltString.getBytes();
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(saltAsByteArray);
        byte[] hashedPassword = md.digest(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hashedPassword);
    }
}
