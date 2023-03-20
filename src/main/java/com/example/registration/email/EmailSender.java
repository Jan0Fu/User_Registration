package com.example.registration.email;

public interface EmailSender {

    void send(String to, String email);

    String buildEmail(String getfirstName, String link);
}
