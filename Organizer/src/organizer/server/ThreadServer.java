/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package organizer.server;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Ilya
 */
public class ThreadServer extends Thread {

    private static final long MILLISECONDS_IN_SECOND = 1000;
    private static final long SECONDS_IN_MINUTE = 60;
    private static final long SECONDS_IN_HOUR = SECONDS_IN_MINUTE * 60;
    private static final long SECONDS_IN_DAY = SECONDS_IN_HOUR * 24;

    ObjectOutputStream out;
    ObjectInputStream in;
    Document documentUserTree;
    Document documentPublicTree;
    String pathToUserTree;
    String pathToRegistrationTable = "src\\organizer\\server\\RegistrationTable.xml";
    String pathToPublicTree = "src\\organizer\\server\\PublicTree.xml";
    DocumentBuilder builder;
    TransformerFactory tFactory;
    LinkedList userList;

    /**
     *
     * @param out
     * @param in
     * @param userList
     * @throws javax.xml.parsers.ParserConfigurationException
     * @throws org.xml.sax.SAXException
     * @throws java.io.IOException
     * @throws javax.xml.transform.TransformerConfigurationException
     */
    public ThreadServer(ObjectOutputStream out,ObjectInputStream in, LinkedList userList)
            throws ParserConfigurationException, SAXException, IOException,
            TransformerConfigurationException {
        this.userList = userList;
        this.out = out;
        this.in = in;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        builder = factory.newDocumentBuilder();
        documentUserTree = builder.parse(pathToRegistrationTable);
        tFactory = TransformerFactory.newInstance();
    }

    @Override
    public void run() {
        try {
            do {
                Document readDocument = (Document) in.readObject();
                NodeList rootChildren = readDocument.getDocumentElement().getChildNodes();

                Node searchNodeByNameOnFirstLevel = searchNodeByNameOnFirstLevel(rootChildren, "task");
                String task = searchNodeByNameOnFirstLevel.getTextContent();
                switch (task) {
                    case "authorization":
                        authorization(rootChildren);
                        break;
                    case "registration":
                        registration(rootChildren);
                        break;
                    case "downloadTree":
                        downloadTree(rootChildren);
                        break;
                    case "downloadPublicTree":
                        downloadPublicTree();
                        break;
                    case "addNewItem":
                        addNewItem(rootChildren);
                        break;
                    case "addNewItemPublicTree":
                        addNewItemPublicTree(rootChildren);
                        //notifyUsersUpdate();
                        break;
                    case "removeItem":
                        removeItem(rootChildren);
                        break;
                    case "removePublicItem":
                        removePublicItem(rootChildren);
                        //notifyUsersUpdate();
                        break;
                    case "renameItem":
                        renameItem(rootChildren);
                        break;
                    case "renamePublicItem":
                        renamePublicItem(rootChildren);
                       // notifyUsersUpdate();
                        break;
                    case "startItem":
                        startItem(rootChildren);
                        break;
                    case "startPublicItem":
                        startItemPublicTree(rootChildren);
                        //notifyUsersUpdate();
                        break;
                    case "stopItem":
                        stopItem(rootChildren);
                        break;
                    case "stopPublicItem":
                        stopPublicItem(rootChildren);
                        //notifyUsersUpdate();
                        break;
                    case "getReport":
                        getReport(rootChildren, documentUserTree);
                        break;
                    case "getPublicReport":
                        getReport(rootChildren, documentPublicTree);
                        break;
                }
            } while (true);
        } catch (IOException | TransformerException | SAXException | ClassNotFoundException ex) {
            Logger.getLogger(ThreadServer.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }

    private void notifyUsersUpdate() throws IOException {
        Document document = builder.newDocument();
        Element taskXml = document.createElement("root");
        document.appendChild(taskXml);

        Element node = document.createElement("task");
        node.setTextContent("updatePublicTree");
        taskXml.appendChild(node);
        for (int i = 0; i < userList.size(); i++) {
            ObjectOutputStream output = (ObjectOutputStream) userList.get(i);
            output.writeObject(document);
            output.flush();
        }
    }

    private Node searchNodeById(String searchId, Node root) {
        NodeList rootChildren = root.getChildNodes();

        Node searchNodeByNameOnFirstLevel = searchNodeByNameOnFirstLevel(rootChildren, "idNode");
        String id = searchNodeByNameOnFirstLevel.getTextContent();

        if (searchId.equals(id)) {
            return root;
        }
        Node[] searchAllNodesByNameOnFirstLevel = searchAllNodesByNameOnFirstLevel(rootChildren, "cildNode");
        for (int i = 0; i < searchAllNodesByNameOnFirstLevel.length; i++) {
            Node searchNodeById = searchNodeById(searchId, searchAllNodesByNameOnFirstLevel[i]);
            if (searchNodeById != null) {
                return searchNodeById;
            }
        }
        return null;
    }

    //ищет на одном уровне первый попавшийся
    private Node searchNodeByNameOnFirstLevel(NodeList rootChildren, String name) {

        int length = rootChildren.getLength();
        for (int i = 0; i < rootChildren.getLength(); i++) {

            String nodeName = rootChildren.item(i).getNodeName();
            if (rootChildren.item(i).getNodeName().equals(name)) {
                return rootChildren.item(i);
            }
        }
        return null;
    }

    //ищет на уровне все совпадения
    private Node[] searchAllNodesByNameOnFirstLevel(NodeList rootChildren, String name) {
        ArrayList<Node> array = new ArrayList();

        for (int i = 0; i < rootChildren.getLength(); i++) {

            if (rootChildren.item(i).getNodeName().equals(name)) {
                array.add(rootChildren.item(i));
            }
        }
        if (array != null) {
            Node[] ret = new Node[array.size()];
            for (int i = 0; i < array.size(); i++) {
                ret[i] = array.get(i);
            }
            return ret;
        }
        return null;
    }

    private Long calculateAllTimeEmployment(Node root) {
        Node searchNodeByNameOnFirstLevel = searchNodeByNameOnFirstLevel(root.getChildNodes(), "timeEmployment");
        String textContent = searchNodeByNameOnFirstLevel.getTextContent();

        Long timeEmployment = null;
        if (!textContent.equals("")) {
            timeEmployment = Long.parseLong(textContent);
        }

        Node[] searchAllNodesByNameOnFirstLevel = searchAllNodesByNameOnFirstLevel(root.getChildNodes(), "cildNode");
        for (int i = 0; i < searchAllNodesByNameOnFirstLevel.length; i++) {
            Long AllTimeEmployment = calculateAllTimeEmployment(searchAllNodesByNameOnFirstLevel[i]);
            if (timeEmployment != null && AllTimeEmployment != null) {
                timeEmployment += AllTimeEmployment;
            }
            if (timeEmployment == null) {
                timeEmployment = AllTimeEmployment;
            }
        }
        return timeEmployment;
    }

    private void authorization(NodeList rootChildren) throws IOException{
        Node searchNodeByNameOnFirstLevel = searchNodeByNameOnFirstLevel(rootChildren, "login");
        String login = searchNodeByNameOnFirstLevel.getTextContent();

        searchNodeByNameOnFirstLevel = searchNodeByNameOnFirstLevel(rootChildren, "password");
        String password = searchNodeByNameOnFirstLevel.getTextContent();

        NodeList children = documentUserTree.getDocumentElement().getChildNodes();
        int length = children.getLength();
        boolean bol = false;
        for (int i = 0; i < length; i++) {
            Node node = children.item(i);
            NodeList childNodes = node.getChildNodes();
            Node item = childNodes.item(0);
            String login2 = item.getChildNodes().item(0).getTextContent();
            if (!login2.equals(login)) {
                continue;
            }
            item = childNodes.item(1);
            String password2 = item.getChildNodes().item(0).getTextContent();
            if (password2.equals(password)) {
                out.writeBoolean(true);
                out.flush();
                bol = true;
                break;
            }
        }
        if (!bol) {
            out.writeBoolean(false);
            out.flush();
        }
    }

    private void registration(NodeList rootChildren) throws TransformerException, IOException {
        Node searchNodeByNameOnFirstLevel = searchNodeByNameOnFirstLevel(rootChildren, "login");
        String login = searchNodeByNameOnFirstLevel.getTextContent();

        searchNodeByNameOnFirstLevel = searchNodeByNameOnFirstLevel(rootChildren, "password");
        String password = searchNodeByNameOnFirstLevel.getTextContent();

        NodeList children = documentUserTree.getDocumentElement().getChildNodes();
        int length = children.getLength();
        boolean bool = false;
        for (int i = 0; i < length; i++) {
            Node node = children.item(i);
            NodeList childNodes = node.getChildNodes();
            Node item = childNodes.item(0);
            String login2 = item.getChildNodes().item(0).getTextContent();
            if (login2.equals(login)) {
                out.writeBoolean(false);
                out.flush();
                bool = true;
                break;
            }
        }
        if (!bool) {
            Element user = documentUserTree.createElement("user");
            documentUserTree.getDocumentElement().appendChild(user);

            Element element = documentUserTree.createElement("login");
            element.setTextContent(login);
            user.appendChild(element);

            element = documentUserTree.createElement("password");
            element.setTextContent(password);
            user.appendChild(element);

            DOMSource dom_source = new DOMSource(documentUserTree);
            StreamResult out_stream = new StreamResult(pathToRegistrationTable);//StreamSource
            Transformer transformer = tFactory.newTransformer();
            transformer.transform(dom_source, out_stream);
            out.writeBoolean(true);
            out.flush();
        }
    }

    private void downloadPublicTree() throws SAXException, IOException {
        if (new File(pathToPublicTree).exists()) {
            documentPublicTree = builder.parse(pathToPublicTree);
            out.writeObject(documentPublicTree);
            out.flush();
        } else {
            documentUserTree = builder.newDocument();
            Element taskXml = documentUserTree.createElement("root");
            documentUserTree.appendChild(taskXml);

            Element node = documentUserTree.createElement("report");
            node.setTextContent("notDownloadTree");
            taskXml.appendChild(node);

            out.writeObject(documentUserTree);
            out.flush();
        }
    }

    private void downloadTree(NodeList rootChildren)
            throws IOException, ClassNotFoundException, TransformerConfigurationException, TransformerException,
            SAXException {

        Node searchNodeByNameOnFirstLevel = searchNodeByNameOnFirstLevel(rootChildren, "userName");
        String userName = searchNodeByNameOnFirstLevel.getTextContent();

        pathToUserTree = "src\\organizer\\server\\usersTree\\"
                + userName + ".xml";
        if (new File(pathToUserTree).exists()) {
            documentUserTree = builder.parse(pathToUserTree);
            out.writeObject(documentUserTree);
            out.flush();
        } else {
            documentUserTree = builder.newDocument();
            Element taskXml = documentUserTree.createElement("root");
            documentUserTree.appendChild(taskXml);

            Element node = documentUserTree.createElement("report");
            node.setTextContent("notDownloadTree");
            taskXml.appendChild(node);

            out.writeObject(documentUserTree);
            out.flush();

            Object readObj = in.readObject();
            documentUserTree = (Document) readObj;

            NodeList root = documentUserTree.getDocumentElement().getChildNodes();
            searchNodeByNameOnFirstLevel = searchNodeByNameOnFirstLevel(root, "cildNode");
            searchNodeByNameOnFirstLevel = searchNodeByNameOnFirstLevel(searchNodeByNameOnFirstLevel.getChildNodes(), "lastDate");
            searchNodeByNameOnFirstLevel.setTextContent(String.valueOf(Calendar.getInstance().getTimeInMillis() / MILLISECONDS_IN_SECOND));

            DOMSource dom_source = new DOMSource(documentUserTree);
            StreamResult out_stream = new StreamResult(pathToUserTree);
            Transformer transformer = tFactory.newTransformer();
            transformer.transform(dom_source, out_stream);
        }
    }

    private void addNewItem(NodeList rootChildren) throws TransformerException {
        Node searchNodeByNameOnFirstLevel = searchNodeByNameOnFirstLevel(rootChildren, "idParentTask");
        String idParentTask = searchNodeByNameOnFirstLevel.getTextContent();

        searchNodeByNameOnFirstLevel = searchNodeByNameOnFirstLevel(rootChildren, "nameTask");
        String nameTask = searchNodeByNameOnFirstLevel.getTextContent();

        searchNodeByNameOnFirstLevel = searchNodeByNameOnFirstLevel(rootChildren, "idTask");
        String idTask = searchNodeByNameOnFirstLevel.getTextContent();

        Node root = documentUserTree.getDocumentElement();
        Node searchNodeById = searchNodeById(idParentTask, root);

        root = documentUserTree.createElement("cildNode");
        searchNodeById.appendChild(root);

        Element node = documentUserTree.createElement("nameNode");
        node.setTextContent(nameTask);
        root.appendChild(node);

        node = documentUserTree.createElement("idNode");
        node.setTextContent(idTask);
        root.appendChild(node);

        node = documentUserTree.createElement("lastDate");
        root.appendChild(node);

        node = documentUserTree.createElement("timeEmployment");
        root.appendChild(node);

        DOMSource dom_source = new DOMSource(documentUserTree);
        StreamResult out_stream = new StreamResult(pathToUserTree);//StreamSource
        Transformer transformer = tFactory.newTransformer();
        transformer.transform(dom_source, out_stream);
    }

    private void addNewItemPublicTree(NodeList rootChildren) throws TransformerException {
        Node searchNodeByNameOnFirstLevel = searchNodeByNameOnFirstLevel(rootChildren, "idParentTask");
        String idParentTask = searchNodeByNameOnFirstLevel.getTextContent();

        searchNodeByNameOnFirstLevel = searchNodeByNameOnFirstLevel(rootChildren, "nameTask");
        String nameTask = searchNodeByNameOnFirstLevel.getTextContent();

        searchNodeByNameOnFirstLevel = searchNodeByNameOnFirstLevel(rootChildren, "idTask");
        String idTask = searchNodeByNameOnFirstLevel.getTextContent();

        Node root = documentPublicTree.getDocumentElement();
        Node searchNodeById = searchNodeById(idParentTask, root);

        root = documentPublicTree.createElement("cildNode");
        searchNodeById.appendChild(root);

        Element node = documentPublicTree.createElement("nameNode");
        node.setTextContent(nameTask);
        root.appendChild(node);

        node = documentPublicTree.createElement("idNode");
        node.setTextContent(idTask);
        root.appendChild(node);

        node = documentPublicTree.createElement("lastDate");
        root.appendChild(node);

        node = documentPublicTree.createElement("timeEmployment");
        root.appendChild(node);

        node = documentPublicTree.createElement("performanceNameUser");
        root.appendChild(node);

        DOMSource dom_source = new DOMSource(documentPublicTree);
        StreamResult out_stream = new StreamResult(pathToPublicTree);
        Transformer transformer = tFactory.newTransformer();
        transformer.transform(dom_source, out_stream);
    }

    private void removeItem(NodeList rootChildren) throws TransformerException {
        Node searchNodeByName = searchNodeByNameOnFirstLevel(rootChildren, "idTask");
        String idTask = searchNodeByName.getTextContent();

        Node root = documentUserTree.getDocumentElement();
        Node searchNodeById = searchNodeById(idTask, root);

        Node parentNode = searchNodeById.getParentNode();

        parentNode.removeChild(searchNodeById);

        DOMSource dom_source = new DOMSource(documentUserTree);
        StreamResult out_stream = new StreamResult(pathToUserTree);
        Transformer transformer = tFactory.newTransformer();
        transformer.transform(dom_source, out_stream);
    }

    private void removePublicItem(NodeList rootChildren) throws TransformerException {
        Node searchNodeByName = searchNodeByNameOnFirstLevel(rootChildren, "idTask");
        String idTask = searchNodeByName.getTextContent();

        Node root = documentPublicTree.getDocumentElement();
        Node searchNodeById = searchNodeById(idTask, root);

        Node parentNode = searchNodeById.getParentNode();

        parentNode.removeChild(searchNodeById);

        DOMSource dom_source = new DOMSource(documentPublicTree);
        StreamResult out_stream = new StreamResult(pathToPublicTree);
        Transformer transformer = tFactory.newTransformer();
        transformer.transform(dom_source, out_stream);
    }

    private void renameItem(NodeList rootChildren) throws TransformerException {
        Node searchNodeByNameOnFirstLevel = searchNodeByNameOnFirstLevel(rootChildren, "nameTask");
        String nameTask = searchNodeByNameOnFirstLevel.getTextContent();

        searchNodeByNameOnFirstLevel = searchNodeByNameOnFirstLevel(rootChildren, "idTask");
        String idTask = searchNodeByNameOnFirstLevel.getTextContent();

        Node root = documentUserTree.getDocumentElement();
        Node searchNodeById = searchNodeById(idTask, root);

        searchNodeByNameOnFirstLevel = searchNodeByNameOnFirstLevel(searchNodeById.getChildNodes(), "nameNode");
        searchNodeByNameOnFirstLevel.setTextContent(nameTask);

        DOMSource dom_source = new DOMSource(documentUserTree);
        StreamResult out_stream = new StreamResult(pathToUserTree);
        Transformer transformer = tFactory.newTransformer();
        transformer.transform(dom_source, out_stream);
    }

    private void renamePublicItem(NodeList rootChildren) throws TransformerException {
        Node searchNodeByNameOnFirstLevel = searchNodeByNameOnFirstLevel(rootChildren, "nameTask");
        String nameTask = searchNodeByNameOnFirstLevel.getTextContent();

        searchNodeByNameOnFirstLevel = searchNodeByNameOnFirstLevel(rootChildren, "idTask");
        String idTask = searchNodeByNameOnFirstLevel.getTextContent();

        Node root = documentPublicTree.getDocumentElement();
        Node searchNodeById = searchNodeById(idTask, root);

        searchNodeByNameOnFirstLevel = searchNodeByNameOnFirstLevel(searchNodeById.getChildNodes(), "nameNode");
        searchNodeByNameOnFirstLevel.setTextContent(nameTask);

        DOMSource dom_source = new DOMSource(documentPublicTree);
        StreamResult out_stream = new StreamResult(pathToPublicTree);//StreamSource
        Transformer transformer = tFactory.newTransformer();
        transformer.transform(dom_source, out_stream);
    }

    private void startItem(NodeList rootChildren) throws TransformerException {
        Node searchNodeByNameOnFirstLevel = searchNodeByNameOnFirstLevel(rootChildren, "idTask");
        String idTask = searchNodeByNameOnFirstLevel.getTextContent();

        Node root = documentUserTree.getDocumentElement();
        Node searchNodeById = searchNodeById(idTask, root);
        Long lastDate = Calendar.getInstance().getTimeInMillis() / MILLISECONDS_IN_SECOND;
        searchNodeByNameOnFirstLevel = searchNodeByNameOnFirstLevel(searchNodeById.getChildNodes(), "lastDate");
        searchNodeByNameOnFirstLevel.setTextContent(String.valueOf(lastDate));

        DOMSource dom_source = new DOMSource(documentUserTree);
        StreamResult out_stream = new StreamResult(pathToUserTree);
        Transformer transformer = tFactory.newTransformer();
        transformer.transform(dom_source, out_stream);
    }

    private void startItemPublicTree(NodeList rootChildren) throws TransformerException {
        Node searchNodeByNameOnFirstLevel = searchNodeByNameOnFirstLevel(rootChildren, "idTask");
        String idTask = searchNodeByNameOnFirstLevel.getTextContent();

        Node root = documentPublicTree.getDocumentElement();
        Node searchNodeById = searchNodeById(idTask, root);
        Long lastDate = Calendar.getInstance().getTimeInMillis() / MILLISECONDS_IN_SECOND;

        searchNodeByNameOnFirstLevel = searchNodeByNameOnFirstLevel(searchNodeById.getChildNodes(), "lastDate");
        searchNodeByNameOnFirstLevel.setTextContent(String.valueOf(lastDate));

        searchNodeByNameOnFirstLevel = searchNodeByNameOnFirstLevel(rootChildren, "userName");
        String userName = searchNodeByNameOnFirstLevel.getTextContent();

        searchNodeByNameOnFirstLevel = searchNodeByNameOnFirstLevel(searchNodeById.getChildNodes(), "performanceNameUser");
        searchNodeByNameOnFirstLevel.setTextContent(userName);

        DOMSource dom_source = new DOMSource(documentPublicTree);
        StreamResult out_stream = new StreamResult(pathToPublicTree);
        Transformer transformer = tFactory.newTransformer();
        transformer.transform(dom_source, out_stream);
    }

    private void stopItem(NodeList rootChildren) throws TransformerException {
        Node searchNodeByNameOnFirstLevel = searchNodeByNameOnFirstLevel(rootChildren, "idTask");
        String idTask = searchNodeByNameOnFirstLevel.getTextContent();

        Node root = documentUserTree.getDocumentElement();
        Node searchNodeById = searchNodeById(idTask, root);
        searchNodeByNameOnFirstLevel = searchNodeByNameOnFirstLevel(searchNodeById.getChildNodes(), "lastDate");
        String textContent = searchNodeByNameOnFirstLevel.getTextContent();
        Long lastDate = null;
        if (!textContent.equals("")) {
            lastDate = Long.decode(searchNodeByNameOnFirstLevel.getTextContent());
        }

        searchNodeByNameOnFirstLevel = searchNodeByNameOnFirstLevel(searchNodeById.getChildNodes(), "timeEmployment");
        textContent = searchNodeByNameOnFirstLevel.getTextContent();
        Long timeEmployment = null;
        if (!textContent.equals("")) {
            timeEmployment = Long.decode(textContent);
        }

        if (timeEmployment != null) {
            timeEmployment += Calendar.getInstance().getTimeInMillis() / MILLISECONDS_IN_SECOND - lastDate;
        } else {
            timeEmployment = Calendar.getInstance().getTimeInMillis() / MILLISECONDS_IN_SECOND - lastDate;

        }
        searchNodeByNameOnFirstLevel.setTextContent(String.valueOf(timeEmployment));

        DOMSource dom_source = new DOMSource(documentUserTree);
        StreamResult out_stream = new StreamResult(pathToUserTree);
        Transformer transformer = tFactory.newTransformer();
        transformer.transform(dom_source, out_stream);
    }

    private void stopPublicItem(NodeList rootChildren) throws TransformerException {

        Node searchNodeByNameOnFirstLevel = searchNodeByNameOnFirstLevel(rootChildren, "idTask");
        String idTask = searchNodeByNameOnFirstLevel.getTextContent();

        Node root = documentPublicTree.getDocumentElement();
        Node searchNodeById = searchNodeById(idTask, root);

        searchNodeByNameOnFirstLevel = searchNodeByNameOnFirstLevel(searchNodeById.getChildNodes(), "lastDate");
        String textContent = searchNodeByNameOnFirstLevel.getTextContent();
        Long lastDate = null;
        if (!textContent.equals("")) {
            lastDate = Long.decode(searchNodeByNameOnFirstLevel.getTextContent());
        }

        searchNodeByNameOnFirstLevel = searchNodeByNameOnFirstLevel(searchNodeById.getChildNodes(), "timeEmployment");
        textContent = searchNodeByNameOnFirstLevel.getTextContent();
        Long timeEmployment = null;
        if (!textContent.equals("")) {
            timeEmployment = Long.decode(textContent);
        }

        if (timeEmployment != null) {
            timeEmployment += Calendar.getInstance().getTimeInMillis() / MILLISECONDS_IN_SECOND - lastDate;
        } else {
            timeEmployment = Calendar.getInstance().getTimeInMillis() / MILLISECONDS_IN_SECOND - lastDate;

        }
        searchNodeByNameOnFirstLevel.setTextContent(String.valueOf(timeEmployment));

        searchNodeByNameOnFirstLevel = searchNodeByNameOnFirstLevel(searchNodeById.getChildNodes(), "performanceNameUser");
        searchNodeByNameOnFirstLevel.setTextContent("");

        DOMSource dom_source = new DOMSource(documentPublicTree);
        StreamResult out_stream = new StreamResult(pathToPublicTree);
        Transformer transformer = tFactory.newTransformer();
        transformer.transform(dom_source, out_stream);
    }

    private void getReport(NodeList rootChildren, Document documentTree) throws IOException {//урезанный вариант
        Node searchNodeByName = searchNodeByNameOnFirstLevel(rootChildren, "idTask");
        String idTask = searchNodeByName.getTextContent();
        Node root = documentTree.getDocumentElement();
        Node searchNodeById = searchNodeById(idTask, root);

        searchNodeByName = searchNodeByNameOnFirstLevel(searchNodeById.getChildNodes(), "nameNode");
        String nameNode = searchNodeByName.getTextContent();

        searchNodeByName = searchNodeByNameOnFirstLevel(searchNodeById.getChildNodes(), "timeEmployment");
        Long AllTimeEmploymentUser = calculateAllTimeEmployment(root);
        Long timeEmployment = calculateAllTimeEmployment(searchNodeById);
        Long percentageEmployment;
        if (timeEmployment == null) {
            timeEmployment = Long.decode("0");
        }
        if (AllTimeEmploymentUser == 0) {
            percentageEmployment = Long.decode("0");
        } else {
            percentageEmployment = timeEmployment / (AllTimeEmploymentUser / 100);
        }

        Document reportDocument = builder.newDocument();
        Element rootXml = reportDocument.createElement("root");
        reportDocument.appendChild(rootXml);

        Element node = reportDocument.createElement("nameTask");
        node.setTextContent(nameNode);
        rootXml.appendChild(node);

        node = reportDocument.createElement("idTask");
        node.setTextContent(idTask);
        rootXml.appendChild(node);

        node = reportDocument.createElement("timeEmploymentTask");
        node.setTextContent(String.valueOf(timeEmployment));
        rootXml.appendChild(node);

        node = reportDocument.createElement("percentageEmployment");
        node.setTextContent(String.valueOf(percentageEmployment));
        rootXml.appendChild(node);

        out.writeObject(reportDocument);
        out.flush();
    }
}
