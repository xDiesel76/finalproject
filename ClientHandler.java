// NEEDS LAUNCH TO WORK
// START SERVER.JAVA 
// THEN CLIENT.JAVA AS MANY TIMES AS WANTED

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ClientHandler implements Runnable {

    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    public static int fileNum = 0; // num of msglogs
    public static int numFileTxT; // num of msglogs
    public static boolean execute1 = true; // execute only once for all instances
    public static boolean execute2 = true; // execute only once for all instances
    public static ArrayList<String> totalLog = new ArrayList<String>(); // gets all messages into arraylist
    public static String messageFromClientFiltered; // messageFromClient is sent through bad word detector and is turned
                                                    // in filtered
    public String messageFromClient; // message recieved from client

    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientUsername;

    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.clientUsername = bufferedReader.readLine();
            clientHandlers.add(this); // when client joins
            broadcastMessage("SERVER: " + clientUsername + " has joined"); // all users see this except the user who
                                                                           // joined
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    public static String getTime() { // gets hour:minutes:seconds
        LocalDateTime myDateObj = LocalDateTime.now();
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("HH:mm:ss");
        String formattedDate = myDateObj.format(myFormatObj);
        return formattedDate;
    }

    public static String getYear() { // gets year:month:day
        LocalDateTime myDateObj = LocalDateTime.now();
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedDate = myDateObj.format(myFormatObj);
        return formattedDate;
    }

    public static int getfileNum() {
        try {
            FileInputStream fis = new FileInputStream("filenums.txt");

            Scanner sc = new Scanner(fis);
            fileNum = Integer.parseInt(sc.nextLine()); // fileNum = first line of filenums.txt (starts at 0)
            FileWriter msg = new FileWriter("filenums.txt"); // make new text file called filenums.txt (replaces)
            fileNum++; // adds 1 to the first line/number
            msg.write(Integer.toString(fileNum)); // writes to the text file
            sc.close();
            msg.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileNum;
    }

    @Override
    public void run() {
        File logs = new File("C:" + File.separator + "Users" + File.separator + "Liamz" + File.separator + "Downloads"
                + File.separator + "finalproject" + File.separator + "finalproject-main" + File.separator
                + "logs"); // This depends on user but I have to use absolute directory (this makes logs
                           // folder)
        if (!logs.exists()) { // Only makes directory if it doesn't exist
            logs.mkdirs();
        }

        if (execute1) { // I have variable of execute1 so it doesn't run for all instances
            numFileTxT = getfileNum();
            totalLog.add("Message Log - [" + getYear() + "]"); // calls the date only once when client joins
            execute1 = false; // Make sure it doesn't run again
        }

        while (socket.isConnected()) {

            try {
                boolean execute3 = true;
                messageFromClient = bufferedReader.readLine();
                FileInputStream fis = new FileInputStream("badwords.txt"); // reads through a txt of badwords I found on
                                                                           // google
                Scanner sc = new Scanner(fis);

                while (sc.hasNextLine()) {
                    String naughtyWords = sc.nextLine();
                    if (messageFromClient.toLowerCase().contains(naughtyWords.toLowerCase())) { // lower case to get all
                                                                                                // variations of swear
                                                                                                // words
                        messageFromClientFiltered = messageFromClient.replace(naughtyWords,
                                "*".repeat(naughtyWords.length())); // Repeats stars to cover entire bad word
                        ;
                        execute3 = false;
                        break;
                    }
                }
                if (execute3) { // makes sure runs once when there are multiple instances
                    messageFromClientFiltered = messageFromClient; // only runs if no bad word is detected
                }

                totalLog.add("[" + getTime() + "] " + messageFromClientFiltered);
                broadcastMessage(messageFromClientFiltered); // Brodcasts messages to everyone else
                sc.close();
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            }

        }
        if (execute2) {
            try {
                FileWriter logger = new FileWriter("C:" + File.separator + "Users" + File.separator + "Liamz"
                        + File.separator + "Downloads"
                        + File.separator + "finalproject" + File.separator + "finalproject-main" + File.separator
                        + "logs" + File.separator + "msglogs"
                        + numFileTxT + ".txt", true); // path depends on user, this makes the msglogs.txt
                for (int i = 0; i < totalLog.size(); i++) {
                    logger.write(totalLog.get(i) + "\n"); // writes all messages stored in totalLog arraylist
                }
                logger.close();
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
            execute2 = false; // Makes sure only 1 messagelog is written and not 1 for each instance
        }

    }

    public void broadcastMessage(String messageToSend) {

        for (ClientHandler clientHandler : clientHandlers) {
            try {
                if (!clientHandler.clientUsername.equals(clientUsername)) {
                    clientHandler.bufferedWriter.write("[" + getTime() + "] " + messageToSend); // calls time everytime
                                                                                                // to make sure time is
                                                                                                // updated
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    public void removeClientHandler() { // runs when client leaves
        clientHandlers.remove(this);
        broadcastMessage("SERVER: " + clientUsername + " has left the chat");
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) { // closes
                                                                                                               // all

        removeClientHandler();
        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
