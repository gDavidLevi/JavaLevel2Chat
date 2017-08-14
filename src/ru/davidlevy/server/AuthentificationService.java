package ru.davidlevy.server;

import java.util.ArrayList;

public interface AuthentificationService {
    void start();
    void stop();
    String getUser(String login, String password);
    String getUser(String user);
    String getPassword(String login);
    String getLogin(String login);
    boolean registration(String login, String password, String name);
    boolean changeUserName(String oldName, String newName);
    boolean saveToHistory(String datetime, String sender, String receiver, String type, String message);
    ArrayList<String> getHistory(String sender, int day);
}
