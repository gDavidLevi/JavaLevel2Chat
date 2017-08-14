package ru.davidlevy.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.Calendar;

public class ClientHandler {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Server server;
    private String username;
    private int counterActivity = 0;
    //

    public ClientHandler(Server server, Socket socket, Controller controller) {
        this.server = server;
        this.socket = socket;
        this.username = null;
        try {
            this.in = new DataInputStream(this.socket.getInputStream());
            this.out = new DataOutputStream(this.socket.getOutputStream());
            thread();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void thread() {
        Thread thread = new Thread(() -> {
            try {
                while (true) {
                    String str = in.readUTF();
                    System.out.println(str);
                    //
                    if (str.startsWith("/auth|")) {
                        String[] arg = str.split("\\|", 3);
                        String login = arg[1];
                        String password = arg[2];
                        String loginDb = this.server.getAuthentificationService().getLogin(login);
                        String passwordDb = this.server.getAuthentificationService().getPassword(login);
                        //
                        if (loginDb != null && loginDb.equals(login)) { // Логин корректен
                            if (passwordDb != null && passwordDb.equals(password)) {  // Пароль корректен
                                if (!this.server.isUserConnected(login)) { // Пользователь не подключен к серверу
                                    this.username = this.server.getAuthentificationService().getUser(arg[1], arg[2]);
                                    this.server.addClient(this);
                                    break;
                                } else sendAllert("The login is already completed.");
                            } else sendAllert("The password is incorrect.");
                        } else sendAllert("Invalid login.");
                        //
                    }
                    //
                    if (str.startsWith("/registration|")) { // newLogin|newPassword|newUser
                        String[] arg = str.split("\\|");
                        String newLogin = arg[1];
                        String newPassword = arg[2];
                        String newUser = arg[3];
                        //
                        String dbLogin = this.server.getAuthentificationService().getLogin(newLogin);
                        System.out.println("dbLogin - " + dbLogin);
                        if (dbLogin == null) {  // если не присутствует в БД
                            if (this.server.getAuthentificationService().registration(newLogin, newPassword, newUser)) {
                                server.message("New user " + newUser + " is added to the database");
                                sendAllert("Registration is complete!");
                            } else sendAllert("Error creating user in the database.");
                        } else sendAllert("Login is already taken.");
                    }
                }
                //
                counterActivity();
                //
                while (true) {
                    String str = in.readUTF();
                    //
                    if (str.startsWith("/gethistory|")) {
                        System.out.println("запрос на историю " + str);
                        String days = str.split("\\|")[1];  // days
                        sendService("/echo|<hr>");
                        for (String line : this.server.getAuthentificationService().getHistory(this.username, Integer.parseInt(days))) {
                            sendService("/echo|" + line);
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        sendService("/echo|<hr>");
                    }
                    //
                    if (str.startsWith("/disconnect")) {
                        this.server.removeClient(this);
                        break;
                    }
                    //
                    if (str.startsWith("/msgbc|")) {

                        counterActivity = 0; // сбросить счетчик неактивности
                        String message = str.split("\\|")[1];
                        this.server.broadcastMsg(message);
                        setHistory(timestamp(), this.username, "", "broadcast", message);
                    }
                    //
                    if (str.startsWith("/msgpr|")) {
                        counterActivity = 0; // сбросить счетчик неактивности
                        String datetime = str.split("\\|")[1];
                        String recipient = str.split("\\|")[2];
                        String message = str.split("\\|")[3];
                        this.server.privatMsg(this.username, recipient, message);
                        setHistory(datetime, this.username, recipient, "private", message);
                    }
                    //
                    if (str.startsWith("/whoisonline")) {
                        server.message(this.getName() + " > /whoisonline");
                        this.sendService("/online|" + this.server.listWhoisOnline());
                    }
                    //
                    if (str.startsWith("/changenick|")) {
                        String oldName = this.username;
                        this.username = str.split("\\|")[1];
                        if (this.server.getAuthentificationService().changeUserName(oldName, this.username)) {
                            this.server.changeNameClient(this, this.username, oldName);
                            sendAllert("Name changed.");
                            sendService("/newnick|" + this.username); // для интерфейса
                        } else
                            sendAllert("The name is not changed because the user name is already used in the database.");
                    }
                }
            } catch (java.net.SocketException e) {
                // закрылся соккет
            } catch (EOFException e) {
                System.out.println("Disconnect user " + this.username);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    private void counterActivity() {
        Thread activity = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000); // 1 минута = 60000 мс
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                ++counterActivity;
                if (counterActivity > 10) { // если больше 10 мин, то выкидываем из чата пользователя
                    sendService("/echo|<b>Disconnected due to inactivity in the chat.</b>");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    this.server.removeClient(this);
                    break; // выходим из while (true){...}
                }
            }
        });
        activity.start();
    }

    public String getName() {
        return this.username;
    }

    private String timestamp() { // "YYYY-MM-DD HH:MM:SS.SSS"
        Calendar calendar = Calendar.getInstance();
        java.sql.Timestamp timeStamp = new java.sql.Timestamp(calendar.getTime().getTime());
        return timeStamp.toString();
    }

    public synchronized void sendAllert(String message) {
        try {
            this.out.writeUTF("/allert|" + message);
            System.out.println("/allert|" + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void sendService(String message) {
        try {
            this.out.writeUTF(message);
            System.out.println(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void close() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void setHistory(String datetime, String sender, String receiver, String type, String message) {
        this.server.getAuthentificationService().saveToHistory(datetime, sender, receiver, type, message);
    }
}
