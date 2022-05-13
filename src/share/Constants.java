package share;

import java.io.PrintStream;
import java.util.Scanner;

public class Constants {

    /** Flag for turn on/off debugging outputs */
    public static final boolean DEBUG = true;

    /** Scanner for console input */
    public static Scanner in = new Scanner(System.in);

    /** PrintStream for console output */
    public static PrintStream out = System.out;

    /** Address of server */
    public static final String SERVER = "localhost";

    /** Port number for file share service */
    public static final int PORT = 9999;

    /** Separator for words in a message line */
    public static final String SEPARATOR = " ";

    /** End of line characters for terminating a message line */
    public static final String END_OF_LINE = "\r\n";


}
