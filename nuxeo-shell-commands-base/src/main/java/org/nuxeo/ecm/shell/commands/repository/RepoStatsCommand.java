package org.nuxeo.ecm.shell.commands.repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.client.NuxeoClient;
import org.nuxeo.ecm.shell.CommandLine;

public class RepoStatsCommand extends AbstractCommand {

    private static class StatInfo {
        private final Map<String, Long> docsPerTypes;

        private long totalBlobSize = 0;
        private long totalBlobNb = 0;

        StatInfo() {
            docsPerTypes = new ConcurrentHashMap<String, Long>();
        }

        public Map<String, Long> getDocsPerType() {
            return docsPerTypes;
        }

        public synchronized void addDoc(String type) {
            Long counter = docsPerTypes.get(type);
            if (counter == null) {
                counter = 1L;
            } else {
                counter += 1;
            }
            docsPerTypes.put(type, counter);
        }

        public synchronized void addBlob(long size) {
            totalBlobSize += size;
            totalBlobNb += 1;
        }

        public long getTotalNbDocs() {
            long total = 0;
            for (String k : docsPerTypes.keySet()) {
                total += docsPerTypes.get(k);
            }
            return total;
        }

        public long getTotalBlobSize() {
            return totalBlobSize;
        }

        public long getTotalBlobNumber() {
            return totalBlobNb;
        }

    }

    public static StatInfo info;
    protected Boolean includeBlob = true;

    protected class StatTask implements Runnable {
        private CoreSession session;

        private DocumentModel rootDoc;

        protected StatTask(DocumentModel rootDoc) throws Exception {
            session = NuxeoClient.getInstance().openRepository();
            this.rootDoc = rootDoc;
        }

        public void dispose() {
            try {
                CoreInstance.getInstance().close(session);
            } catch (Exception e) {
                e.printStackTrace();// TODO
            }
        }

        public synchronized void run() {
            try {
                recurse(rootDoc);
            } catch (ClientException e) {
                try {
                    CoreInstance.getInstance().close(session);
                } catch (Exception e1) {
                    e1.printStackTrace();// TODO
                }
            }
        }

        private StatTask getNextTask(DocumentModel root) {
            if (pool.getQueue().size() > 1) {
                return null;
            }
            StatTask newTask;
            try {
                newTask = new StatTask(root);
            } catch (Exception e) {
                return null;
            }
            return newTask;
        }

        private void recurse(DocumentModel doc) throws ClientException {
            fetchInfoFromDoc(session, doc);
            if (doc.isFolder()) {
                for (DocumentModel child : session.getChildren(doc.getRef())) {
                    if (child.isFolder()) {
                        StatTask newTask = getNextTask(child);
                        if (newTask != null) {
                            pool.execute(newTask);
                        } else {
                            recurse(child);
                        }
                    } else {
                        fetchInfoFromDoc(session, child);
                    }
                }
            }
        }

        private void fetchInfoFromDoc(CoreSession session, DocumentModel doc)
                throws UnsupportedOperationException, ClientException {
            RepoStatsCommand.info.addDoc(doc.getType());
            // XXX check versions too
            if (includeBlob && doc.hasSchema("file")) {
                //Long size = (Long) doc.getPart("file").resolvePath("content/length").getValue();
                Long size = (Long) doc.getPart("file").get("content").get(
                        "length").getValue();
                if (size != null) {
                    RepoStatsCommand.info.addBlob(size);
                }
                /*Blob blob = (Blob) doc.getProperty("file", "content");
                if (blob!=null)
                    RepoStatsCommand.info.addBlob(blob.getLength());*/
            }
        }
    }

    private void printHelp() {
        System.out.println("");
        System.out.println("Synthax: repostats doc_path");
        System.out.println(
                " doc_path: reprository path from where stats must be gathered");
        System.out.println(
                " [nbThreads]: defines the number of cucurrent threads (optional, default=5)");
        System.out.println(
                " [includeBlobs] : Boolean indicating if Blob data should introspcetde (optional, default=True)");
    }


    protected static ThreadPoolExecutor pool;

    @Override
    public void run(CommandLine cmdLine) throws Exception {

        String[] elements = cmdLine.getParameters();

        if (elements.length == 0) {
            System.out.println(
                    "SYNTAX ERROR: the repostats command must take at least one argument");
            printHelp();
            return;
        }

        if ("help".equals(elements[0])) {
            printHelp();
            return;
        }

        DocumentModel root = null;
        if (elements.length >= 1) {
            Path path = new Path(elements[0]);
            try {
                root = context.fetchDocument(path);
            } catch (Exception e) {
                System.err.println("Failed to retrieve the given folder");
                return;
            }
        }

        int nbThreads = 5;
        if (elements.length >= 2) {
            try {
                nbThreads = Integer.parseInt(elements[1]);
            } catch (Exception e) {
                System.err.println("Failed to parse number of threads");
                return;
            }
        } else {
            System.out.println(" Using default Thread number : " + nbThreads);
        }

        if (elements.length >= 3) {
            try {
                includeBlob = Boolean.parseBoolean(elements[2]);
            } catch (Exception e) {
                System.err.println("Failed to parse the includeBlob parameter");
                return;
            }
        } else {
            includeBlob = true;
        }


        info = new StatInfo();

        StatTask task = new StatTask(root);

        pool = new ThreadPoolExecutor(nbThreads, nbThreads, 500L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(100));

        long t0 = System.currentTimeMillis();

        pool.execute(task);

        Thread.sleep(100);

        int activeTasks = pool.getActiveCount();
        int oldActiveTasks = 0;
        long tprint = t0;
        while (activeTasks > 0) {
            Thread.sleep(200);
            activeTasks = pool.getActiveCount();
            if (oldActiveTasks != activeTasks || System.currentTimeMillis() - tprint > 2000) {
                oldActiveTasks = activeTasks;
                tprint = System.currentTimeMillis();
                System.out.print("Please wait while browsing repository ...");
                System.out.print(
                        " - (" + info.getTotalNbDocs() + " docs read so far)");
                System.out.println(" - (" + activeTasks + " threads working)");
            }
        }

        System.out.println(
                "Total number of documents : " + info.getTotalNbDocs());
        Map<String, Long> docsPerType = info.getDocsPerType();
        for (String type : docsPerType.keySet()) {
            System.out.println(
                    "   number of " + type + " docs : " + docsPerType.get(
                            type));
        }
        if (includeBlob) {
            System.out.println(
                    "Total number of blobs : " + info.getTotalBlobNumber());
            System.out.println(
                    "Total size of blobs (MB) : " + ((float) info.getTotalBlobSize() / (1024 * 1024)));
            System.out.println(
                    " average blob size (KB) :" + ((float) info.getTotalBlobSize() / 1024) / info.getTotalBlobNumber());
        }

        long t1 = System.currentTimeMillis();
        System.out.println(
                "Repository performance during stats was " + 1000 * ((float) info.getTotalNbDocs()) / (t1 - t0) + " doc/s");
    }

}
