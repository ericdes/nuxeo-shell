package org.nuxeo.ecm.shell.commands.repository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.shell.CommandLine;

public class MTFSImportCommand extends AbstractCommand {

    static ThreadPoolExecutor importTP;

    static Map<String, Long> nbCreatedDocsByThreads = new ConcurrentHashMap<String, Long>();

    private static final File logFile = new File("mtimport.log");

    private static final Log log = LogFactory.getLog(MTFSImportCommand.class);

    private static final SimpleDateFormat LOGDATEFORMAT = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);

    private static FileOutputStream logFileOut;

    private static void fslog(String msg) {
        fslog(msg, false);
    }

    // This should be done with log4j but I did not find a way to configure it
    // the way I wanted ...
    private static void fslog(String msg, boolean debug) {
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
            log.error("Unaable to log in file ", e);
        }

        if (!debug) {
            System.out.println(msg);
        }
    }

    public static ThreadPoolExecutor getExecutor() {
        return importTP;
    }

    public synchronized static void addCreatedDoc(String taskId, long nbDocs) {
        String tid = Thread.currentThread().getName();
        nbCreatedDocsByThreads.put(tid + "-" + taskId, nbDocs);
    }

    public synchronized static long getCreatedDocsCounter() {
        long counter = 0;
        for (String tid : nbCreatedDocsByThreads.keySet()) {
            Long tCounter = nbCreatedDocsByThreads.get(tid);
            if (tCounter != null) {
                counter += tCounter;
            }
        }
        return counter;
    }

    private void printHelp() {
        System.out.println("");
        System.out.println(
                "Synthax: fsimport local_file_path [remote_path] [batch_size] [nbThreads]");
    }

    @Override
    public void run(CommandLine cmdLine) throws Exception {
        nbCreatedDocsByThreads = new ConcurrentHashMap<String, Long>();
        String[] elements = cmdLine.getParameters();
        DocumentModel parent;
        if (elements.length == 0) {
            System.out.println(
                    "SYNTAX ERROR: the fsimport command must take at least one argument: fsimport local_file_path [remote_path] [batch_size] [nbThreads] ");
            printHelp();
            return;
        }

        File localFile = new File(elements[0]);
        if ("help".equals(elements[0])) {
            printHelp();
            return;
        }

        if (elements.length >= 2) {
            Path path = new Path(elements[1]);
            try {
                parent = context.fetchDocument(path);
            } catch (Exception e) {
                System.err.println("Failed to retrieve the given folder");
                return;
            }
        } else {
            parent = context.fetchDocument();
        }

        Integer batchSize = 50;
        if (elements.length >= 3) {
            try {
                batchSize = Integer.parseInt(elements[2]);
            } catch (Throwable t) {
                System.err.println("Failed to parse batch size, using default");
                batchSize = 10;
            }
        }

        Integer nbThreads = 5;
        if (elements.length >= 4) {
            try {
                nbThreads = Integer.parseInt(elements[3]);
            } catch (Throwable t) {
                System.err.println("Failed to parse nbThreads, using default");
            }
        }
        importTP = new ThreadPoolExecutor(nbThreads, nbThreads, 500L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(100));

        ThreadedImportTask rootImportTask = new ThreadedImportTask(localFile,
                parent);
        rootImportTask.setRootTask();
        long t0 = System.currentTimeMillis();
        importTP.execute(rootImportTask);
        Thread.sleep(200);
        int activeTasks = importTP.getActiveCount();
        int oldActiveTasks = 0;
        while (activeTasks > 0) {
            Thread.sleep(200);
            activeTasks = importTP.getActiveCount();
            if (oldActiveTasks != activeTasks) {
                oldActiveTasks = activeTasks;
                System.out.println(
                        "currently " + activeTasks + " active import Threads");
                long inbCreatedDocs = getCreatedDocsCounter();
                fslog(inbCreatedDocs + " docs created");
                long ti = System.currentTimeMillis();
                fslog(1000 * ((float) (inbCreatedDocs) / (ti - t0)) + " docs/s");
            }
        }
        fslog("All Threads terminated");
        long t1 = System.currentTimeMillis();
        long nbCreatedDocs = getCreatedDocsCounter();
        fslog(nbCreatedDocs + " docs created");
        fslog(1000 * ((float) (nbCreatedDocs) / (t1 - t0)) + " docs/s");
        for (String k : nbCreatedDocsByThreads.keySet()) {
            fslog(k + " --> " + nbCreatedDocsByThreads.get(k), true);
        }
    }

}
