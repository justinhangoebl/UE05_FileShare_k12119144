package share.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import static share.Constants.*;

import share.Msg;

import static share.Msg.*;
import static share.Msg.Kind.*;

public class FileShareServer {

    public static void main(String[] args) {
        FileShareServer server = new FileShareServer();
        try {
            server.start();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Thread acceptThread;
    private volatile boolean terminate = false;

    private void start() throws IOException, InterruptedException {
        out.println("Start server");
        out.println("Terminate with 'ENTER'");

        acceptThread = new Thread(new AcceptRunnable());
        acceptThread.start();

        // terminate with 'ENTER'
        in.nextLine();
        terminate();
        out.println("Terminated");
    }

    public void terminate() {
        terminate = true;
    }

    private class AcceptRunnable implements Runnable {

        @Override
        public void run() {

            // TODO: Create server socket and start accepting until terminate
            // TODO: for connected clients start ClientHdlr in new thread

            try (ServerSocket ss = new ServerSocket(PORT);) {
                ss.setSoTimeout(1000);
                while (!terminate) {
                    try {
                        new Thread(new ClientHdlr(ss.accept())).start();
                    } catch (SocketTimeoutException ignored) {
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ClientHdlr implements Runnable {
        private final Socket socket;
        private OutputStream outputStream;
        private InputStream inputStream;
        private PrintWriter writer;
        private BufferedReader reader;

        private String clientName;
        private Path clientDir;


        private ClientHdlr(Socket socket) throws IOException {
            this.socket = socket;
            // create inputStream, outputStream, reader, writer
            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();

            writer = new PrintWriter(outputStream);
            reader = new BufferedReader(new InputStreamReader(inputStream));
        }

        @Override
        public void run() {
            try {

                // Send connect Message
                sendMsg(writer, HELO, socket.toString());

                // handle login with client name
                Msg msg = receiveMsg(reader);
                if (msg.kind().equals(LOGIN)) {
                    clientName = msg.param(0);
                    clientDir = Paths.get("global/" + clientName);

                    //check for directory otherwise create a client directory
                    if (!Files.exists(clientDir)) {
                        Files.createDirectory(clientDir);
                    }

                    // Response Success on LogIn
                    sendMsg(writer, SUCCESS, LOGIN.name(), clientName);

                } else {
                    sendMsg(writer, FAILED, LOGIN.name());
                    socket.close();
                }


                // read and handle commands sent from client until logout
                msg = receiveMsg(reader);
                while (!msg.kind().equals(LOGOUT)) {
                    switch (msg.kind()) {
                        case DIRS -> sendDirs();
                        case FILES -> sendFileList(msg.param(0));
                        case DOWNLOAD -> downloadFile(msg.param(0), msg.param(1));
                        case UPLOAD -> uploadFile(msg.param(0), Integer.parseInt(msg.param(1)));
                        default -> System.out.println("This type of message is not accepted");
                    }
                    msg = receiveMsg(reader);
                }

                sendMsg(writer, SUCCESS, clientName);
                System.out.println("Logged out " + clientName);

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void sendDirs() {

            // get directories from server and send them in SUCCESS message to client

            try (Stream<Path> paths = Files.walk(Path.of("global"), 3)) {
                // Map each Path to a String, filter all items in paths to only contain files and map again to a String Array
                String[] strArr = paths.filter(Files::isDirectory).map(Path::toString).toArray(String[]::new);
                sendMsg(writer, SUCCESS, strArr);
            } catch (IOException e) {
                sendMsg(writer, FAILED, "Couldn't send dirs: ", e.getMessage());
                e.printStackTrace();
            }
        }

        private void sendFileList(String dir) {

            // get files in directory and send them in SUCCESS message to client
            Path dirP = Paths.get(dir);

            if (!Files.exists(dirP)) {
                sendMsg(writer, FAILED, "No such Directory");
                return;
            }

            try (Stream<Path> paths = Files.walk(dirP, 3)) {
                // Map each Path to a String, filter all items in paths to only contain files and map again to a String Array
                String[] strArr = paths.filter(Files::isRegularFile).map(Path::toString).toArray(String[]::new);
                sendMsg(writer, SUCCESS, strArr);
            } catch (IOException e) {
                sendMsg(writer, FAILED, "Couldn't send dirs: ", e.getMessage());
                e.printStackTrace();
            }
        }

        private void downloadFile(String dir, String fileName) throws IOException {

            Path filePath = Paths.get("global", dir, fileName);

            if (!Files.exists(filePath)) {
                sendMsg(writer, FAILED, "Such a file does not exist");
                return;
            }

            long fileSize = Files.size(filePath);

            sendMsg(writer, SUCCESS, String.valueOf(fileSize));

            byte[] buffer = new byte[(int) Math.min(fileSize, 1024)];
            InputStream in = Files.newInputStream(filePath);
            long written = 0;
            while (written < fileSize) {
                int n = in.read(buffer);
                outputStream.write(buffer, 0, n);
                written += n;
            }
            in.close();
            Msg msg = receiveMsg(reader);
            if(msg.kind().equals(SUCCESS)){
                System.out.println("Downloaded: " + msg.param(0));
                sendMsg(writer, SUCCESS, fileName);
            } else {
                sendMsg(writer, FAILED, fileName, String.valueOf(fileSize));
            }
        }

        private void uploadFile(String fileName, int fileSize) throws IOException {

            sendMsg(writer, SUCCESS);

            Path filePath = Paths.get(clientDir.toString() + File.pathSeparator + fileName);

            // Create File if it doesn't exist
            if (!Files.exists(filePath)) {
                new File(String.valueOf(filePath)).createNewFile();
            }


            try (FileWriter file = new FileWriter(String.valueOf(filePath))) {
                byte[] buffer = new byte[(int) Math.min(fileSize, 1024)];
                long read = 0;
                while (read < fileSize) {
                    int n = inputStream.read(buffer);
                    read += n;
                    for (byte e : buffer) {
                        file.write(e);
                    }
                }
                sendMsg(writer, SUCCESS, "File scucessfully uploaded!");
            } catch (Exception e) {
                sendMsg(writer, FAILED, "There was a mistake on the upload!");
            }
        }

    }

}
