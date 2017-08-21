package ru.davidlevy.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.Calendar;

/**
 * Обработчик клиентского потока
 */
public class ClientHandler {
    /* Инициируемые поля класса в конструкторе */
    private Server server;
    private Socket socket;

    /* Потоки данных */
    private DataInputStream in;
    private DataOutputStream out;

    private String nickname;
    private int counterActivity;

    /* Динамическая инициализация */ {
        this.nickname = null;
        this.counterActivity = 0;
    }

    /**
     * @param server     сервер
     * @param socket     соккет
     * @param controller контекст контроллера
     * @throws IOException исключение
     */
    public ClientHandler(Server server, Socket socket, Controller controller) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(this.socket.getInputStream());
        this.out = new DataOutputStream(this.socket.getOutputStream());
        clientThread();
    }

    /**
     * Поток клиентский
     *
     * @throws IOException исключение
     */
    private void clientThread() throws IOException {
        Thread thread = new Thread(() -> {
            try {
                /* Цикл аутентификации и регистрации */
                while (true) {
                    String str = in.readUTF();
                    System.out.println(str);

                    /* Аутентификация */
                    if (str.startsWith("/auth|")) {
                        String[] arg = str.split("\\|", 3);
                        String login = arg[1];
                        String password = arg[2];
                        String loginInDb = this.server.getAuthentificationService().getLogin(login);
                        String passwordInDb = this.server.getAuthentificationService().getPassword(login);
                        //
                        if (loginInDb != null && loginInDb.equals(login)) { // Логин корректен
                            if (passwordInDb != null && passwordInDb.equals(password)) {  // Пароль корректен
                                if (!this.server.isUserConnected(login)) { // Пользователь не подключен к серверу
                                    this.nickname = this.server.getAuthentificationService().getUser(arg[1], arg[2]);
                                    this.server.addClient(this);
                                    break;
                                } else sendAllert("The login is already completed.");
                            } else sendAllert("The password is incorrect.");
                        } else sendAllert("Invalid login.");
                        //
                    }
                    /* Регистрация */
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

                /* Запуск счетчика активности */
                counterActivity();

                /* Основной цикл */
                while (true) {
                    String str = in.readUTF();
                    /* Вернуть историю переписки */
                    if (str.startsWith("/gethistory|")) {
                        System.out.println("запрос на историю " + str);
                        String days = str.split("\\|")[1];  // days
                        sendService("/echo|<hr>");
                        for (String line : this.server.getAuthentificationService().getHistory(this.nickname, Integer.parseInt(days))) {
                            sendService("/echo|" + line);
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        sendService("/echo|<hr>");
                    }
                    /* Отключить пользователя */
                    if (str.startsWith("/disconnect")) {
                        this.server.removeClient(this);
                        break;
                    }
                    /* Сообщение всем */
                    if (str.startsWith("/msgbc|")) {
                        counterActivity = 0; // сбросить счетчик неактивности
                        String message = str.split("\\|")[1];
                        this.server.broadcastMsg(message);
                        setHistory(timestamp(), this.nickname, "", "broadcast", message);
                    }
                    /* Личное сообщение */
                    if (str.startsWith("/msgpr|")) {
                        counterActivity = 0; // сбросить счетчик неактивности
                        String datetime = str.split("\\|")[1];
                        String recipient = str.split("\\|")[2];
                        String message = str.split("\\|")[3];
                        this.server.privatMsg(this.nickname, recipient, message);
                        setHistory(datetime, this.nickname, recipient, "private", message);
                    }
                    /* Вернуть список активных пользователей */
                    if (str.startsWith("/whoisonline")) {
                        server.message(this.getNickname() + " > /whoisonline");
                        this.sendService("/online|" + this.server.listWhoisOnline());
                    }
                    /* Изменить имя пользователя */
                    if (str.startsWith("/changenick|")) {
                        String oldName = this.nickname;
                        this.nickname = str.split("\\|")[1];
                        if (this.server.getAuthentificationService().changeUserName(oldName, this.nickname)) {
                            this.server.changeNameClient(this, this.nickname, oldName);
                            sendAllert("Name changed.");
                            sendService("/newnick|" + this.nickname); // для интерфейса
                        } else
                            sendAllert("The name is not changed because the user name is already used in the database.");
                    }
                }
            } catch (java.net.SocketException e) {
                // закрылся соккет
            } catch (EOFException e) {
                System.out.println("Disconnect user " + this.nickname);
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

    /**
     * Счетчик активности
     */
    private void counterActivity() {
        Thread activity = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000); // 1 минута = 60000 мс
                    ++counterActivity;
                    if (counterActivity > 10) { // если больше 10 мин, то выкидываем из чата пользователя
                        sendService("/echo|<b>Disconnected due to inactivity in the chat.</b>");
                        Thread.sleep(1000);
                        this.server.removeClient(this);
                        break; // выходим из цикла и потока
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        activity.start();
    }

    /* Получить имя пользователя */
    String getNickname() {
        return this.nickname;
    }

    /**
     * Временной штамп
     *
     * @return String вида "YYYY-MM-DD HH:MM:SS.SSS"
     */
    private String timestamp() { // "YYYY-MM-DD HH:MM:SS.SSS"
        Calendar calendar = Calendar.getInstance();
        Timestamp timeStamp = new Timestamp(calendar.getTime().getTime());
        return timeStamp.toString();
    }

    /**
     * Отправить уведомление
     *
     * @param message текст сообщения
     */
    private synchronized void sendAllert(String message) throws IOException {
        this.out.writeUTF("/allert|" + message);
        System.out.println("/allert|" + message);
    }

    /**
     * Отправить сообщение в поток
     *
     * @param message сообщение
     */
    synchronized void sendService(String message) throws IOException {
        this.out.writeUTF(message);
        System.out.println(message);
    }

    /* Закрыть соккет объекта */
    synchronized void close() throws IOException {
        socket.close();
    }

    /**
     * Записать в историю
     *
     * @param datetime время
     * @param sender   отправитель
     * @param receiver получатель
     * @param type     тип сообщения
     * @param message  сообщение
     */
    private synchronized void setHistory(String datetime, String sender, String receiver, String type, String message) {
        this.server.getAuthentificationService().saveToHistory(datetime, sender, receiver, type, message);
    }
}