package Server;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.text.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

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
        private  String loginUser(String username, String password) {
            try {
                String query = "SELECT * FROM users WHERE userName = ? AND password = ?";
                PreparedStatement stmt = con.prepareStatement(query);
                stmt.setString(1, username);
                stmt.setString(2, password);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return "Login successful for user: " + username;
                } else {
                    return "Invalid username or password.";
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return "Error during login.";
            }
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

        //handling the view challenges command
        private String viewChallenges() {
            if (!isAuthenticated()) {
                return "You must be logged in to view challenges.";
            }
            String sql = "SELECT * FROM Challenge ORDER BY challengeNo";
            StringBuilder result = new StringBuilder();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            
            result.append(String.format("%-5s | %-20s | %-15s | %-15s | %-12s | %-10s | %-10s\n",
                    "No.", "Challenge Name", "Duration", "Questions", "Overall Mark", "Open Date", "Close Date"));
            result.append("-".repeat(100)).append("\n");
        
            try (PreparedStatement pstmt = con.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
        
                boolean hasResults = false;
                while (rs.next()) {
                    hasResults = true;
                    int challengeNo = rs.getInt("challengeNo");
                    String challengeName = rs.getString("challengeName");
                    Time attemptDuration = rs.getTime("attemptDuration");
                    int noOfQuestions = rs.getInt("noOfQuestions");
                    int overallMark = rs.getInt("overallMark");
                    Date openDate = rs.getDate("openDate");
                    Date closeDate = rs.getDate("closeDate");
        
                    result.append(String.format("%-5d | %-20s | %-15s | %-15d | %-12d | %-10s | %-10s\n",
                            challengeNo,
                            challengeName,
                            attemptDuration.toString(),
                            noOfQuestions,
                            overallMark,
                            dateFormat.format(openDate),
                            dateFormat.format(closeDate)));
                }
        
                return hasResults ? result.toString() : "No challenges found in the database.";
        
            } catch (SQLException e) {
                e.printStackTrace();
                return "Error retrieving challenges: " + e.getMessage();
            }
        }

        //handling attempt challenge command
        private String attemptChallenge(String challengeNumber) {
            if (!isAuthenticated() || isSchoolRepresentative) {
                return "You must be logged in as a participant to attempt a challenge.";
            }
            try {
                int challengeNo = Integer.parseInt(challengeNumber);
                
                // Fetch challenge details
                String checkOpenSql = "SELECT * FROM Challenge WHERE challengeNo = ? AND openDate <= CURDATE() AND closeDate >= CURDATE()";
                try (PreparedStatement pstmt = con.prepareStatement(checkOpenSql)) {
                    pstmt.setInt(1, challengeNo);
                    ResultSet rs = pstmt.executeQuery();
                    if (!rs.next()) {
                        return "Challenge is not open or does not exist.";
                    }
                    
                    String challengeName = rs.getString("challengeName");
                    String attemptDurationStr = rs.getString("attemptDuration");
                    int totalQuestions = rs.getInt("noOfQuestions");
                    
                    LocalTime attemptDuration = LocalTime.parse(attemptDurationStr, DateTimeFormatter.ofPattern("HH:mm:ss"));
                    long durationInSeconds = attemptDuration.toSecondOfDay();
        
                    // Check number of attempts
                    if (hasExceededAttempts(challengeNo)) {
                        return "You have already attempted this challenge 3 times.";
                    }
                    List<Map<String, Object>> questions = fetchRandomQuestions(challengeNo);
                    String description = String.format("Challenge: %s\nDuration: %s",
                            challengeName, attemptDuration.toString());
                    out.println(description);
                    out.flush();
                    
                    String startResponse = in.readLine();
                    if (!startResponse.equalsIgnoreCase("start")) {
                        return "Challenge cancelled.";
                    }
                    
                    int attemptID = storeAttempt(challengeNo);
                    return conductChallenge(questions, durationInSeconds, attemptID);
                }
            } catch (SQLException | IOException e) {
                System.err.println("Error during challenge attempt: " + e.getMessage());
                e.printStackTrace();
                return "Error during challenge attempt: " + e.getMessage();
            }
        }

        private String conductChallenge(List<Map<String, Object>> questions, long durationInSeconds, int attemptID) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'conductChallenge'");
        }
        private int storeAttempt(int challengeNo) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'storeAttempt'");
        }
        private List<Map<String, Object>> fetchRandomQuestions(int challengeNo) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'fetchRandomQuestions'");
        }
        private boolean hasExceededAttempts(int challengeNo) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'hasExceededAttempts'");
        }
        //handling confirm applicant method
        private String confirmApplicant(String decision, String username) {
            if (!isSchoolRepresentative) {
                return "You don't have permission to confirm applicants.";
            }
        
            boolean isApproved = decision.equalsIgnoreCase("yes");
            String targetTable = isApproved ? "Participant" : "Rejected";
        
            try {
                con.setAutoCommit(false);
        
                // Get applicant details
                String selectSql = "SELECT * FROM Applicant WHERE userName = ? AND schoolRegNo = ?";
                try (PreparedStatement selectStmt = con.prepareStatement(selectSql)) {
                    selectStmt.setString(1, username);
                    selectStmt.setInt(2, school_registration_number);
                    ResultSet rs = selectStmt.executeQuery();
        
                    if (!rs.next()) {
                        con.rollback();
                        return "No applicant found with username: " + username;
                    }
        
                    int applicantID = rs.getInt("applicantID");
        
                    // Insert into target table
                    String insertSql;
                    if (isApproved) {
                        insertSql = "INSERT INTO Participant (applicantID, firstName, lastName, emailAddress, dateOfBirth, schoolRegNo, userName, imagePath, password) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    } else {
                        insertSql = "INSERT INTO Rejected (schoolRegNo, emailAddress, applicantID, userName, imagePath, firstName, lastName, password, dateOfBirth) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    }
        
                    try (PreparedStatement insertStmt = con.prepareStatement(insertSql)) {
                        if (isApproved) {
                            insertStmt.setInt(1, applicantID);
                            insertStmt.setString(2, rs.getString("firstName"));
                            insertStmt.setString(3, rs.getString("lastName"));
                            insertStmt.setString(4, rs.getString("emailAddress"));
                            insertStmt.setDate(5, rs.getDate("dateOfBirth"));
                            insertStmt.setInt(6, rs.getInt("schoolRegNo"));
                            insertStmt.setString(7, rs.getString("userName"));
                            insertStmt.setString(8, rs.getString("imagePath"));
                            insertStmt.setString(9, rs.getString("password"));
                        } else {
                            insertStmt.setInt(1, rs.getInt("schoolRegNo"));
                            insertStmt.setString(2, rs.getString("emailAddress"));
                            insertStmt.setInt(3, applicantID);
                            insertStmt.setString(4, rs.getString("userName"));
                            insertStmt.setString(5, rs.getString("imagePath"));
                            insertStmt.setString(6, rs.getString("firstName"));
                            insertStmt.setString(7, rs.getString("lastName"));
                            insertStmt.setString(8, rs.getString("password"));
                            insertStmt.setDate(9, rs.getDate("dateOfBirth"));
                        }
        
                        insertStmt.executeUpdate();
                    }
        
                    // Delete from Applicant table
                    String deleteSql = "DELETE FROM Applicant WHERE applicantID = ?";
                    try (PreparedStatement deleteStmt = con.prepareStatement(deleteSql)) {
                        deleteStmt.setInt(1, applicantID);
                        deleteStmt.executeUpdate();
                    }
        
                    con.commit();
        
                    // Send email notification (implement this method separately)
                    sendEmailNotification(rs.getString("emailAddress"), isApproved);
        
                    return "Applicant " + username + " has been " + (isApproved ? "accepted" : "rejected") + ".";
                }
            } catch (SQLException e) {
                try {
                    con.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
                e.printStackTrace();
                return "Error confirming applicant: " + e.getMessage();
            } finally {
                try {
                    con.setAutoCommit(true);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        
        //sending an email notification
        private void sendEmailNotification(String email, boolean isApproved) {
            // Implement email sending logic here
            System.out.println("Sending " + (isApproved ? "acceptance" : "rejection") + " email to: " + email);
        }
        private String viewApplicants() {
            if (!isSchoolRepresentative) {
                return "You don't have permission to view applicants.";
            }
        
            String sql = "SELECT applicantID, userName, firstName, lastName, emailAddress, dateOfBirth FROM Applicant WHERE schoolRegNo = ?";
            StringBuilder result = new StringBuilder();
            result.append("List of Pending Applicants:\n");
            result.append(String.format("%-5s | %-20s | %-20s | %-20s | %-30s | %-15s\n",
                    "ID", "Username", "First Name", "Last Name", "Email", "Date of Birth"));
            result.append("-".repeat(120)).append("\n");
        
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setInt(1, school_registration_number);
                ResultSet rs = pstmt.executeQuery();
        
                while (rs.next()) {
                    int applicantID = rs.getInt("applicantID");
                    String username = rs.getString("userName");
                    String firstName = rs.getString("firstName");
                    String lastName = rs.getString("lastName");
                    String email = rs.getString("emailAddress");
                    Date dob = rs.getDate("dateOfBirth");
        
                    result.append(String.format("%-5d | %-20s | %-20s | %-20s | %-30s | %-15s\n",
                            applicantID, username, firstName, lastName, email, dob.toString()));
                }
        
                return result.toString();
            } catch (SQLException e) {
                e.printStackTrace();
                return "Error retrieving applicants: " + e.getMessage();
            }
        }

           
    
   
}
