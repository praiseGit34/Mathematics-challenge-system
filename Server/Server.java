package Server;

import java.io.*;
import java.net.*;
import java.sql.*;

public class Server {
    private final ServerSocket serverSocket;
    private Connection con;

    public Server(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        initializeDatabaseConnection();
    }

    private void initializeDatabaseConnection() {
        try {
            // Load the MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Establish the database connection
            String url = "jdbc:mysql://localhost:3306/challenge";
            String user = "root";
            String password = "";
            con = DriverManager.getConnection(url, user, password);
            
            System.out.println("Connected to the database successfully.");
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Error connecting to the database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void start() {
        System.out.println("Mathematics Server started on port " + serverSocket.getLocalPort());
        System.out.println("Waiting for client requests...");
        
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());
                
                // Create a new thread to handle the client
                ClientHandler clientHandler = new ClientHandler(clientSocket, con);
                new Thread(clientHandler).start();
            } catch (IOException e) {
                System.err.println("Error accepting client connection: " + e.getMessage());
            }
        }
    }

    public void stop() {
        try {
            if (con != null && !con.isClosed()) {
                con.close();
            }
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            System.out.println("Server stopped.");
        } catch (SQLException | IOException e) {
            System.err.println("Error stopping the server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            Server server = new Server(6546);
            server.start();
        } catch (IOException e) {
            System.err.println("Error starting the server: " + e.getMessage());
        }
    }
}