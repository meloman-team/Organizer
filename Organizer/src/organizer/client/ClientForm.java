/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package organizer.client;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
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
import organizer.classes.Task;

/**
 *
 * @author Ilya
 */
public class ClientForm extends javax.swing.JFrame {

    /**
     * Creates new form Client2
     *
     * @param socket
     * @param out
     * @param in
     * @param userName
     * @throws javax.xml.parsers.ParserConfigurationException
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     * @throws javax.xml.transform.TransformerException
     */
    public ClientForm(ObjectOutputStream out, ObjectInputStream in, String userName)
            throws ParserConfigurationException, IOException, ClassNotFoundException, TransformerException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        builder = factory.newDocumentBuilder();
        this.in = in;
        this.out = out;
        this.userName = userName;
        initComponents(userName);
    }

    private void initComponents(String userName) throws IOException, ClassNotFoundException, TransformerException {

        jScrollUserPane = new javax.swing.JScrollPane();
        jUserTree = new javax.swing.JTree();
        jScrollPublicPane = new javax.swing.JScrollPane();
        jPublicTree = new javax.swing.JTree();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Organizer");
        setResizable(false);

        mouse(jUserTree);
        mouse(jPublicTree);
        initTree(userName);
        updatePublicTree();
        jScrollUserPane.setViewportView(jUserTree);

        jScrollPublicPane.setViewportView(jPublicTree);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollUserPane, javax.swing.GroupLayout.PREFERRED_SIZE, 284, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPublicPane, javax.swing.GroupLayout.PREFERRED_SIZE, 282, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jScrollUserPane)
                .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPublicPane, javax.swing.GroupLayout.PREFERRED_SIZE, 429, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
        );

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    jUserTree.setSelectionPath(new TreePath(launchedItemUserTree.getPath()));
                    stopItem(jUserTree);
                    if (launchedItemPublicTree != null) {
                        jPublicTree.setSelectionPath(new TreePath(launchedItemPublicTree.getPath()));
                        stopItem(jPublicTree);
                    }
                } catch (IOException | ParserConfigurationException | TransformerException ex) {
                    Logger.getLogger(ClientForm.class.getName()).log(Level.SEVERE, null, ex);
                }
                System.exit(0);
            }
        });
        pack();
    }

    private final ObjectOutputStream out;
    private ObjectInputStream in;
    DocumentBuilder builder;
    DefaultMutableTreeNode launchedItemUserTree;
    DefaultMutableTreeNode launchedItemPublicTree;
    DefaultMutableTreeNode notJob;
    String userName;

    public void updatePublicTree() throws IOException, ClassNotFoundException {
        org.w3c.dom.Document document = builder.newDocument();

        Element taskXml = document.createElement("root");
        document.appendChild(taskXml);

        Element element = document.createElement("task");
        element.setTextContent("downloadPublicTree");
        taskXml.appendChild(element);

        sendDocumentToServer(document);//java.io.StreamCorruptedException: invalid type code: AC

        Document readDocument = adoptDocumentFromServer();
        NodeList rootChildren = readDocument.getDocumentElement().getChildNodes();
        String task = rootChildren.item(0).getTextContent();

        if (task.equals("notDownloadTree")) {
            JOptionPane.showMessageDialog(null, "Общего дерева на сервере не существует!");
        } else {
            DefaultMutableTreeNode root = createPublicTree(rootChildren);
            jPublicTree.setModel(new javax.swing.tree.DefaultTreeModel(root));
        }

        RenderPublicTree renderer = new RenderPublicTree();
        jPublicTree.setCellRenderer(renderer);
        jPublicTree.repaint();
    }

    private void sendDocumentToServer(Document document) throws IOException {
        out.writeObject(document);
        out.flush();
    }

    private Document adoptDocumentFromServer() throws IOException, ClassNotFoundException {
        return (Document) in.readObject();
    }

    private void initTree(String userName)
            throws IOException, ClassNotFoundException, TransformerConfigurationException,
            TransformerException {

        org.w3c.dom.Document document = builder.newDocument();

        Element taskXml = document.createElement("root");
        document.appendChild(taskXml);

        Element element = document.createElement("task");
        element.setTextContent("downloadTree");
        taskXml.appendChild(element);

        element = document.createElement("userName");
        element.setTextContent(userName);
        taskXml.appendChild(element);

        sendDocumentToServer(document);

        Document readDocument = adoptDocumentFromServer();

        NodeList rootChildren = readDocument.getDocumentElement().getChildNodes();
        String task = rootChildren.item(0).getTextContent();

        if (task.equals("notDownloadTree")) {
            Task newUser = new Task(userName);
            DefaultMutableTreeNode root = new DefaultMutableTreeNode("user");
            root.setUserObject(newUser);

            Task newTask = new Task("Не работа");
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode("task");
            newNode.setUserObject(newTask);
            root.add(newNode);

            notJob = newNode;

            jUserTree.setModel(new javax.swing.tree.DefaultTreeModel(root));

            document = builder.newDocument();
            Element rootXml = document.createElement("root");
            document.appendChild(rootXml);

            Element node = document.createElement("nameNode");
            node.setTextContent(userName);
            rootXml.appendChild(node);

            node = document.createElement("idNode");
            node.setTextContent(String.valueOf(newUser.getId()));
            rootXml.appendChild(node);

            node = document.createElement("lastDate");
            rootXml.appendChild(node);

            node = document.createElement("timeEmployment");
            rootXml.appendChild(node);

            element = document.createElement("cildNode");
            rootXml.appendChild(element);
            node = document.createElement("nameNode");
            node.setTextContent("Не работа");
            element.appendChild(node);

            node = document.createElement("idNode");
            node.setTextContent(String.valueOf(newTask.getId()));
            element.appendChild(node);

            node = document.createElement("lastDate");
            element.appendChild(node);

            node = document.createElement("timeEmployment");
            element.appendChild(node);

            sendDocumentToServer(document);

            launchedItemUserTree = notJob;
        } else {

            DefaultMutableTreeNode root = createTree(rootChildren);

            jUserTree.setModel(new javax.swing.tree.DefaultTreeModel(root));

            jUserTree.setSelectionRow(1);
            notJob = (DefaultMutableTreeNode) jUserTree.getLastSelectedPathComponent();
            launchedItemUserTree = notJob;
        }
        TestRender renderer = new TestRender();
        jUserTree.setCellRenderer(renderer);
    }

    private DefaultMutableTreeNode createTree(NodeList children) {
        Node item = children.item(0);
        String nameNode = item.getTextContent();
        item = children.item(1);
        int userId = Integer.parseInt(item.getTextContent());

        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("user");
        rootNode.setUserObject(new Task(userId, nameNode));

        int length = children.getLength();
        if (length > 4) {
            for (int i = 4; i < length; i++) {
                createTree(children.item(i).getChildNodes(), rootNode);
            }
        }
        return rootNode;
    }

    private void createTree(NodeList children, DefaultMutableTreeNode root) {
        Node item = children.item(0);
        String textContent = item.getTextContent();
        item = children.item(1);
        int userId = Integer.parseInt(item.getTextContent());

        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode("user");
        newNode.setUserObject(new Task(userId, textContent));
        root.add(newNode);

        int length = children.getLength();
        if (length > 4) {
            for (int i = 4; i < length; i++) {
                createTree(children.item(i).getChildNodes(), newNode);
            }
        }
    }

    private DefaultMutableTreeNode createPublicTree(NodeList children) {
        Node item = children.item(0);
        String nameTask = item.getTextContent();
        item = children.item(1);
        int taskId = Integer.parseInt(item.getTextContent());
        item = children.item(4);
        String performanceNameUser = item.getTextContent();

        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("user");
        Task task = new Task(taskId, nameTask);
        task.setPerformanceNameUser(performanceNameUser);
        rootNode.setUserObject(task);

        int length = children.getLength();
        if (length > 5) {
            for (int i = 5; i < length; i++) {
                createPublicTree(children.item(i).getChildNodes(), rootNode);
            }
        }
        return rootNode;
    }

    private void createPublicTree(NodeList children, DefaultMutableTreeNode root) {
        Node item = children.item(0);
        String textContent = item.getTextContent();
        item = children.item(1);
        int userId = Integer.parseInt(item.getTextContent());
        item = children.item(4);
        String performanceNameUser = item.getTextContent();

        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode("user");
        Task task = new Task(userId, textContent);
        task.setPerformanceNameUser(performanceNameUser);
        newNode.setUserObject(task);
        if (performanceNameUser.equals(userName)) {
            launchedItemPublicTree = newNode;
        }
        root.add(newNode);

        int length = children.getLength();
        if (length > 5) {
            for (int i = 5; i < length; i++) {
                createPublicTree(children.item(i).getChildNodes(), newNode);
            }
        }
    }

    private void mouse(JTree jTree) {
        MouseListener ml = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int selRow = jTree.getRowForLocation(e.getX(), e.getY());
                TreePath selPath = jTree.getPathForLocation(e.getX(), e.getY());
                jTree.setSelectionPath(selPath);
//                if (e.getButton() == e.BUTTON1) {
//                    try {
//                        updatePublicTree();
//                    } catch (IOException ex) {
//                        Logger.getLogger(ClientForm.class.getName()).log(Level.SEVERE, null, ex);
//                    } catch (ClassNotFoundException ex) {
//                        Logger.getLogger(ClientForm.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//                }
                if (selRow != -1) {
                    if (e.getButton() == e.BUTTON3) {

                        JMenuItem start = new JMenuItem("Начать выполнение");
                        start.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                try {
                                    startItem(jTree);
                                } catch (IOException | ParserConfigurationException | TransformerException ex) {
                                    Logger.getLogger(ClientForm.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        });

                        JMenuItem stop = new JMenuItem("Завершить выполнение");
                        stop.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                try {
                                    stopItem(jTree);
                                } catch (IOException | ParserConfigurationException | TransformerException ex) {
                                    Logger.getLogger(ClientForm.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        });

                        JMenuItem add = new JMenuItem("Добавить");
                        add.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                try {
                                    addNewItem(jTree);
                                } catch (IOException ex) {
                                    Logger.getLogger(ClientForm.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        });

                        JMenuItem getReport = new JMenuItem("Посмотреть отчет");
                        getReport.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                try {
                                    getReport(selPath, jTree);
                                } catch (IOException ex) {
                                    System.err.println("Проблемы с сокетом: " + ex.getMessage());
                                } catch (ClassNotFoundException ex) {
                                    Logger.getLogger(ClientForm.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        });

                        JMenuItem rename = new JMenuItem("Переименовать");
                        rename.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                try {
                                    renameItem(jTree);
                                } catch (IOException | ParserConfigurationException | TransformerException ex) {
                                    Logger.getLogger(ClientForm.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        });

                        JMenuItem remove = new JMenuItem("Удалить");
                        remove.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                try {
                                    removeItem(jTree);
                                } catch (IOException ex) {
                                    Logger.getLogger(ClientForm.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        });

                        JMenuItem update = new JMenuItem("обновить");
                        update.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                try {
                                    updatePublicTree();
                                } catch (IOException ex) {
                                    Logger.getLogger(ClientForm.class.getName()).log(Level.SEVERE, null, ex);
                                } catch (ClassNotFoundException ex) {
                                    Logger.getLogger(ClientForm.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        });
                        JPopupMenu popupMenu = new JPopupMenu();
                        popupMenu.add(start);
                        popupMenu.add(stop);
                        popupMenu.add(add);
                        popupMenu.add(getReport);
                        popupMenu.add(rename);
                        popupMenu.add(remove);
                        popupMenu.add(update);

                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        };
        jTree.addMouseListener(ml);
    }

    /**
     *
     * @param jTree
     * @throws IOException
     * @throws javax.xml.parsers.ParserConfigurationException
     * @throws javax.xml.transform.TransformerConfigurationException
     */
    public void renameItem(JTree jTree)
            throws IOException, ParserConfigurationException, TransformerConfigurationException,
            TransformerException {
        Object obj = jTree.getLastSelectedPathComponent();
        DefaultMutableTreeNode sel = (DefaultMutableTreeNode) obj;
        if (!sel.isRoot()) {
            Task task = (Task) sel.getUserObject();
            if (!task.getName().equals("не работа")) {

                AddNewItem dialog = new AddNewItem(this, true);
                String stringNewItem = dialog.getString();

                if (stringNewItem.equals("")) {
                    task.setName(stringNewItem);

                    Document document = builder.newDocument();
                    Element taskXml = document.createElement("root");
                    document.appendChild(taskXml);

                    Element element = document.createElement("task");
                    if (jTree == jUserTree) {
                        element.setTextContent("renameItem");
                    } else {
                        element.setTextContent("renamePublicItem");
                    }
                    taskXml.appendChild(element);

                    element = document.createElement("nameTask");
                    element.setTextContent(stringNewItem);
                    taskXml.appendChild(element);

                    element = document.createElement("idTask");
                    element.setTextContent(String.valueOf(task.getId()));
                    taskXml.appendChild(element);

                    sendDocumentToServer(document);
                }
            } else {
                JOptionPane.showMessageDialog(null, "не работу переименовать нельзя!");
            }
        } else {
            JOptionPane.showMessageDialog(null, "Корень переименовать нельзя!");
        }
    }

    /**
     *
     * @param jTree
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws TransformerConfigurationException
     * @throws TransformerException
     */
    public void startItem(JTree jTree)
            throws IOException, ParserConfigurationException, TransformerConfigurationException,
            TransformerException {
        Object obj = jTree.getLastSelectedPathComponent();
        DefaultMutableTreeNode sel = (DefaultMutableTreeNode) obj;
        if (!sel.isRoot()) {
            if (launchedItemUserTree != sel) {

                if (jTree == jUserTree && launchedItemUserTree != null) {
                    jTree.setSelectionPath(new TreePath(launchedItemUserTree.getPath()));
                    stopItem(jTree);
                }
                if (jTree == jPublicTree && launchedItemPublicTree != null) {
                    jTree.setSelectionPath(new TreePath(launchedItemPublicTree.getPath()));
                    stopItem(jTree);
                }

                Task task = (Task) sel.getUserObject();

                Document document = builder.newDocument();
                Element taskXml = document.createElement("root");
                document.appendChild(taskXml);

                Element element = document.createElement("task");
                if (jTree == jUserTree) {
                    element.setTextContent("startItem");
                } else {
                    //проверить не запушен ли
                    element.setTextContent("startPublicItem");
                }
                taskXml.appendChild(element);

                element = document.createElement("idTask");
                element.setTextContent(String.valueOf(task.getId()));
                taskXml.appendChild(element);

                if (jTree == jPublicTree) {
                    element = document.createElement("userName");
                    element.setTextContent(userName);
                    taskXml.appendChild(element);
                    task.setPerformanceNameUser(userName);
                }

                sendDocumentToServer(document);

                if (jTree == jUserTree) {
                    launchedItemUserTree = sel;
                } else {
                    launchedItemPublicTree = sel;
                }
            }
        } else {
            JOptionPane.showMessageDialog(null, "Корень запустить нельзя!");
        }
    }

    /**
     *
     * @param jTree
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws TransformerConfigurationException
     * @throws TransformerException
     */
    public void stopItem(JTree jTree)
            throws IOException, ParserConfigurationException, TransformerConfigurationException,
            TransformerException {
        if (launchedItemUserTree != null) {
            Object obj = jTree.getLastSelectedPathComponent();
            DefaultMutableTreeNode sel = (DefaultMutableTreeNode) obj;
            Task task = (Task) sel.getUserObject();

            Document document = builder.newDocument();
            Element taskXml = document.createElement("root");
            document.appendChild(taskXml);

            Element element = document.createElement("task");
            if (jTree == jUserTree) {
                element.setTextContent("stopItem");
            } else {
                element.setTextContent("stopPublicItem");
            }
            taskXml.appendChild(element);

            element = document.createElement("idTask");
            element.setTextContent(String.valueOf(task.getId()));
            taskXml.appendChild(element);

            sendDocumentToServer(document);

            if (jTree == jPublicTree) {
                task.setPerformanceNameUser("");
            }
            if (jTree == jUserTree) {
                launchedItemUserTree = notJob;
                //запуск не работы
                if (sel != notJob) {
                    jTree.setSelectionRow(1);
                    startItem(jTree);
                }
            }
        }
    }

    /**
     *
     * @param selPath
     * @param jTree
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void getReport(TreePath selPath, JTree jTree) throws IOException, ClassNotFoundException {
        Object obj = jTree.getLastSelectedPathComponent();
        DefaultMutableTreeNode sel = (DefaultMutableTreeNode) obj;
        Task task = (Task) sel.getUserObject();

        Document document = builder.newDocument();
        Element taskXml = document.createElement("root");
        document.appendChild(taskXml);

        Element element = document.createElement("task");
        if (jTree == jUserTree) {
            element.setTextContent("getReport");
        } else {
            element.setTextContent("getPublicReport");
        }
        taskXml.appendChild(element);

        element = document.createElement("idTask");
        element.setTextContent(String.valueOf(task.getId()));
        taskXml.appendChild(element);

        sendDocumentToServer(document);

        Document reportDocument = adoptDocumentFromServer();

        java.awt.EventQueue.invokeLater(() -> {
            new ReportForm(reportDocument).setVisible(true);
        });
    }

    /**
     *
     * @param jTree
     * @throws IOException
     */
    public void removeItem(JTree jTree) throws IOException {
        DefaultTreeModel model = (DefaultTreeModel) jTree.getModel();
        Object obj = jTree.getLastSelectedPathComponent();
        DefaultMutableTreeNode sel = (DefaultMutableTreeNode) obj;
        if (!sel.isRoot()) {
            Task task = (Task) sel.getUserObject();
            if (!task.getName().equals("не работа")) {

                Task removeTask = (Task) sel.getUserObject();
                int id = removeTask.getId();

                Document document = builder.newDocument();
                Element taskXml = document.createElement("root");
                document.appendChild(taskXml);

                Element element = document.createElement("task");
                if (jTree == jUserTree) {
                    element.setTextContent("removeItem");
                } else {
                    element.setTextContent("removePublicItem");
                }
                taskXml.appendChild(element);

                element = document.createElement("idTask");
                element.setTextContent(String.valueOf(removeTask.getId()));
                taskXml.appendChild(element);

                StreamResult out_stream;
                TransformerFactory tFactory = TransformerFactory.newInstance();
                Transformer transformer;
                try {
                    transformer = tFactory.newTransformer();

                    DOMSource dom_source = new DOMSource(document);
                    out_stream = new StreamResult("src\\organizer\\xml.xml");//StreamSource
                    transformer = tFactory.newTransformer();
                    transformer.transform(dom_source, out_stream);
                } catch (TransformerConfigurationException ex) {
                    Logger.getLogger(ClientForm.class.getName()).log(Level.SEVERE, null, ex);
                } catch (TransformerException ex) {
                    Logger.getLogger(ClientForm.class.getName()).log(Level.SEVERE, null, ex);
                }

                sendDocumentToServer(document);

                model.removeNodeFromParent(sel);
            } else {
                JOptionPane.showMessageDialog(null, "не работу удалить нельзя!");
            }
        } else {
            JOptionPane.showMessageDialog(null, "Корень удалить нельзя!");
        }
    }

    /**
     *
     * @param jTree
     * @throws IOException
     */
    public void addNewItem(JTree jTree) throws IOException {
        DefaultTreeModel model = (DefaultTreeModel) jTree.getModel();
        Object obj = jTree.getLastSelectedPathComponent();
        if (obj != null) {
            DefaultMutableTreeNode sel = (DefaultMutableTreeNode) obj;

            AddNewItem dialog = new AddNewItem(this, true);
            String stringNewItem = dialog.getString();
            if (stringNewItem != null && !stringNewItem.equals("")) {

                DefaultMutableTreeNode tmp = new DefaultMutableTreeNode(stringNewItem);

                Task newTask;
                do {
                    newTask = new Task(stringNewItem);
                } while (searchId(newTask.getId()));//проверка уникальности id

                tmp.setUserObject(newTask);
                model.insertNodeInto(tmp, sel, sel.getChildCount());

                jTree.expandPath(new TreePath(sel.getPath()));

                Task parentTask = (Task) sel.getUserObject();
                int id = parentTask.getId();

                Document document = builder.newDocument();
                Element taskXml = document.createElement("root");
                document.appendChild(taskXml);

                Element element = document.createElement("task");
                if (jTree == jUserTree) {
                    element.setTextContent("addNewItem");
                } else {
                    element.setTextContent("addNewItemPublicTree");
                }
                taskXml.appendChild(element);

                element = document.createElement("idParentTask");
                element.setTextContent(String.valueOf(id));
                taskXml.appendChild(element);

                element = document.createElement("nameTask");
                element.setTextContent(stringNewItem);
                taskXml.appendChild(element);

                element = document.createElement("idTask");
                element.setTextContent(String.valueOf(newTask.getId()));
                taskXml.appendChild(element);

                sendDocumentToServer(document);
            }
        }
    }

    /**
     *
     * @param id
     * @return
     */
    public boolean searchId(int id) {
        jUserTree.setSelectionRow(0);
        DefaultMutableTreeNode sel = (DefaultMutableTreeNode) jUserTree.getLastSelectedPathComponent();
        Enumeration preorderEnumeration = sel.preorderEnumeration();
        while (preorderEnumeration.hasMoreElements()) {
            DefaultMutableTreeNode nextElement = (DefaultMutableTreeNode) preorderEnumeration.nextElement();
            Task task = (Task) nextElement.getUserObject();
            if (id == task.getId()) {
                return true;
            }
        }
        return false;
    }

    // Variables declaration - do not modify                     
    private javax.swing.JScrollPane jScrollUserPane;
    private javax.swing.JScrollPane jScrollPublicPane;
    private javax.swing.JTree jUserTree;
    private javax.swing.JTree jPublicTree;
    // End of variables declaration                   

    private class TestRender extends DefaultTreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            Component retValue;
            retValue = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (launchedItemUserTree == value) {
                setIcon(new ImageIcon("src\\organizer\\16.png"));
            } else {
                setIcon(new ImageIcon("src\\organizer\\17.png"));
            }
            return retValue;
        }
    }

    private class RenderPublicTree extends DefaultTreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            Component retValue;
            retValue = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            DefaultMutableTreeNode v = (DefaultMutableTreeNode) value;
            Task task = (Task) v.getUserObject();
            if (task.getPerformanceNameUser() == null || task.getPerformanceNameUser().equals("")) {
                setIcon(new ImageIcon("src\\organizer\\17.png"));
                return retValue;
            }
            if (task.getPerformanceNameUser().equals(userName)) {
                setIcon(new ImageIcon("src\\organizer\\16.png"));
                return retValue;
            } else {
                setIcon(new ImageIcon("src\\organizer\\18.png"));
            }
            jPublicTree.repaint();
            return retValue;

        }
    }
}
