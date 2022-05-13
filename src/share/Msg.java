package share;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static share.Constants.*;

/**
 * Class for representing and handling messages between client and server.
 */
public class Msg {

    /**
     * Enumeration for different kind of messages.
     */
    public enum Kind  {
        HELO, LOGIN, LOGOUT, DIRS, FILES, UPLOAD, DOWNLOAD, SUCCESS, FAILED
    }

    /**
     * Sends a message with kind and parameters using provided writer.
     * @param writer the writer to send the message line
     * @param kind the message kind
     * @param params the parameters
     * @return the message line sent
     */
    public static String sendMsg(PrintWriter writer, Kind kind, String...params) {
        return sendMsg(writer, line(kind, params));
    }

    /**
     * Sends a message line using provided writer.
     * @param writer the writer to send the message line
     * @param line the message line
     * @return the message line sent
     */
    public static String sendMsg(PrintWriter writer, String line) {
        if (DEBUG) System.out.format("--> sent %s %n", line.trim());
        writer.write(line);
        writer.flush();
        return line;
    }

    /**
     * Reads and decodes a message line.
     * @param reader the reader to read the message line
     * @return the message object representing the read message
     * @throws IOException
     */
    public static Msg receiveMsg(BufferedReader reader) throws IOException {
        String msg = reader.readLine();
        if (DEBUG) System.out.format("<-- received %s %n", msg.trim());
        return Msg.from(msg);
    }

    /**
     * Creates a message object from the message line
     * @param line the message line
     * @return the message object
     */
    public static Msg from(String line) {
        String[] elems = line.trim().split(SEPARATOR);
        Kind kind = Kind.valueOf(elems[0]);
        String[] params = Arrays.stream(elems)
                .skip(1)
                .collect(Collectors.toList())
                .toArray(new String[0]);
        return new Msg(kind, params);
    }

    /**
     * Creates a message line from the given message kind and parameters.
     * The line is terminated with end of line characters.
     * @param kind the message kind
     * @param params the message parameter
     * @return the message line
     */
    public static String line(Kind kind, String...params) {
        String paramStr = String.join(SEPARATOR, params);
        return kind.name() + SEPARATOR + paramStr + END_OF_LINE;
    }

    private final Kind kind;
    private final List<String> params;

    /**
     * Private constructor
     * @param kind the message kind
     * @param params the message parameter
     */
    private Msg(Kind kind, String...params) {
        this.kind = kind;
        this.params = List.of(params);
    }

    /**
     * Returns the message kind
     * @return the message kind
     */
    public Kind kind() {
        return kind;
    }

    /**
     * Returns the list of parameters
     * @return the list of parameters
     */
    public List<String> params() {
        return params;
    }

    /**
     * Returns the number of parameters
     * @return the number of parameters
     */
    public int nParams() {
        return params.size();
    }

    /**
     * Returns the ith parameter
     * @param i the index
     * @return the ith parameter
     */
    public String param(int i) {
        return params.get(i);
    }

    /**
     * Returns a string representation of this message object
     * @return the string representation
     */
    @Override
    public String toString() {
        String paramStr = params.stream().collect(Collectors.joining(SEPARATOR));
        return kind.name() + SEPARATOR + paramStr;
    }

    /**
     * Returns a message line for this message object.
     * The line is terminated with end of line characters.
     * @return the message line
     */
    public String toMsgLine() {
        return toString() + END_OF_LINE;
    }

}
