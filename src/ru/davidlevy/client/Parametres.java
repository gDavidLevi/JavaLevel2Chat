package ru.davidlevy.client;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;

@XmlRootElement
public class Parametres {
    private static String fileXml = (isWindows() ? "src/ru/davidlevy/client/" : "src/ru/davidlevy/client/") + "parametres.xml";
    private String user;
    private String server;
    private int port;

    private static boolean isWindows(){
        return (System.getProperty("os.name").toLowerCase().contains("win"));
    }

    public String getUser() {
        return this.user;
    }

    @XmlElement
    public void setUser(String user) {
        this.user = user;
    }

    public String getServer() {
        return this.server;
    }

    @XmlElement
    public void setServer(String server) {
        this.server = server;
    }

    public int getPort() {
        return this.port;
    }

    @XmlElement
    public void setPort(int port) {
        this.port = port;
    }

    public void saveToXml() {
        try {
            JAXBContext context = JAXBContext.newInstance(Parametres.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(Controller.parametres, new File(fileXml));
            //marshaller.marshal(Controller.parametres, System.out);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    public void loadFromXml() {
        try {
            JAXBContext context = JAXBContext.newInstance(Parametres.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            Parametres parametres = (Parametres) unmarshaller.unmarshal(new File(fileXml));
            //TODO Загружаем из XML и вставляем в поля класса Parametres
            setUser(parametres.user);
            setServer(parametres.server);
            setPort(parametres.port);
            //
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }
}
