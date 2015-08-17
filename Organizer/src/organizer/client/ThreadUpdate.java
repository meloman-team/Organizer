/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package organizer.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Ilya
 */
public class ThreadUpdate extends Thread {

    public ThreadUpdate(ClientForm clientForm, ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        
        while (true) {
            Document doc = (Document) in.readObject();
            NodeList rootChildren = doc.getDocumentElement().getChildNodes();
            Node searchNodeByName = searchNodeByNameOnFirstLevel(rootChildren, "task");
            String task = searchNodeByName.getTextContent();
            if(task.equals("updatePublicTree")) clientForm.updatePublicTree();
        }
        
    }

    private Node searchNodeByNameOnFirstLevel(NodeList rootChildren, String name) {
        for (int i = 0; i < rootChildren.getLength(); i++) {
            if (rootChildren.item(i).getNodeName().equals(name)) {
                return rootChildren.item(i);
            }
        }
        return null;
    }
}
