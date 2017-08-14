package ru.davidlevy.server;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

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
        for (ClientHandler client : registeredUsersOnline) if (client.getName().equals(user)) return true;
        return false;
    }

    public AuthentificationService getAuthentificationService() {
        return authentificationService;
    }

    private synchronized void showUsersOnline() {
        ArrayList<String> list = new ArrayList<String>();
        for (ClientHandler user : registeredUsersOnline) list.add(user.getName());
        items = FXCollections.observableArrayList(list);
        controller.listView.setItems(items);
        controller.listView.refresh();
    }

    public synchronized String listWhoisOnline() {
        String result = "";
        for (ClientHandler client : registeredUsersOnline) result += client.getName() + "|";
        return result;
    }

    public synchronized void addClient(ClientHandler obj) {
        registeredUsersOnline.add(obj);
        Platform.runLater(() -> showUsersOnline());
        obj.sendService("/authok|" + obj.getName());
        //
        String list = listWhoisOnline();
        for (ClientHandler user : registeredUsersOnline) user.sendService("/online|" + list);
        broadcastMsg("The user " + obj.getName() + " has joined the chat.");
    }

    public synchronized void changeNameClient(ClientHandler obj, String newName, String oldName) {
        registeredUsersOnline.remove(obj);
        registeredUsersOnline.add(obj);
        Platform.runLater(() -> showUsersOnline());
        for (ClientHandler user : registeredUsersOnline) user.sendService("/online|" + listWhoisOnline());
        broadcastMsg("The user " + oldName + " changed the name to " + newName);
    }

    public synchronized void removeClient(ClientHandler obj) {
        broadcastMsg("The user " + obj.getName() + " left the chat.");
        Platform.runLater(() -> showUsersOnline());
        //
        registeredUsersOnline.remove(obj);
        obj.close();
        //
        for (ClientHandler client : registeredUsersOnline) client.sendService("/online|" + listWhoisOnline());
    }

    public synchronized void broadcastMsg(String message) {
        Platform.runLater(() -> controller.message(message));
        for (ClientHandler client : registeredUsersOnline) {
            client.sendService("/echo|" + message);
        }
    }

    public void privatMsg(String sender, String recipient, String message) {
        Platform.runLater(() -> controller.message("<b>" + message + "</b>"));
        for (ClientHandler client : registeredUsersOnline) {
            if (client.getName().equals(recipient)) {
                Platform.runLater(() -> client.sendService("/echo|" + "<b>" + message + "</b>"));
            }
        }
    }

    public synchronized void message(String message) {
        Platform.runLater(() -> controller.message(message));
    }
}
