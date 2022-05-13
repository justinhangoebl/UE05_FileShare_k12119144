package share.client;

import share.Msg;

import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import static share.Constants.*;
import static share.Msg.*;
import static share.Msg.Kind.*;

public class FileShareClient {

    public static void main(String[] args) throws IOException {
        out.println("------- Welcome to FileShare -------");
        out.print("Please, input your name: ");
        String name = in.nextLine().trim();
        FileShareClient client = new FileShareClient(name);
        client.start();
    }

    private final String name;

    private Socket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private PrintWriter writer;
    private BufferedReader reader;
    private Path localDir;

    private FileShareClient(String name) {
        this.name = name;
    }

    private void start() throws IOException {
        localDir = Paths.get("local", name);
        if (!Files.exists(localDir)) {
            Files.createDirectory(localDir);
        }

        try {
            socket = new Socket(SERVER, PORT);

            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();

            writer = new PrintWriter(outputStream);
            reader = new BufferedReader(new InputStreamReader(inputStream));

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Connection on: " + receiveMsg(reader).param(0));

        // append a carriage return linefeed so the OS knows the writer is finished, and it can be read
        sendMsg(writer, LOGIN, name);
        if(receiveMsg(reader).kind() != SUCCESS){
            return;
        }

        out.println("------------------------------------");
        out.println(String.format("Hello %s!", name));
        out.println("Your commands: ");
        out.println("  x - exit ");
        out.println("  l - list directories ");
        out.println("  f - files in directory ");
        out.println("  u - upload file ");
        out.println("  d - download file ");
        out.println("------------------------------------");
        out.print("\nInput command: ");
        String cmd = in.nextLine().trim();
        while (!cmd.startsWith("x")) {
            switch (cmd) {
                case "l":
                    receiveDirs();
                    break;
                case "f":
                    receiveFileList();
                    break;
                case "u":
                    uploadFile();
                    break;
                case "d":
                    downLoadFile();
                    break;
                default:
                    out.println("Wrong command " + cmd);
                    break;
            }
            out.print("\nInput command: ");
            cmd = in.nextLine().trim();
        }

        logout();
        out.println("\n--- Thanks for using FileShare -----");

    }

    private void receiveDirs() throws IOException {

        // Request
        sendMsg(writer, line(DIRS));

        // Result
        Msg msg = Msg.receiveMsg(reader);
        if(msg.kind().equals(SUCCESS)){
            for(String item: msg.params()){
                System.out.println(item);
            }
        } else {
            System.out.println(msg);
        }

    }

    private void receiveFileList() throws IOException {
        // Send the Directory
        out.print(" Input directory: ");
        String dir = in.nextLine().trim();

        if(dir.isEmpty()){
            System.out.println("Directory cannot be empty");
            return;
        }

        // Request
        sendMsg(writer, FILES, dir);

        // Result
        Msg msg = Msg.receiveMsg(reader);
        if(msg.kind().equals(SUCCESS)){
            for(String item: msg.params()){
                System.out.println(item);
            }
        } else {
            System.out.println(msg);
        }
    }

    private void downLoadFile() throws IOException {
        out.print(" Input directory: ");
        String dir = in.nextLine().trim();
        out.print(" Input file name: ");
        String fileName = in.nextLine().trim();

        Path localFilePath = localDir.resolve(fileName);

        if(dir.isEmpty()){
            System.out.println("Directory cannot be empty");
            return;
        }
        if(fileName.isEmpty()){
            System.out.println("Filename cannot be empty");
            return;
        }

        // Request
        sendMsg(writer, DOWNLOAD, dir, fileName);


        // Result
        Msg msg = receiveMsg(reader);
        if(msg.kind().equals(SUCCESS)){
            try(FileWriter file = new FileWriter(String.valueOf(localFilePath))) {
                long fileSize = Long.parseLong(msg.param(0));
                byte[] buffer = new byte[(int) Math.min(fileSize, 1024)];
                long read = 0;
                while (read < fileSize) {
                    int n = inputStream.read(buffer);
                    read += n;
                    for (byte e : buffer) {
                        file.write(e);
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
                System.out.println("Couldn't download File");
            }
        } else {
            System.out.println(msg);
        }
        sendMsg(writer, SUCCESS, fileName);
        msg = receiveMsg(reader);
        System.out.println(msg);

    }

    private void uploadFile() throws IOException {
        out.print(" Input file name: ");
        String fileName = in.nextLine().trim();
        Path localFilePath = localDir.resolve(fileName);
        long fileSize = Files.size(localFilePath);

        if(!Files.exists(localFilePath)){
            System.out.println("This File does not exist");
            return;
        } else {
            sendMsg(writer, UPLOAD, fileName, String.valueOf(fileSize));
        }

        Msg res = receiveMsg(reader);

        if(res.kind().equals(SUCCESS)){
            byte[] buffer = new byte[(int) Math.min(fileSize, 1024)];
            InputStream in = Files.newInputStream(localFilePath);
            long written = 0;
            while(written < fileSize){
                int n = in.read(buffer);
                outputStream.write(buffer, 0, n);
                written += n;
            }
            in.close();
        } else {
            System.out.println("Couldn't upload File");
            System.out.println(res);
        }

        res = receiveMsg(reader);
        System.out.println(res);
    }

    private void logout() throws IOException {

        sendMsg(writer, LOGOUT, name);

        Msg res = receiveMsg(reader);
        if (res.kind() == SUCCESS) {
            out.println("Logged out: " + res.param(0));
        } else {
            System.out.println("Couldn't log out client");
        }

        socket.close();
    }

}
