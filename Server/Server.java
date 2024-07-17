package Server;

import java.io.*;
import java.net.*;
import java.sql.*;

public class Server {
    private final ServerSocket serverSocket;
    private final Connection con;

    public Server(int port) throws IOException, SQLException {
        serverSocket = new ServerSocket(port);
        con = DriverManager.getConnection("jdbc:mysql://localhost:3306/mathematics", "root", "");
    }
    

    public void start() {
        while (true) {
            try {
                new ClientHandler(serverSocket.accept(), con).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public static void main(String[] args) {
        try {
            Server server = new Server(6546);
            System.out.println("Mathematics Server started on port 6546");
            System.out.println("waiting for client requests");
            server.start();
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }
}