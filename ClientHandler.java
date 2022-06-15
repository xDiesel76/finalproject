import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class ClientHandler implements Runnable {

    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    public static int fileNum = 0;
    public static int numFileTxT;
    public static boolean execute1 = true; // execute only once for all instances
    public static boolean execute2 = true; // execute only once for all instances
    public static ArrayList<String> totalLog = new ArrayList<String>();
    public static String messageFromClientFiltered;
    public String messageFromClient;

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
            clientHandlers.add(this);
            broadcastMessage("SERVER: " + clientUsername + " has joined");
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    public static int getfileNum() {
        try {
            FileInputStream fis = new FileInputStream("filenums.txt");

            Scanner sc = new Scanner(fis);
            fileNum = Integer.parseInt(sc.nextLine());
            FileWriter msg = new FileWriter("filenums.txt");
            fileNum++;
            msg.write(Integer.toString(fileNum));
            sc.close();
            msg.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileNum;
    }

    @Override
    public void run() {

        if (execute1) {
            numFileTxT = getfileNum();
            execute1 = false;
        }

        while (socket.isConnected()) {
            try {
                boolean execute3 = true; 
                messageFromClient = bufferedReader.readLine();
                FileInputStream fis = new FileInputStream("badwords.txt");
                Scanner sc = new Scanner(fis);
                while (sc.hasNextLine()) {
                    String naughtyWords = sc.nextLine();
                    if (messageFromClient.contains(naughtyWords)) {
                        messageFromClientFiltered = messageFromClient.replace(naughtyWords, "*".repeat(12));;
                        execute3 = false;
                        break;
                    }
                }
                if (execute3) {
                    messageFromClientFiltered = messageFromClient;
                }

                totalLog.add(messageFromClientFiltered);
                broadcastMessage(messageFromClientFiltered);
                sc.close();
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            }

        }
        if (execute2) {
            try {
                FileWriter logs = new FileWriter("msglogs" + numFileTxT + ".txt", true);
                for (int i = 0; i < totalLog.size(); i++) {
                    logs.write(totalLog.get(i) + "\n");
                }
                logs.close();
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
            execute2 = false;
        }

    }

    public void broadcastMessage(String messageToSend) {
        for (ClientHandler clientHandler : clientHandlers) {
            try {
                if (!clientHandler.clientUsername.equals(clientUsername)) {
                    clientHandler.bufferedWriter.write(messageToSend);
                    clientHandler.bufferedWriter.newLine();
                    clientHandler.bufferedWriter.flush();
                }
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
            }
        }
    }

    public void removeClientHandler() {
        clientHandlers.remove(this);
        broadcastMessage("SERVER: " + clientUsername + " has left the chat!");
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {

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
