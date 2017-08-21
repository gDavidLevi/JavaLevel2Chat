package ru.davidlevy.server;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.web.WebView;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    public TextArea inputArea;
    @FXML
    public TextField port;
    @FXML
    public WebView browser;
    @FXML
    public ListView<String> listView;

    static Parametres settings;
    private static StringBuffer html;
    public static ArrayList<String> list;

    private Server server;

    /* Инициализация статических полей */
    static {
        settings = new Parametres();
        html = new StringBuffer();
        list = new ArrayList<>();
    }

    /* Инициализация */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        settings.loadFromXml();
        port.setText(String.valueOf(settings.getPort()));
        server = new Server(this);
    }

    public void message(String message) {
        list.add(message);
        showInPage();
    }

    private void showInPage() {
        browser.getEngine().setUserStyleSheetLocation(getClass().getResource("style.css").toString());
        html.delete(0, html.length())
                .append("<!DOCTYPE html><html lang=\"ru\"><head><meta charset=\"UTF-8\"></head><body>");
        for (String line : list) html.append("<p>").append(line).append("</p>");
        //
        html.append("</body>");
        browser.getEngine().loadContent(html.toString());
        browser.getEngine().reload();
//        browser.getEngine().executeScript("window.scrollTo(0, document.body.scrollHeight);");
    }

    public void onSaveSettings(ActionEvent actionEvent) {
        settings.setPort(Integer.parseInt(port.getText()));
        settings.saveToXml();
    }

    private String getTimeSchtamp() {
        //"2016-01-30 00:00:00"
        Calendar cal = Calendar.getInstance();
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH);
        int year = cal.get(Calendar.YEAR);
        int hh = cal.get(Calendar.HOUR_OF_DAY);
        int mm = cal.get(Calendar.MINUTE);
        int ss = cal.get(Calendar.SECOND);
        return String.format("%d-%02d-%02d %02d:%02d:%02d", year, month, day, hh, mm, ss);
    }

    private String timestamp() { // "YYYY-MM-DD HH:MM:SS.SSS"
        Calendar calendar = Calendar.getInstance();
        java.sql.Timestamp timeStamp = new java.sql.Timestamp(calendar.getTime().getTime());
        return timeStamp.toString();
    }

    private void sendMessage() throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<font color=\"grey\">[" + getTimeSchtamp() + "]</font>&nbsp;")
                .append("<font color=\"red\">[SERVER]</font>&nbsp;::&nbsp;")
                .append(inputArea.getText());
        server.broadcastMsg(stringBuilder.toString());
        server.getAuthentificationService().saveToHistory(timestamp(), "[SERVER]", "", "broadcast", stringBuilder.toString());
        inputArea.clear();
        inputArea.requestFocus();
    }

    public void onSendMessageToAll(ActionEvent actionEvent) throws IOException {
        sendMessage();
    }

    public void onCtrlEnterSendMessage(KeyEvent keyEvent) throws IOException {
        if (keyEvent.isControlDown() && keyEvent.getCode().equals(KeyCode.ENTER))
            sendMessage();
    }
}