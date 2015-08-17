package organizer.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import org.xml.sax.SAXException;

/**
 *
 * @author Ilya
 */
public class Server {

    /**
     * @param args the command line arguments
     * @throws java.lang.ClassNotFoundException
     * @throws javax.xml.bind.JAXBException
     * @throws org.xml.sax.SAXException
     * @throws javax.xml.parsers.ParserConfigurationException
     * @throws java.io.IOException
     * @throws javax.xml.transform.TransformerConfigurationException
     */
    public static void main(String[] args)
            throws ClassNotFoundException, JAXBException, SAXException, ParserConfigurationException,
            IOException, TransformerConfigurationException {
        LinkedList userList = new LinkedList();
        int port1 = 6667; // случайный порт (может быть любое число от 1025 до 65535)
        //int port2 = 6668;
        try {
            ServerSocket serverSocket = new ServerSocket(port1);
            //ServerSocket serverSocket2 = new ServerSocket(port2);
            System.out.println("жду клиента...");
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("клиент подключился");
                System.out.println();
                
                //Socket socket2 = serverSocket2.accept();
                //System.out.println("клиент демон подключился");
                //System.out.println();

                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                
                //ObjectOutputStream outUpdate = new ObjectOutputStream(socket2.getOutputStream());
                //userList.addLast(outUpdate);
                ThreadServer r = new ThreadServer(out, in, userList);
                r.start();
            }
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
