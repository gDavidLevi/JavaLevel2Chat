package ru.davidlevy.server;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {
    private Controller controller;
    private ArrayList<ClientHandler> registeredUsersOnline;
    private ObservableList<String> items;
    private AuthentificationService authentificationService;

    public Server(Controller controller) {
        this.controller = controller;
        this.registeredUsersOnline = new ArrayList<>();
        daemon();
    }

    private void daemon() {
        Thread daemon = new Thread(() -> {
            try (ServerSocket server = new ServerSocket(controller.settings.getPort())) {
                this.authentificationService = new SQLiteService();   // = new BaseAuthService();
                this.authentificationService.start();
                while (true) {
                    Socket socket = server.accept();
                    new ClientHandler(this, socket, this.controller);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                this.authentificationService.stop();
            }
        });
        daemon.setDaemon(true);
        daemon.start();
    }

    public boolean isUserConnected(String user) {
        for (ClientHandler client : registeredUsersOnline) if (client.getNickname().equals(user)) return true;
        return false;
    }

    public AuthentificationService getAuthentificationService() {
        return authentificationService;
    }

    private synchronized void showUsersOnline() {
        ArrayList<String> list = new ArrayList<String>();
        for (ClientHandler user : registeredUsersOnline) list.add(user.getNickname());
        items = FXCollections.observableArrayList(list);
        controller.listView.setItems(items);
        controller.listView.refresh();
    }

    public synchronized String listWhoisOnline() {
        String result = "";
        for (ClientHandler client : registeredUsersOnline) result += client.getNickname() + "|";
        return result;
    }

    public synchronized void addClient(ClientHandler obj) throws IOException {
        registeredUsersOnline.add(obj);
        Platform.runLater(() -> showUsersOnline());
        obj.sendService("/authok|" + obj.getNickname());
        //
        String list = listWhoisOnline();
        for (ClientHandler user : registeredUsersOnline) user.sendService("/online|" + list);
        broadcastMsg("The user " + obj.getNickname() + " has joined the chat.");
    }

    public synchronized void changeNameClient(ClientHandler obj, String newName, String oldName) throws IOException {
        registeredUsersOnline.remove(obj);
        registeredUsersOnline.add(obj);
        Platform.runLater(() -> showUsersOnline());
        for (ClientHandler user : registeredUsersOnline) user.sendService("/online|" + listWhoisOnline());
        broadcastMsg("The user " + oldName + " changed the name to " + newName);
    }

    public synchronized void removeClient(ClientHandler obj) throws IOException {
        broadcastMsg("The user " + obj.getNickname() + " left the chat.");
        Platform.runLater(() -> showUsersOnline());
        //
        registeredUsersOnline.remove(obj);
        obj.close();
        //
        for (ClientHandler client : registeredUsersOnline) client.sendService("/online|" + listWhoisOnline());
    }

    public synchronized void broadcastMsg(String message) throws IOException {
        Platform.runLater(() -> controller.message(message));
        for (ClientHandler client : registeredUsersOnline) {
            client.sendService("/echo|" + message);
        }
    }

    public void privatMsg(String sender, String recipient, String message) {
        Platform.runLater(() -> controller.message("<b>" + message + "</b>"));
        for (ClientHandler client : registeredUsersOnline) {
            if (client.getNickname().equals(recipient)) {
                Platform.runLater(() -> {
                    try {
                        client.sendService("/echo|" + "<b>" + message + "</b>");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    public synchronized void message(String message) {
        Platform.runLater(() -> controller.message(message));
    }
}
