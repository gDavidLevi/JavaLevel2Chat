package ru.davidlevy.server;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;

@XmlRootElement
public class Parametres {
    private static String fileXml = (isWindows() ? "src/ru/davidlevy/server/" : "src/ru/davidlevy/server/") + "parametres.xml";
    private int port;

    private static boolean isWindows(){
        return (System.getProperty("os.name").toLowerCase().contains("win"));
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
            marshaller.marshal(Controller.settings, new File(fileXml));
            marshaller.marshal(Controller.settings, System.out);
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
            setPort(parametres.port);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }
}
