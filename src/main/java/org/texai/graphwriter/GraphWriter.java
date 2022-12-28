/*
 * GraphWriter.java
 *
 * Created on Aug 2, 2011, 10:12:24 AM
 *
 * Description: A singleton instance of this class listens on a socket for graph-writing requests, queues them, and
 * serially emits graphs.
 *
 *
 ***************************************************************************************************************
 *
 * Install php php-gd libfreetype6 libfreetype6-dev
 * $ sudo apt install php php-gd libfreetype6 libfreetype6-dev
 *
 ***************************************************************************************************************
 *
 * Copyright (C) Aug 2, 2011, Stephen L. Reed, Texai.org.
 *
 */
package org.texai.graphwriter;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.log4j.Logger;

/**
 * A singleton instance of this class listens on socket port 14446 for
 * graph-writing requests, queues them, and serially emits graphs.
 *
 * phpsyntaxtree supports SVG images which can contain embedded hyperlinks, as a
 * future enhancement.
 *
 * @author reed
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class GraphWriter {

    // the logger, which is configured via log4j.properties to log to the file GraphWriter.log
    private static final Logger LOGGER = Logger.getLogger(GraphWriter.class);

    // the path to the graph writing server
    private static final String GRAPH_WRITER_PATH = System.getProperty("user.home") + "/GraphWriter-1.0";

    // the path to the PHP syntax tree tools
    private static final String PHP_SYNTAX_TREE_PATH = GRAPH_WRITER_PATH + "/phpsyntaxtree";

    // the listening port
    public static final int LISTENING_PORT = 14446;

    // the server thread
    private Thread serverThread;

    // the indicator to quit
    private final AtomicBoolean isQuit = new AtomicBoolean(false);

    // the ring buffer
    private RingBuffer<GraphRequest> ringBuffer;

    // the server socket
    private ServerSocket serverSocket = null;

    // the disruptor lock-free queue
    private Disruptor<GraphRequest> disruptor;

    // the graphing request thread pool
    private ExecutorService graphingRequestExecutor = Executors.newCachedThreadPool();

    // the GraphViz request indicator which files the labeledTree field otherwise used for a PHP syntax tree string
    private static final String GRAPHVIZ = "*GraphViz*";

    static {
        assert Runtime.getRuntime().availableProcessors() >= 4;
    }
    // the graph making thread pool using all process threads less three reserved for the application
    ExecutorService graphMakingExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 3);

    /**
     * Constructs a new GraphWriter instance.
     */
    public GraphWriter() {
        try {
            serverSocket = new ServerSocket();
        } catch (IOException ex) {
            LOGGER.error("Exception when creating the server socket: " + ex.getMessage());
            LOGGER.error("Exception class: " + ex.getClass().getName() + ", " + ex);
            System.exit(1);
        }
    }

    /**
     * Initializes this application.
     */
    public void initialialization() {

        //LOGGER.setLevel(org.apache.log4j.Level.DEBUG);
        Thread.currentThread().setName("main");
        LOGGER.debug("is debug logging...");
        // listens for graph requests on the server socket, and puts them into the ring buffer
        final GraphRequestFactory graphRequestFactory = new GraphRequestFactory();

        // the lock-free ring buffer
        disruptor = new Disruptor(
                graphRequestFactory, // eventFactory,
                4096, // ringBufferSize
                (ThreadFactory) DaemonThreadFactory.INSTANCE); // threadFactory);

        // the handler gets a gueued graph request from the ring buffer and executes a shell script to create the graph image
        disruptor.handleEventsWith(new GraphRequestEventHandler(this));

        disruptor.start();
        ringBuffer = disruptor.getRingBuffer();

        // start server thread
        serverThread = new Thread(new RequestServer(this));
        serverThread.setName("server");
        serverThread.start();
    }

    /**
     * Provides an initialized graph request factory for filling the ring
     * buffer.
     */
    private static class GraphRequestFactory implements EventFactory<GraphRequest> {

        /**
         * Instantiates an event object, with all memory already allocated.
         *
         * @return the graph request
         */
        @Override
        public GraphRequest newInstance() {
            return new GraphRequest(
                    "initial", // fileName,
                    "initial"); // labeledTree
        }

    }

    /**
     * Provides a graph request event handler for emptying the ring buffer.
     */
    private static class GraphRequestEventHandler implements EventHandler<GraphRequest> {

        // the parent GraphWriter instance
        private final GraphWriter graphWriter;

        /**
         * Constructs a new GraphRequestEventHandler instance.
         *
         * @param graphWriter the graph writer
         */
        GraphRequestEventHandler(final GraphWriter graphWriter) {
            //Preconditions
            assert graphWriter != null : "graphWriter must not be null";

            this.graphWriter = graphWriter;
        }

        /**
         * Callback interface to be implemented for processing events as they
         * become available in the RingBuffer
         *
         * @param graphRequest the graph request
         * @param sequence sequence of the event being processed
         * @param endOfBatch indicates end of a batch of event entries from the
         * ring buffer
         * @throws java.lang.Exception the thrown exception
         */
        @Override
        public void onEvent(
                final GraphRequest graphRequest,
                final long sequence,
                final boolean endOfBatch) throws Exception {
            //Preconditions
            assert graphRequest != null : "event must not be null";
            assert sequence >= 0 : "sequence must not be negative";

            Thread.currentThread().setName("event-handler");
            if (!graphWriter.isQuit.get()) {

                final GraphMakingRunnable graphMakingRunnable = new GraphMakingRunnable(
                        graphWriter,
                        graphRequest);

                // multiple threads make the requested graph, manually adjusted for CPU capacity
                graphWriter.graphMakingExecutor.execute(graphMakingRunnable);
            }
        }
    }

    /**
     * Provides a graph making thread.
     */
    private static class GraphMakingRunnable implements Runnable {

        // the parent GraphWriter instance
        private final GraphWriter graphWriter;

        // the graph request
        final GraphRequest graphRequest;

        GraphMakingRunnable(
                final GraphWriter graphWriter,
                final GraphRequest graphRequest) {
            //Preconditions
            assert graphWriter != null : "graphWriter must not be null";
            assert graphRequest != null : "graphRequest must not be null";

            this.graphWriter = graphWriter;
            this.graphRequest = graphRequest;
        }

        /**
         * Makes the desired graph.
         */
        @Override
        public void run() {
            // process a new entry as it becomes available.
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("processing: " + graphRequest);
            }
            switch (graphRequest.getFileName()) {
                case "quit" -> {
                    graphWriter.finalization();
                    return;
                }

                case "ignore" -> {
                    return;
                }

                default -> {
                    if (GRAPHVIZ.equals(graphRequest.getLabeledTree())) {
                        graphWriter.graphVizDiagram(graphRequest.getFileName());
                    } else {
                        graphWriter.graphPHPSyntaxTree(
                                graphRequest.getFileName(),
                                graphRequest.getLabeledTree());
                    }
                }
            }
        }

    }

    /**
     * Finalizes this application and releases its resources.
     */
    public void finalization() {
        LOGGER.info("finishing GraphWriter...");
        isQuit.set(true);
        if (serverThread != null) {
            serverThread.interrupt();
        }
        try {
            serverSocket.close();
        } catch (IOException ex) {
        }
        graphingRequestExecutor.shutdown();
        LOGGER.info("GraphWriter shutdown.");
        System.exit(0);
    }

    /**
     * Provides a request server that listens on a certain port for graphing
     * requests, and then queues them.
     */
    private static class RequestServer implements Runnable {

        // the graph writer
        private final GraphWriter graphWriter;

        /**
         * Constructs a new RequestServer instance.
         *
         * @param graphWriter the graph writer
         */
        RequestServer(final GraphWriter graphWriter) {
            //Preconditions
            assert graphWriter != null : "graphWriter must not be null";

            this.graphWriter = graphWriter;
        }

        /**
         * Executes this request server.
         */
        @Override
        public void run() {
            // listen on the socket
            try {
                try {
                    final SocketAddress socketAddress = new InetSocketAddress(
                            InetAddress.getLoopbackAddress(), // ip,
                            LISTENING_PORT);
                    graphWriter.serverSocket.bind(socketAddress);
                } catch (IOException ex) {
                    LOGGER.error("ServerSocket exception: " + ex.getMessage());
                    System.exit(1);
                }
                assert graphWriter.serverSocket != null;
                LOGGER.info("  listening for connections on " + graphWriter.serverSocket.getLocalPort() + "...");

                // handle client graphing requests
                while (true) {
                    final Socket clientSocket = graphWriter.serverSocket.accept();
                    if (graphWriter.isQuit.get()) {
                        return;
                    }
                    final RequestHandler requestHandler = new RequestHandler(graphWriter, clientSocket);
                    graphWriter.graphingRequestExecutor.execute(requestHandler);
                }

            } catch (SocketException ex) {
                if (!graphWriter.isQuit.get()) {
                    throw new RuntimeException(ex);
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * Provides a runnable to handle a graph request on the server socket.
     */
    private static class RequestHandler implements Runnable {

        // the graph writer
        private final GraphWriter graphWriter;

        // the disruptor event translator (slot populator)
        private final EventTranslatorOneArg graphRequestEventTranslatorOneArg = new GraphRequestEventTranslatorOneArg();

        // the connection to the client
        final Socket clientSocket;

        /**
         * Constructs a new RequestHandler instance.
         *
         * @param graphWriter the graph writer
         * @param clientSocket the client socket
         */
        RequestHandler(
                final GraphWriter graphWriter,
                final Socket clientSocket) {
            //Preconditions
            assert graphWriter != null : "graphWriter must not be null";

            this.graphWriter = graphWriter;
            this.clientSocket = clientSocket;
        }

        /**
         * Gets the graph request and puts into the ring buffer.
         */
        @Override
        public void run() {
            final BufferedReader bufferedReader;
            try {
                bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            final GraphRequest graphRequest = GraphRequest.makeGraphRequest(bufferedReader);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("queuing: " + graphRequest);
            }

            // put the request into the ring buffer
            graphWriter.ringBuffer.publishEvent(
                    graphRequestEventTranslatorOneArg,
                    graphRequest); // arg0, the request to be moved field by field into the next ring buffer slot
        }

    }

    /**
     * Provides a disruptor event translator that populates a graph request slot
     * in the ring buffer with a received graph request.
     */
    private static class GraphRequestEventTranslatorOneArg implements EventTranslatorOneArg<GraphRequest, GraphRequest> {

        /**
         * Translates a data representation into fields set in the given event
         *
         * @param event the ring buffer graph request into which the data should
         * be translated
         * @param sequence the event sequence number
         * @param arg0 the received graph request from the client app
         */
        @Override
        public void translateTo(
                final GraphRequest event,
                final long sequence,
                final GraphRequest arg0) {
            //Preconditions
            assert event != null : "event must not be null";
            assert sequence >= 0 : "sequence must not be negative";
            assert arg0 != null : "arg0 must not be null";

            event.setFileName(arg0.getFileName());
            event.setLabeledTree(arg0.getLabeledTree());
        }
    }

    /**
     * Emits a labeled tree graph for the parsing interpretation tree.
     * Synchronized to issue only one graph at at time.
     *
     * @param filePath the graph file path
     * @param labeledTree the labeled tree
     */
    public void graphPHPSyntaxTree(
            final String filePath,
            final String labeledTree) {
        //Preconditions
        assert filePath != null : "filePath must not be null";
        assert !filePath.isEmpty() : "filePath must not be empty";
        assert labeledTree != null : "labeledTree must not be null";
        assert !labeledTree.isEmpty() : "labeledTree must not be empty for: " + filePath;

        if (System.getProperty("file.separator").equals("\\")) {
            // do not try to create a PHP syntax tree on Windows
            return;
        }
        String[] cmdArray = {
            "sh",
            "-c",
            ""
        };
        final StringBuilder stringBuilder = new StringBuilder(1000);
        stringBuilder.append("cd ");
        stringBuilder.append(PHP_SYNTAX_TREE_PATH);
        stringBuilder.append(" ; php graph.php ");
        stringBuilder.append(filePath);
        stringBuilder.append(".png \"");
        stringBuilder.append(labeledTree);
        stringBuilder.append("\"");
        cmdArray[2] = stringBuilder.toString();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("  shell cmd: " + cmdArray[2]);
        }

        try {
            final Process process = Runtime.getRuntime().exec(cmdArray);
            final StreamConsumer errorConsumer = new StreamConsumer(process.getErrorStream(), LOGGER);
            final StreamConsumer outputConsumer = new StreamConsumer(process.getInputStream(), LOGGER);
            errorConsumer.setName("errorConsumer");
            errorConsumer.start();
            outputConsumer.setName("outputConsumer");
            outputConsumer.start();
            int exitVal = process.waitFor();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("  exitVal: " + exitVal);
            } else if (exitVal != 0) {
                LOGGER.warn("process terminated with a non-zero exit value " + exitVal);
                LOGGER.warn("labeledTree...\n " + labeledTree);
            }

            process.getInputStream().close();
            process.getOutputStream().close();
        } catch (InterruptedException ex) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("interrupted");
            }
        } catch (final IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Emits a GraphViz diagram. Synchronized to issue only one graph at at
     * time.
     *
     * @param filePath the graph file path
     */
    public void graphVizDiagram(final String filePath) {
        //Preconditions
        assert filePath != null : "filePath must not be null";
        assert !filePath.isEmpty() : "filePath must not be empty";

        if (System.getProperty("file.separator").equals("\\")) {
            // do not try to create a GraphViz syntax tree on Windows
            return;
        }
        String[] cmdArray = {
            "sh",
            "-c",
            ""
        };
        final StringBuilder stringBuilder = new StringBuilder(200)
                .append("cd; dot -Tpng ")
                .append(filePath)
                .append(".dot -o ")
                .append(filePath)
                .append(".png");
//        if (!filePath.contains("/temp/")) {
            stringBuilder
                    .append("; rm ")
                    .append(filePath)
                    .append(".dot");
//        }
        cmdArray[2] = stringBuilder.toString();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("  shell cmd: " + cmdArray[2]);
        }

        try {
            final Process process = Runtime.getRuntime().exec(cmdArray);
            final StreamConsumer errorConsumer = new StreamConsumer(process.getErrorStream(), LOGGER);
            final StreamConsumer outputConsumer = new StreamConsumer(process.getInputStream(), LOGGER);
            errorConsumer.setName("errorConsumer");
            errorConsumer.start();
            outputConsumer.setName("outputConsumer");
            outputConsumer.start();
            int exitVal = process.waitFor();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("  exitVal: " + exitVal);
            } else if (exitVal != 0) {
                LOGGER.warn("process terminated with a non-zero exit value " + exitVal);
            }

            process.getInputStream().close();
            process.getOutputStream().close();
        } catch (InterruptedException ex) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("interrupted");
            }
        } catch (final IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static class StreamConsumer extends Thread {

        // the input stream that consumes the launched process standard output or standard error stream
        private final InputStream inputStream;
        // the logger
        private final Logger logger;

        /**
         * Constructs a new StreamConsumer instance.
         *
         * @param inputStream the input stream
         * @param logger the logger
         */
        StreamConsumer(
                final InputStream inputStream,
                final Logger logger) {
            //Preconditions
            assert inputStream != null : "inputStream must not be null";
            assert logger != null : "logger must not be null";

            this.inputStream = inputStream;
            this.logger = logger;
        }

        /**
         * Runs this stream consumer.
         */
        @Override
        @SuppressWarnings("NestedAssignment")
        public void run() {
            try {
                try (final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
                    @SuppressWarnings("UnusedAssignment")
                    String line = null;
                    while ((line = bufferedReader.readLine()) != null) {
                        logger.info("> " + line);
                    }
                }
            } catch (IOException ioe) {
                logger.info(ioe.getMessage());
            }
        }
    }

    /**
     * Executes this application.
     *
     * @param args the command line arguments - not used
     */
    public static void main(final String[] args) {
        final GraphWriter graphWriter = new GraphWriter();
        graphWriter.initialialization();
    }

    /**
     * Conveniently as a static method, called from within client code to
     * determine whether the graph-writing server is running.
     *
     * @return whether the graph-writing server is running
     */
    public static boolean isGraphServerRunning() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("issuing a graph request to see if the graph-writing server is running...");
        }
        return issuePHPGraphRequest("ignore", "ignore");
    }

    /**
     * Conveniently as a static method, called from within client code to ensure
     * that the graph-writing server is running.
     */
    public static void ensureRunningGraphServer() {
        if (isGraphServerRunning()) {
            LOGGER.info("graph-writing server is already running");
        } else {
            LOGGER.info("starting graph-writing server");
            String[] cmdArray = {
                "sh",
                "-c",
                ""
            };
            final StringBuilder stringBuilder = new StringBuilder(100);
            stringBuilder.append("cd ");
            stringBuilder.append(GRAPH_WRITER_PATH);
            stringBuilder.append(" ; ./run-graph-writer.sh");
            cmdArray[2] = stringBuilder.toString();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("shell cmd: " + cmdArray[2]);
            }
            try {
                Runtime.getRuntime().exec(cmdArray);
            } catch (IOException ex1) {
                throw new RuntimeException(ex1);
            }
            LOGGER.info("waiting 5 seconds for the graph server to start...");
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException ex2) {
            }

        }
    }

    /**
     * Conveniently as a static method, called from within client code to issue
     * a PHP syntax tree graph request, or when checking whether the server is
     * running.
     *
     * @param fileName the file name without .dot extension
     * @param labeledTree the graph writer debugging description
     *
     * @return true if no errors occurred, or return false if the server is not
     * running
     */
    public static boolean issuePHPGraphRequest(
            final String fileName,
            final String labeledTree) {
        //Preconditions
        assert fileName != null : "fileName must not be null";
        assert !fileName.isEmpty() : "fileName must not be empty";
        assert labeledTree != null : "labeledTree must not be null";
        assert !labeledTree.isEmpty() : "labeledTree must not be empty";

        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("  connecting to localhost graph server on port: " + GraphWriter.LISTENING_PORT);
                if (labeledTree.length() > 30) {
                    LOGGER.debug("  labeledTree: " + labeledTree.substring(0, 30) + " ...");
                } else {
                    LOGGER.debug("  labeledTree: " + labeledTree);
                }
            }
            final Socket socket = new Socket("127.0.0.1", GraphWriter.LISTENING_PORT);
            try (PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true)) {
                final GraphRequest graphRequest = new GraphRequest(fileName, labeledTree);
                printWriter.print(graphRequest.serialize());
                printWriter.flush();
            }
            return true;
        } catch (IOException ex) {
            if (!fileName.equals("ignore")) {
                LOGGER.error("  received exception: " + ex.getMessage());
            }
            return false;
        }
    }

    /**
     * Conveniently as a static method, called from within client code to issue
     * a GraphViz diagram request, or when checking whether the server is
     * running.
     *
     * @param fileName the file name without .dot extension
     *
     * @return true if no errors occurred
     */
    public static boolean issueGraphVizRequest(final String fileName) {
        //Preconditions
        assert fileName != null : "fileName must not be null";
        assert !fileName.isEmpty() : "fileName must not be empty";

        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("  connecting to localhost graph server on port: " + GraphWriter.LISTENING_PORT);
            }
            final Socket socket = new Socket("127.0.0.1", GraphWriter.LISTENING_PORT);
            try (PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true)) {
                final GraphRequest graphRequest = new GraphRequest(
                        fileName,
                        GRAPHVIZ); // labeledTree
                printWriter.print(graphRequest.serialize());
                printWriter.flush();
            }
            return true;
        } catch (IOException ex) {
            if (!fileName.equals("ignore")) {
                LOGGER.error("  received exception: " + ex.getMessage());
            }
            return false;
        }
    }

    /**
     * Conveniently issues a shutdown request from a client.
     */
    public static void shutDown() {
        LOGGER.warn("quitting - waiting 10 seconds for the graph server to finish...");
        try {
            Thread.sleep(10_000);
        } catch (InterruptedException ex2) {
        }
        issuePHPGraphRequest("quit", "quit");
    }

}
