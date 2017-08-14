package ru.davidlevy.client;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Calendar;

import static javafx.scene.control.SelectionMode.*;

public class Controller implements Initializable {
    @FXML
    public TextArea inputArea;
    @FXML
    public WebView browser;
    @FXML
    private ColorPicker pickerMakeColor;
    @FXML
    public ListView<String> listView;
    @FXML
    public TextField loginInput;
    @FXML
    public PasswordField passwordInput;
    @FXML
    public HBox loginPanel;
    @FXML
    public HBox chatPanel;
    @FXML
    public HBox registerPanel;
    @FXML
    public HBox parametresPanel;
    @FXML
    public Button buttonConnect;
    @FXML
    public Button buttonSendMessage;
    @FXML
    public Button buttonBack;
    @FXML
    public Button buttonSaveSettings;
    @FXML
    public TextField newLoginInput;
    @FXML
    public PasswordField newPassInput;
    @FXML
    public TextField newUserInput;
    @FXML
    public TextField emailInput;
    @FXML
    public TextField settingsIpAdress;
    @FXML
    public TextField settingsPort;
    //
    public static Parametres parametres = new Parametres();
    private static StringBuffer html = new StringBuffer();
    private static ArrayList<String> list = new ArrayList<String>();
    //
    private Thread thread;
    private static Socket socket;
    private static DataInputStream in;
    private static DataOutputStream out;
    private static String str = null;
    private boolean authorized = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setAuthorized(false);
        parametres.loadFromXml();
        //
        listView.getSelectionModel().setSelectionMode(MULTIPLE);
        pickerMakeColor.setValue(Color.BLACK);
        //
        daemon();
    }

    private void daemon() {
        thread = new Thread(() -> {
            try {
                socket = new Socket(parametres.getServer(), parametres.getPort());
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());
                //
                while (true) {
                    str = in.readUTF();
                    //
                    if (str.startsWith("/authok|")) {
                        parametres.setUser(str.split("\\|")[1]);  // получили ник и записали его в настройки
                        //Platform.runLater(() -> message("<h3>" + parametres.getUser() + "</h3>"));
                        parametres.saveToXml();
                        parametres.loadFromXml();
                        Platform.runLater(() -> setAuthorized(true));
                        Platform.runLater(() -> MainClassClient.mainStage.setTitle("Chatter [" + parametres.getUser() + "]"));
                        break;
                    }
                    //
                    if (str.startsWith("/allert|")) {
                        str = str.replace("/allert|", "");
                        Platform.runLater(() -> showAllert(str));
                    }
                }
                //
                while (true) {
                    str = in.readUTF();
                    //System.out.println("[CLIENT] получил от сервера: " + str);
                    //
                    if (str.startsWith("/allert|")) {
                        str = str.replace("/allert|", "");
                        Platform.runLater(() -> showAllert(str));
                    }
                    //
                    if (str.startsWith("/online|")) {
                        str = str.replace("/online|", "");
                        ArrayList<String> list = new ArrayList<String>();
                        list.clear();
                        list.addAll(Arrays.stream(str.split("\\|")).collect(Collectors.toList()));
                        ObservableList<String> items = FXCollections.observableArrayList(list);
                        Platform.runLater(() -> listView.setItems(items));
                    }
                    //
                    if (str.startsWith("/echo|")) {
                        str = str.replace("/echo|", "");
                        System.out.println("[CLIENT] получил от сервера: " + str);
                        Platform.runLater(() -> message(str));
                    }
                    //
                    if (str.startsWith("/newnick|")) {
                        str = str.replace("/newnick|", "");
                        parametres.setUser(str);
                        parametres.saveToXml();
                        Platform.runLater(() -> MainClassClient.mainStage.setTitle("Chatter [" + parametres.getUser() + "]"));
                    }
                }
            } catch (EOFException e) {
                // тут пользователь всегда выходит из чата
            } catch (IOException e) {
                Platform.runLater(() -> message("[CHAT]* No connection to the server."));
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.setDaemon(true);
        thread.start();

    }

    public void onReconnectToServer(ActionEvent actionEvent) {
        try {
            out.writeUTF("/disconnect");
            //
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        thread.interrupt();
        setAuthorized(false);
        //
        parametres.loadFromXml();
        daemon();
    }

    public void onResistration(ActionEvent actionEvent) {
        String login = newLoginInput.getText();
        String pass = newPassInput.getText();
        String name = newUserInput.getText();
        try {
            String request = "/registration|" + login + "|" + pass + "|" + name;
            out.writeUTF(request);
            System.out.println(request);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
        if (!this.authorized) {
            showLoginPanel();
        } else {
            showChatPanel();
        }
    }

    public void onButtonSettings(ActionEvent actionEvent) {
        showParametresPanel();
    }

    public void onSaveSettings(ActionEvent actionEvent) {
        parametres.setServer(settingsIpAdress.getText());
        parametres.setPort(Integer.parseInt(settingsPort.getText()));
        parametres.saveToXml();
        showChatPanel();
    }

    private void showParametresPanel() {
        settingsIpAdress.setText(parametres.getServer());
        settingsPort.setText(String.valueOf(parametres.getPort()));
        //
        loginPanel.setVisible(false);
        loginPanel.setManaged(false);
        chatPanel.setVisible(false);
        chatPanel.setManaged(false);
        registerPanel.setVisible(false);
        registerPanel.setManaged(false);
        parametresPanel.setVisible(true);
        parametresPanel.setManaged(true);
        buttonSaveSettings.requestFocus();
    }

    private void showChatPanel() {
        loginPanel.setVisible(false);
        loginPanel.setManaged(false);
        chatPanel.setVisible(true);
        chatPanel.setManaged(true);
        registerPanel.setVisible(false);
        registerPanel.setManaged(false);
        parametresPanel.setVisible(false);
        parametresPanel.setManaged(false);
        buttonSendMessage.requestFocus();
    }

    private void showLoginPanel() {
        loginPanel.setVisible(true);
        loginPanel.setManaged(true);
        chatPanel.setVisible(false);
        chatPanel.setManaged(false);
        registerPanel.setVisible(false);
        registerPanel.setManaged(false);
        parametresPanel.setVisible(false);
        parametresPanel.setManaged(false);
        buttonConnect.requestFocus();
    }

    private void showRegistrationPanel() {
        loginPanel.setVisible(false);
        loginPanel.setManaged(false);
        chatPanel.setVisible(false);
        chatPanel.setManaged(false);
        registerPanel.setVisible(true);
        registerPanel.setManaged(true);
        parametresPanel.setVisible(false);
        parametresPanel.setManaged(false);
        buttonBack.requestFocus();
    }

    private void authorisation() {
        String login = loginInput.getText();
        String pass = passwordInput.getText();
        try {
            out.writeUTF("/auth|" + login + "|" + pass);
        } catch (IOException e) {
            e.printStackTrace();
        }
        loginInput.clear();
        passwordInput.clear();
    }

    public void onConnect(ActionEvent actionEvent) {
        authorisation();
    }

    public void onRequestOnlineUsers(ActionEvent actionEvent) {
        whoIsOnline();
    }

    private void whoIsOnline() {
        try {
            out.writeUTF("/whoisonline");
            System.out.println("/whoisonline");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAllert(String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void message(String message) {
        list.add(message);
        showInPage();
    }

    private void showInPage() {
        browser.getEngine().setUserStyleSheetLocation(getClass().getResource("style.css").toString());
        html.delete(0, html.length())
                .append("<!DOCTYPE html>").append("<html lang=\"ru\">")
                .append("<head>").append("<meta charset=\"UTF-8\">").append("</head>")
                .append("<body>");
        for (String line : list) html.append("<p>").append(line).append("</p>");
        html.append("</body>");
        browser.getEngine().loadContent(html.toString());
        browser.getEngine().reload();
        //browser.getEngine().executeScript("window.scrollTo(0, document.body.scrollHeight);");
    }

    private String timestamp() { // "YYYY-MM-DD HH:MM:SS.SSS"
        Calendar calendar = Calendar.getInstance();
        java.sql.Timestamp timeStamp = new java.sql.Timestamp(calendar.getTime().getTime());
        return timeStamp.toString();
    }

    private void sendMessage() {
        // если выделено несколько
        ObservableList<String> selectedItems = listView.getSelectionModel().getSelectedItems();
        for (String item : selectedItems) {
            if (item != null) {
                try {
                    StringBuilder privatMsg = new StringBuilder();
                    privatMsg.append("<font color=\"grey\">[" + timestamp() + "]</font>&nbsp;")
                            .append("<font color=\"blue\">[" + parametres.getUser() + "]</font>&nbsp;->&nbsp;[" + item + "]::&nbsp;")
                            .append(inputArea.getText());
                    //
                    out.writeUTF("/msgpr|" + timestamp() + "|" + item + "|" + privatMsg.toString());  // отправляем на сервер
                    message(privatMsg.toString());  // сохряняем себе сообщения
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        // если не выделенно ни одного
        if (listView.getSelectionModel().getSelectedIndex() == -1) {
            try {
                Calendar calendar = Calendar.getInstance();
                java.sql.Timestamp timeStamp = new java.sql.Timestamp(calendar.getTime().getTime());
                //
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("<font color=\"grey\">[" + timestamp() + "]</font>&nbsp;")
                        .append("<font color=\"blue\">[" + parametres.getUser() + "]</font>&nbsp;::&nbsp;")
                        .append(inputArea.getText());
                out.writeUTF("/msgbc|" + stringBuilder.toString());
            } catch (IOException e) {
                message("[CHAT] 'sendMessage' : " + e);
            }
        }
        //
        inputArea.clear();
        inputArea.requestFocus();
        //
        listView.getSelectionModel().select(-1);
    }

    public void onSelectAll(ActionEvent actionEvent) {
        listView.getSelectionModel().selectAll();  //select all
    }

    public void onDeselectAll(ActionEvent actionEvent) {
        listView.getSelectionModel().select(-1);  //deselect all
    }


    static void disconnect() {
        try {
            out.writeUTF("/disconnect");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }

    public void onButtonSendMessageClick(ActionEvent actionEvent) {
        sendMessage();
    }

    public void onCtrlEnterSendMessage(KeyEvent keyEvent) {
        if (keyEvent.isControlDown() && keyEvent.getCode().equals(KeyCode.ENTER)) sendMessage();
    }

    private String teggingText(String teg, TextArea a, String color) {
        StringBuilder str = new StringBuilder();
        str.append(a.getText().substring(0, a.getSelection().getStart()));
        if (teg == "font") {
            str.append("<" + teg + " color=" + color + ">");
        } else {
            str.append("<" + teg + ">");
        }
        str.append(a.getText().substring(a.getSelection().getStart(), a.getSelection().getEnd()))
                .append("</" + teg + ">")
                .append(a.getText().substring(a.getSelection().getEnd(), a.getLength()));
        return str.toString();
    }

    public void onMakeTextBold(ActionEvent actionEvent) {
        inputArea.setText(teggingText("b", inputArea, null));
    }

    public void onMakeTextItalic(ActionEvent actionEvent) {
        inputArea.setText(teggingText("i", inputArea, null));
    }

    public void onMakeTextUnderline(ActionEvent actionEvent) {
        inputArea.setText(teggingText("u", inputArea, null));
    }

    public void onMakeTextColor(ActionEvent actionEvent) {
        String color = "#" + pickerMakeColor.getValue().toString().substring(2, 8);
        inputArea.setText(teggingText("font", inputArea, color));
    }

    public void onButtonSaveAs(ActionEvent actionEvent) {
        FileChooser saver = new FileChooser();
        saver.setTitle("Save file as...");
        saver.setInitialFileName("history.html");
        File file = saver.showSaveDialog(MainClassClient.mainStage);
        if (file != null) {
            try {
                FileWriter out = new FileWriter(file);
                out.write(html.toString());
                out.close();
            } catch (IOException e) {
                message("[CHAT] 'FileWriter' " + e.toString());
            }
        }
    }

    public void onShowRegisterPanel(ActionEvent actionEvent) {
        showRegistrationPanel();
    }

    public void onShowLoginPanel(ActionEvent actionEvent) {
        showLoginPanel();
    }

    public void onButtonChangeName(ActionEvent actionEvent) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Change name");
        dialog.setHeaderText("You are to change the display name in the chat");
        dialog.setContentText("New name:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent((n) -> {
            changeName(n);
        });
    }

    private void changeName(String newName) {
        try {
            out.writeUTF("/changenick|" + newName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onCleanerMessages(ActionEvent actionEvent) {
        list.clear();
        showInPage();
    }

    public void onButtonExit(ActionEvent actionEvent) {
        disconnect();
    }

    private void getHistory(String days) {
        try {
            System.out.println("/gethistory|" + days);
            out.writeUTF("/gethistory|" + days);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onRequestGetHistory(ActionEvent actionEvent) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Get history");
        dialog.setHeaderText("How many last days to download the history from the server");
        dialog.setContentText("Days:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent((n) -> {
            getHistory(n);
        });
    }
}
