package org.nuxeo.ecm.shell.commands.repository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.client.NuxeoClient;
import org.nuxeo.runtime.services.streaming.FileSource;
import org.nuxeo.runtime.services.streaming.StreamSource;

public class ThreadedImportTask implements Runnable {

    private static final Log log = LogFactory.getLog(ThreadedImportTask.class);

    private static final SimpleDateFormat LOGDATEFORMAT = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);

    private static int taskCounter = 0;

    private boolean isRunning = false;

    private long uploadedFiles = 0;

    private long uploadedKO;

    private int batchSize;

    private CoreSession session;

    private DocumentModel rootDoc;

    private File rootFile;

    private File logFile = null;

    private FileOutputStream logFileOut = null;

    private Boolean skipContainerCreation = false;

    private Boolean isRootTask = false;

    private String taskId = null;

    private synchronized static int getNextTaskId() {
        taskCounter += 1;
        return taskCounter;
    }

    public ThreadedImportTask(CoreSession session, int batchSize)
            throws Exception {
        if (session == null) {
            // TODO - open new session
        }
        logFile = new File(
                "import" + Thread.currentThread().getName() + ".log");
        if (session == null) {
            session = NuxeoClient.getInstance().openRepository();
        }
        this.session = session;
        this.batchSize = batchSize;
        uploadedFiles = 0;
        taskId = "T" + getNextTaskId();
    }

    public ThreadedImportTask(File rootFile, DocumentModel rootDoc)
            throws Exception {
        this(null, rootFile, rootDoc);
    }

    public ThreadedImportTask(CoreSession session, File rootFile,
            DocumentModel rootDoc) throws Exception {
        this(session, 50);
        this.rootFile = rootFile;
        this.rootDoc = rootDoc;
    }

    protected void commit() throws Exception {
        commit(false);
    }

    protected void commit(boolean force) throws Exception {
        uploadedFiles++;
        if (uploadedFiles % 10 == 0) {
            //fslog(uploadedFiles + " doc created ...[" + Thread.currentThread().getName() + "]");
            MTFSImportCommand.addCreatedDoc(taskId, uploadedFiles);
        }

        if (uploadedFiles % batchSize == 0 || force) {
            fslog("Comiting Core Session after " + uploadedFiles
                    + " files", true);
            session.save();
        }
    }

    public DocumentModel createDirectory(DocumentModel parent, File file)
            throws Exception {
        String docType = "Folder";
        String name = getValidNameFromFileName(file.getName());

        Map<String, Object> options = new HashMap<String, Object>();
        options.put("BLOCK_JMS_PRODUCING", true);
        DocumentModel doc = session.createDocumentModel(docType, options);
        doc.setPathInfo(parent.getPathAsString(), name);
        doc.setProperty("dublincore", "title", file.getName());
        doc.putContextData("BLOCK_JMS_PRODUCING", true);
        doc.putContextData("BLOCK_SYNC_INDEXING", true);

        fslog("Creating Folder " + name + " at "
                + parent.getPathAsString(), true);
        doc = session.createDocument(doc);
        // save session if needed
        commit();
        return doc;
    }

    public DocumentModel createFile(DocumentModel parent, File file)
            throws Exception {
        if (!file.exists()) {
            fslog("non readable file : " + file.getName());
            return null;
        }
        String mimeType = getMimeType(file);
        String docType = "File";
        String name = getValidNameFromFileName(file.getName());
        String fileName = file.getName();

        Map<String, Object> options = new HashMap<String, Object>();
        options.put("BLOCK_JMS_PRODUCING", true);
        DocumentModel doc = session.createDocumentModel(docType, options);
        doc.putContextData("BLOCK_JMS_PRODUCING", true);
        doc.putContextData("BLOCK_SYNC_INDEXING", true);
        doc.setPathInfo(parent.getPathAsString(), name);
        doc.setProperty("dublincore", "title", file.getName());
        doc.setProperty("file", "filename", fileName);
        // create a stream source - use the PlatformService.xml file for
        // example
        StreamSource src = new FileSource(file);
        // to use the streaming service we need to use StreamingBlob
        // implementation
        StreamingBlob streamingBlob = new StreamingBlob(src, mimeType);
        doc.setProperty("file", "content", streamingBlob);
        // doc = session.saveDocument(doc);
        long kbSize = file.length() / 1024;
        fslog("Creating doc " + name + " at " + parent.getPathAsString()
                + " with file " + fileName + " of size " + kbSize + "KB",
                true);
        doc = session.createDocument(doc);

        uploadedKO += kbSize;
        // save session if needed
        commit();
        return doc;
    }

    protected ThreadedImportTask getTask(DocumentModel parent, File file) {
        if (isRootTask) {
            // don't fork Root thread on first folder
            isRootTask = false;
            return null;
        }

        if (uploadedFiles < (batchSize / 3)) {
            return null;
        }

        int scheduledTasks = MTFSImportCommand.getExecutor().getQueue().size();
        if (scheduledTasks >= 5) {
            return null;
        } else {
            ThreadedImportTask newTask;
            try {
                newTask = new ThreadedImportTask(file, parent);
                //System.out.println("Created new Task with Session" + newTask.session.getSessionId());
            } catch (Exception e) {
                System.out.println("************ERROR" + e.getMessage());
                return null;
            }
            //System.out.println("Created new Thread on " + file.getAbsolutePath());
            newTask.setBatchSize(this.getBatchSize());
            newTask.setSkipContainerCreation(true);
            return newTask;
        }
    }

    protected void upload(DocumentModel parent, File file) throws Exception {
        if (file.isDirectory()) {
            DocumentModel folder;
            Boolean newThread = false;
            if (skipContainerCreation) {
                folder = parent;
                skipContainerCreation = false;
                newThread = true;
                //System.out.println("Skipping container creation");
            } else {
                folder = createDirectory(parent, file);
            }
            File[] files = file.listFiles();
            if (files.length > 0) {
                // get a new TaskImporter if available to start
                // processing the sub-tree
                ThreadedImportTask task = null;
                if (!newThread) {
                    task = getTask(folder, file);
                }
                if (task != null) {
                    //force comit before starting new thread
                    session.save();
                    MTFSImportCommand.getExecutor().execute(task);
                } else {
                    for (File f : file.listFiles()) {
                        upload(folder, f);
                    }
                }
            }
        } else {
            createFile(parent, file);
        }
    }

    public void setInputFile(File rootFile) {
        this.rootFile = rootFile;
    }

    public void setTargetFolder(DocumentModel rootDoc) {
        this.rootDoc = rootDoc;
    }

    /** Modify this to get right mime types depending on the file input */
    protected String getMimeType(File file) {
        // Dummy MimeType detection : plug nuxeo Real MimeType service to
        // have better results
        String fileName = file.getName();

        if (fileName == null) {
            return "application/octet-stream";
        } else if (fileName.endsWith(".doc")) {
            return "application/msword";
        } else if (fileName.endsWith(".xls")) {
            return "application/vnd.ms-excel";
        } else if (fileName.endsWith(".ppt")) {
            return "application/vnd.ms-powerpoint";
        } else if (fileName.endsWith(".txt")) {
            return "text/plain";
        } else if (fileName.endsWith(".html")) {
            return "text/html";
        } else if (fileName.endsWith(".xml")) {
            return "text/xml";
        } else if (fileName.endsWith(".jpg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        } else if (fileName.endsWith(".odt")) {
            return "application/vnd.oasis.opendocument.text";
        } else if (fileName.endsWith(".zip")) {
            return "application/zip";
        } else {
            return "application/octet-stream";
        }
    }

    // TODO isRunning is not yet handled correctly
    public boolean isRunning() {
        synchronized (this) {
            return isRunning;
        }
    }

    public synchronized void run() {
        synchronized (this) {

            if (isRunning) {
                throw new IllegalStateException("Task already running");
            }
            isRunning = true;
            if (rootDoc == null || rootFile == null) {
                isRunning = false;
                throw new IllegalArgumentException(
                        "target folder and source file must be specified");
            }
        }
        try {
            upload(rootDoc, rootFile);
            session.save();
            MTFSImportCommand.addCreatedDoc(taskId, uploadedFiles);
            //fslog("doc upload terminated");
            CoreInstance.getInstance().close(session);
        } catch (Exception e) {
            log.error("Error during import", e);
        } finally {
            synchronized (this) {
                isRunning = false;
            }
        }
    }

    protected String getValidNameFromFileName(String fileName) {
        String name = IdUtils.generateId(fileName, "-", true, 100);
        name = name.replace("'", "");
        name = name.replace("(", "");
        name = name.replace(")", "");
        name = name.replace("+", "");
        return name;
    }

    public void dispose() {
        try {
            CoreInstance.getInstance().close(session);
        } catch (Exception e) {
            e.printStackTrace();// TODO
        }
    }

    /** Logging hacks * */
    private void fslog(String msg) {
        fslog(msg, false);
    }

    // This should be done with log4j but I did not find a way to configure it
    // the way I wanted ...
    private void fslog(String msg, boolean debug) {
        log.info(msg);

        try {
            if (logFileOut == null) {
                logFile.createNewFile();
                logFileOut = new FileOutputStream(logFile);
            }
            String strMessage = LOGDATEFORMAT.format(new Date()) + " -- " + msg
                    + "\n";
            logFileOut.write(strMessage.getBytes());
        } catch (IOException e) {
            log.error("Unable to log in file ", e);
        }

        if (!debug) {
            System.out.println(msg);
        }
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void setSkipContainerCreation(Boolean skipContainerCreation) {
        this.skipContainerCreation = skipContainerCreation;
    }

    public void setRootTask() {
        isRootTask = true;
        taskCounter = 0;
        taskId = "T0";
    }

}
