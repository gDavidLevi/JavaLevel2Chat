package ru.davidlevy.server;

import java.util.ArrayList;

/**
 * Служба аутентификации
 */
public interface AuthentificationService {
    /**
     * Запуск
     */
    void start();

    /**
     * Остановка
     */
    void stop();

    /**
     * Получить имя пользователя по логин уи паролю
     *
     * @param login    логин
     * @param password пароль
     * @return имя пользователя String
     */
    String getUser(String login, String password);

    /**
     * Возвращает имя пользователя из БД если он там есть, иначе null
     *
     * @param user имя пользователя
     * @return имя пользователя String
     */
    String getUser(String user);

    /**
     * Возвращает пароль по логину
     *
     * @param login логин
     * @return пароль String
     */
    String getPassword(String login);

    /**
     * Возвращает логин из БД по логину
     *
     * @param login логин
     * @return логин из БД String
     */
    String getLogin(String login);

    /**
     * Регистраци пользователя
     *
     * @param login    новый логин
     * @param password новый пароль
     * @param name     новое имя пользователя
     * @return true - успешно
     */
    boolean registration(String login, String password, String name);

    /**
     * Изменяет имя пользователя в БД
     *
     * @param oldName старое имя
     * @param newName новое имя
     * @return true - усешное изменение
     */
    boolean changeUserName(String oldName, String newName);

    /**
     * Сохранение переписку в БД
     *
     * @param datetime время
     * @param sender   отправитель
     * @param receiver получатель
     * @param type     тип сообщения
     * @param message  сообщение
     * @return true - успешное добавление
     */
    boolean saveToHistory(String datetime, String sender, String receiver, String type, String message);

    /**
     * Получить историю перепеиски
     *
     * @param sender история пользователя
     * @param day    за количество дней
     * @return ArrayList<String>
     */
    ArrayList<String> getHistory(String sender, int day);
}