package Server;

import java.io.IOException;
import java.net.*;
import java.sql.*;

public class Server {
    private final ServerSocket serverSocket;
    private final Connection dbConnection;

    public Server(int port) throws IOException, SQLException {
        serverSocket = new ServerSocket(port);
        dbConnection = DriverManager.getConnection("jdbc:mysql://localhost:3306/mathcompetition", "root", "root");
    }
    

    public void start() {
        while (true) {
            try {
                new ClientHandler(serverSocket.accept(), dbConnection).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public static void main(String[] args) {
        try {
            Server server = new Server(6546);
            System.out.println("Server started on port 5000");
            server.start();
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }
}