package Server;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.text.*;

public class ClientHandler extends Thread {
    private final Socket soc;
        private final Connection con;
        private PrintWriter out;
        private BufferedReader in;
        private int participantID;
        private boolean isSchoolRepresentative;
        private int school_registration_number;
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
//closing all the classes
    private void closeResources() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (soc != null) soc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
//clearing the in(soc)
    private void clearInputBuffer() throws IOException {
        while (in.ready()) {
            in.readLine();
        }
    }
//handling all the commands in the process request method
    private String processRequest(String request) {
        String[] partofCommand = request.split(" ");
        String action = partofCommand[0].toUpperCase();
        switch (action) {
            case "REGISTER":
                return registerUser(partofCommand);
            case "LOGIN":
                if (partofCommand.length == 3) {
                    return loginUser(partofCommand[1], partofCommand[2]);
                } else if (partofCommand.length == 2 && partofCommand[1].contains("@")) {
                    return generateSchoolRepresentativePassword(partofCommand[1]);
                } else {
                    return "Invalid login format.";
                }
            case "LOGOUT":
                return logoutUser();
            case "VIEW_CHALLENGES":
                return viewChallenges();
            case "ATTEMPT_CHALLENGE":
                return attemptChallenge(partofCommand[1]);
            case "VIEW_APPLICANTS":
                return viewApplicants();
            case "CONFIRM_APPLICANT":
                if (partofCommand.length != 3) {
                    return "Invalid command format. Use: CONFIRM_APPLICANT yes/no username";
                }
                return confirmApplicant(partofCommand[1], partofCommand[2]);
            default:
                return "Invalid request";
        }
    }

    //method for handling the registration command
    private String registerUser(String[] parts) throws SQLException {
            if (parts.length != 8) {
                return "Invalid registration format";
            }
            String username = parts[2];
            String firstName = parts[3];
            String lastName = parts[4];
            String email = parts[5];
            String dateOfBirth = parts[6];
            int schoolRegNo;
            try {
                schoolRegNo = Integer.parseInt(parts[7]);
            } catch (NumberFormatException e) {
                return "Invalid school registration number";
            }
            String imagePath = parts[8];
            
            if (isRejectedApplicant(email, school_registration_number)) {
                return "You have been previously rejected and cannot register under this school.";
            }
            if (checkUserExists(username, email)) {
                return "User with this username or email already exists.";
            }
            String password = generateRandomPassword();
            String sql = "INSERT INTO Applicant (schoolRegNo, emailAddress, userName, imagePath, firstName, lastName, password, dateOfBirth) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setInt(1, schoolRegNo);
                pstmt.setString(2, email);
                pstmt.setString(3, username);
                pstmt.setString(4, imagePath);
                pstmt.setString(5, firstName);
                pstmt.setString(6, lastName);
                pstmt.setString(7, password);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                java.util.Date utilDate = sdf.parse(dateOfBirth);
                java.sql.Date sqlDate = new java.sql.Date(utilDate.getTime());
                pstmt.setDate(8, sqlDate);
                
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows > 0) {
                    return "User registered successfully. Your password is: " + password;
                } else {
                    return "Failed to register user";
                }
            } catch (SQLException | ParseException e) {
                e.printStackTrace();
                return "Error registering user: " + e.getMessage();
            }
        }
        //checks if the user name already exists
        private boolean checkUserExists(String username, String email) throws SQLException {
            String query = "SELECT COUNT(*) AS count FROM users WHERE username = ? OR emailAddress = ?";
            try (PreparedStatement pstmt = con.prepareStatement(query)) {
                pstmt.setString(1, username);
                pstmt.setString(2, email);
        
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    int count = rs.getInt("count");
                    return count > 0;
                }
            }
            return false;
        }
        
        //log in method
        private String loginUser(String string, String string2) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'loginUser'");
        }
        private String logoutUser() {
            if (isAuthenticated()) {
                participantID = 0;
                isSchoolRepresentative = false;
                school_registration_number = 0;
                currentSchoolRepEmail = null;
                currentSchoolRepPassword = null;
                return "Logged out successfully.";
            } else {
                return "No user is currently logged in.";
            }
        }

        private boolean isAuthenticated() {
            return participantID != 0 || (isSchoolRepresentative && school_registration_number != 0);
        }

    private boolean isRejectedApplicant(String email, int school_registration_number) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isRejectedApplicant'");
    }
    private String generateRandomPassword() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'generateRandomPassword'");
    }
    public void start() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'start'");
    }

}
