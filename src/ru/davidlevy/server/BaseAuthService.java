package ru.davidlevy.server;

import java.util.ArrayList;

public class BaseAuthService implements AuthentificationService {
    private class Entry {
        private String login;
        private String password;
        private String user;
        private String email;

        Entry(String login, String password, String user, String email) {
            this.login = login;
            this.password = password;
            this.user = user;
            this.email = email;
        }

        public String show() {
            return String.format("%s %s %s %s", login, password, user, email);
        }
    }

    private ArrayList<Entry> entries;

    public BaseAuthService() {
        this.entries = new ArrayList<>();
        createTestEntries();
    }

    @Override
    public boolean changeUserName(String oldName, String newName) {
        return false;
    }

    @Override
    public boolean saveToHistory(String datetime, String sender, String receiver, String type, String message) {
        return false;
    }

    @Override
    public ArrayList<String> getHistory(String sender, int day) {
        return null;
    }

    private void createTestEntries() {
        for (int i = 0; i < 4; i++) {
            this.entries.add(new Entry("l" + i, "p" + i, "user" + i, "user" + i + "@mail.com"));
        }
        for (Entry rec : entries) {
            System.out.println(rec.show());
        }
    }

    @Override
    public void start() {
        System.out.println("BaseAuthService run");
    }

    @Override
    public void stop() {
        System.out.println("BaseAuthService stop");

    }

    @Override
    public synchronized String getUser(String login, String password) {
        for (Entry rec : entries) {
            if (rec.login.equals(login) && rec.password.equals(password)) {
                return rec.user;
            }
        }
        return null;
    }

    @Override
    public String getUser(String user) {
        return null;
    }

    @Override
    public String getPassword(String login) {
        return null;
    }

    @Override
    public String getLogin(String login) {
        return null;
    }

    @Override
    public boolean registration(String login, String password, String name) {
        return false;
    }
}