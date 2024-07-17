package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.sql.*;

public class ClientHandler extends Thread {
    private final Socket soc;
        private final Connection con;
        private PrintWriter out;
        private BufferedReader in;
        private int participantID;
        private boolean isSchoolRepresentative;
        private int schoolRegNo;
        private String currentSchoolRepEmail;
        private String currentSchoolRepPassword;

    public ClientHandler(Socket soc, Connection con) {
        this.con = con;
        this.soc=soc;
    }
//accepting client input through the in (buffered reader)
    @Override
        public void run() {
        try {
        out = new PrintWriter(soc.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(soc.getInputStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            String response = processRequest(inputLine);
            out.println(response);
            out.flush();
            clearInputBuffer();  // Clear the input buffer after each response
        }
    } catch (IOException e) {
        e.printStackTrace();
    } finally {
        closeResources();
    }
}
    private void closeResources() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'closeResources'");
    }

    private void clearInputBuffer() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'clearInputBuffer'");
    }

    private String processRequest(String inputLine) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'processRequest'");
    }

    public void start() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'start'");
    }

}
