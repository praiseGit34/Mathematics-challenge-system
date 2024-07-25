package Server;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.sql.Date;
import java.text.*;
import java.text.ParseException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.mail.*;
import javax.mail.PasswordAuthentication;
import javax.mail.internet.*;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final Connection con;
    private PrintWriter out;
    private BufferedReader in;
    private int participantID;
    private boolean isSchoolRepresentative;
    private int school_registration_number;
    private String currentSchoolRepEmail;
    private String currentSchoolRepPassword;

    public ClientHandler(Socket clientSocket, Connection con) {
        this.clientSocket = clientSocket;
        this.con = con;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                String response = processRequest(inputLine);
                out.println(response);
                out.flush();
                clearInputBuffer();
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } finally {
            closeResources();
        }
    }
    private void closeResources() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void clearInputBuffer() throws IOException {
        while (in.ready()) {
            in.readLine();
        }
    }
    //method for handling all the commands from the client
    private String processRequest(String request) throws SQLException, ParseException {
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
            String applicantsList = viewApplicants();
            out.println(applicantsList);
            out.flush();
            break;
            case "CONFIRM_APPLICANT":
                if (partofCommand.length != 3) {
                    return "Invalid command format. Use: CONFIRM_APPLICANT yes/no username";
                }
                return confirmApplicant(partofCommand[1], partofCommand[2]);
            default:
                return "Invalid request";
        }
        return action;
    }
    //method for handling register command
    private String registerUser(String[] parts) throws SQLException, ParseException {
        System.out.println("Received registration request. Parts: " + Arrays.toString(parts));
        if (parts.length != 8) {  // REGISTER + 7 args
            return "Invalid registration format";
        }
        String username = parts[1];
        String firstName = parts[2];
        String lastName = parts[3];
        String email = parts[4];
        String dateOfBirth = parts[5];
        int schoolRegNo;
        try {
            schoolRegNo = Integer.parseInt(parts[6]);
        } catch (NumberFormatException e) {//chaecks the whether the reg no exists in the database and is an integer
           // System.out.println();
            return "Invalid school registration number: " + parts[6] ;
        }
        String imagePath = parts[7];
        System.out.println("Parsed registration data:");
        System.out.println("Username: " + username);
        System.out.println("Name: " + firstName + " " + lastName);
        System.out.println("Email: " + email);
        System.out.println("Date of Birth: " + dateOfBirth);
        System.out.println("School Reg No: " + schoolRegNo);
        System.out.println("Image Path: " + imagePath);
        
        if (isRejectedApplicant(email, schoolRegNo)) {//applicant previously rejected
            return "You have been previously rejected and cannot register under this school.";
        }
        if (checkUserExists(username, email)) {//applicant already exists
            return "User with this username or email already exists.";
        }
        String password = generateRandomPassword();
        String sql = "INSERT INTO Applicants (schoolRegNo, email, userName, imagePath, firstName, lastName, password, dateOfBirth) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setInt(1, schoolRegNo);
            pstmt.setString(2, email);
            pstmt.setString(3, username);
            pstmt.setString(4, imagePath);
            pstmt.setString(5, firstName);
            pstmt.setString(6, lastName);
            pstmt.setString(7, password);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");//sql format
            java.util.Date utilDate = sdf.parse(dateOfBirth);
            java.sql.Date sqlDate = new java.sql.Date(utilDate.getTime());
            pstmt.setDate(8, sqlDate);
            int affectedRows = pstmt.executeUpdate();
    if (affectedRows > 0) {
    out.println("User registered successfully");
    sendParticipantEmail(email, username);
    try (PreparedStatement psSelect = con.prepareStatement("SELECT emailAddress FROM schools WHERE schoolRegNo = ?")) {
        psSelect.setInt(1, schoolRegNo);
        try (ResultSet rs = psSelect.executeQuery()) {
            if (rs.next()) {
                String emailAddress = rs.getString("emailAddress");
                String participantInfo = "Username: " + username + "\n"
                + "Name: " + firstName + " " + lastName + "\n"
                + "Email: " + email + "\n"
                + "Date of Birth: " + dateOfBirth + "\n"
                + "School Reg No: " + schoolRegNo;
                 sendEmail(emailAddress, participantInfo);
            } else {
                out.println("No email address found for the given school registration number.");
            }
        }
    } catch (SQLException e) {
        System.out.println("Error retrieving school email: " + e.getMessage());
    }
    System.out.println("\n");
    System.out.println("User registered successfully. Your password is:"+ password);
    return "User registered successfully. Your password is: " + password;
    } else {
    System.out.println("Failed to register user");
    return "Failed to register user";}}
    }
    
    //method to send a success email for registration
    private void sendParticipantEmail(String email,String username) {
        String host = "smtp.gmail.com";
        String from = "mathchallengesystem@gmail.com";
        String password = "aibj jdgj fvpl cfwb";   // Use the App Password generated earlier
    
        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
    
        Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject("Dear "+username);
            message.setText("Hope this email finds you well\nThis is to congratulate you that you have been registered");
    
            Transport.send(message);
            System.out.println("Email sent successfully to " + email);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
    //generate a six digit password random
    private String generateRandomPassword() {
        return String.format("%06d", new Random().nextInt(1000000));
    }
    //handles participants who have been previously rejected
    private boolean isRejectedApplicant(String email, int schoolRegNo) throws SQLException {
        String sql = "SELECT * FROM Rejected WHERE email = ? AND schoolRegNo = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setInt(2, schoolRegNo);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        }
    }
    //handles the participants who previously registered
    private boolean checkUserExists(String username, String email) throws SQLException {
        String query = "SELECT COUNT(*) AS count FROM Applicants WHERE username = ? OR email = ?";
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
    private String generateSchoolRepresentativePassword(String email) {
        try {
            // Check if the email exists in the School table
            String checkEmailSql = "SELECT * FROM Schools WHERE emailAddress = ?";
            try (PreparedStatement checkEmailStmt = con.prepareStatement(checkEmailSql)) {
                checkEmailStmt.setString(1, email);
                ResultSet emailRs = checkEmailStmt.executeQuery();
                if (!emailRs.next()) {
                    System.out.println("Debug: Email not found in School table: " + email);
                    return "Error: Invalid email address. This email is not registered as a school representative.";
                }
                System.out.println("Debug: Email found in School table: " + email);
            }
            // Generate a new password
            String password = String.format("%05d", new Random().nextInt(100000));
            // Update the password in the School table
            String updatePasswordSql = "UPDATE Schools SET password = ? WHERE emailAddress = ?";
            try (PreparedStatement updateStmt = con.prepareStatement(updatePasswordSql)) {
                updateStmt.setString(1, password);
                updateStmt.setString(2, email);
                int updatedRows = updateStmt.executeUpdate();
                if (updatedRows == 0) {
                    System.out.println("Debug: Failed to update password for email: " + email);
                    return "Error: Failed to update password in the database.";
                }
                System.out.println("Debug: Password updated in database for email: " + email);
            }
            sendRepresentativePassword(email);
            return "A new password has been generated and sent to your email: " + email +"\nPlease use this password to log in.";
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error generating password: " + e.getMessage();
        }
    }
     //method to send an email to the representative with the password for logging in
     private void sendRepresentativePassword(String emailAddress) {
        String host = "smtp.gmail.com";
        String from = "mathchallengesystem@gmail.com";
        String password = "aibj jdgj fvpl cfwb";  // Use the App Password generated earlier
    
        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
    
        Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });
    
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailAddress));

            message.setSubject("Re:password generation");
            message.setText("Hope this email finds you well\nYou are informed that your password "+ password +"has been generated go ahead . ");
    
            Transport.send(message);
            System.out.println("Email sent successfully to " + emailAddress);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
    //login method
    private String loginUser(String username, String password) {
        try {
            System.out.println("Debug: Attempting login for: " + username);
            
            // First, check if it's a participant
            String query = "SELECT * FROM Participants WHERE username = ? AND password = ?";
            PreparedStatement stmt = con.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                participantID = rs.getInt("id");
                isSchoolRepresentative = false;
                //out.println("Thank you for logging in to the mathematics challenge system");
                return "Thank you for logging in to the mathematics challenge system";
            } else {
                // Check if it's a school representative
                query = "SELECT * FROM Schools WHERE emailAddress = ? AND password = ?";
                stmt = con.prepareStatement(query);
                stmt.setString(1, username);
                stmt.setString(2, password);
                rs = stmt.executeQuery();
                if (rs.next()) {
                    school_registration_number = rs.getInt("schoolRegNo");
                    isSchoolRepresentative = true;
                    
                    return "Thank you for logging in to the mathematics challenge system";
                } else {
                    System.out.println("Debug: Invalid login attempt for: " + username);
                    return "Invalid username or password.";
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error during login: " + e.getMessage();
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
    //method for view challenges
    private String viewChallenges() {
        if (!isAuthenticated()) {
            return "You must be logged in to view challenges.";
        }
        String sql = "SELECT * FROM Challenges ORDER BY challengeNo";
        StringBuilder result = new StringBuilder();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        
        result.append(String.format("%-5s | %-20s | %-15s | %-15s | %-12s | %-10s\n",
                "No.", "Challenge Name", "startDate", "endDate", "Duration", "numOfQuestions"));
        result.append("-".repeat(100)).append("\n");
        try (PreparedStatement pstmt = con.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
             boolean hasResults = false;
            while (rs.next()) {
                hasResults = true;
                int challengeNo = rs.getInt("challengeNo");
                String challengeName = rs.getString("challengeName");
                Date startDate = rs.getDate("startDate");
                Date endDate = rs.getDate("endDate");
                
                // Handle Duration as a Long instead of Time
                long durationInSeconds = rs.getLong("Duration");
                String durationFormatted = String.format("%02d:%02d:%02d", 
                    durationInSeconds / 3600, (durationInSeconds % 3600) / 60, durationInSeconds % 60);
                
                int noOfQuestions = rs.getInt("numOfQuestions");
                
                result.append(String.format("%-5d | %-20s | %-15s | %-15s | %-12s | %-10d\n",
                        challengeNo,
                        challengeName,
                        dateFormat.format(startDate),
                        dateFormat.format(endDate),
                        durationFormatted,
                        noOfQuestions
                ));
            }
            return hasResults ? result.toString() : "No challenges found in the database.";
    
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error retrieving challenges: " + e.getMessage();
        }
    }
    //method to handle attempt challenge command
    private String attemptChallenge(String challengeNumber) {
        if (!isAuthenticated() || isSchoolRepresentative) {
            return "You must be logged in as a participant to attempt a challenge.";
        }
        try {
            int challengeNo = Integer.parseInt(challengeNumber);
           
            String checkOpenSql = "SELECT * FROM Challenges WHERE challengeNo = ? AND startDate <= CURDATE() AND endDate >= CURDATE()";
            try (PreparedStatement pstmt = con.prepareStatement(checkOpenSql)) {
                pstmt.setInt(1, challengeNo);
                ResultSet rs = pstmt.executeQuery();
                if (!rs.next()) {
                    // Check if the challenge exists at all
                    String checkExistsSql = "SELECT * FROM Challenges WHERE challengeNo = ?";
                    try (PreparedStatement existsStmt = con.prepareStatement(checkExistsSql)) {
                        existsStmt.setInt(1, challengeNo);
                        ResultSet existsRs = existsStmt.executeQuery();
                        if (!existsRs.next()) {
                            return "Challenge does not exist.";
                        } else {
                            return "Challenge exists but is not currently open.";
                        }
                    }
                }
               
                String challengeName = rs.getString("challengeName");
                int durationMinutes = rs.getInt("Duration");
                long durationInSeconds = durationMinutes * 60L;
                
                if (hasExceededAttempts(challengeNo)) {
                    return "You have already attempted this challenge 3 times.";
                }
                
                List<Map<String, Object>> questions = fetchRandomQuestions(challengeNo);
                if (questions.isEmpty()) {
                    return "No questions available for this challenge.";
                }
                
                out.println("CHALLENGE_READY");
                out.flush();
                
                String startResponse = in.readLine();
                if (!startResponse.equalsIgnoreCase("START")) {
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

    private String conductChallenge(List<Map<String, Object>> questions, long durationInSeconds, int attemptNo) throws IOException, SQLException {
        int totalScore = 0;
        int totalPossibleScore = 0;
        int totalQuestions = questions.size();
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (durationInSeconds * 1000);
        StringBuilder report = new StringBuilder();
    
        for (int i = 0; i < questions.size(); i++) {
            Map<String, Object> question = questions.get(i);
            long currentTime = System.currentTimeMillis();
            
            if (currentTime >= endTime) {
                out.println("Time's up!");
                break;
            }
    
            long remainingTime = (endTime - currentTime) / 1000; // remaining time in seconds
            int remainingQuestions = totalQuestions - i;
    
            out.println("Remaining questions: " + remainingQuestions + ", Remaining time: " + remainingTime + " seconds");
            out.println("Question: " + question.get("question"));
            out.flush();
    
            String userAnswer = in.readLine();
            
            // Safely get the mark, defaulting to 1 if it's missing or null
            int mark = 1;
            if (question.containsKey("mark")) {
                Object markObj = question.get("mark");
                if (markObj instanceof Integer) {
                    mark = (Integer) markObj;
                } else if (markObj instanceof String) {
                    try {
                        mark = Integer.parseInt((String) markObj);
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid mark format for question " + (i+1) + ": " + markObj);
                    }
                }
            }
            
            totalPossibleScore += mark;
            int score = scoreAnswer(userAnswer, (String) question.get("answer"), mark);
            totalScore += score;
    
            long questionTime = (System.currentTimeMillis() - currentTime) / 1000; // time taken for this question in seconds
            report.append("Question ").append(i + 1).append(": Score = ").append(score)
                  .append("/").append(mark).append(", Time taken = ").append(questionTime).append(" seconds\n");
        }
    
        long totalTime = (System.currentTimeMillis() - startTime) / 1000; // total time in seconds
        report.append("Total Score: ").append(totalScore).append("/").append(totalPossibleScore).append("\n");
        report.append("Total Time: ").append(totalTime).append(" seconds");
    
        out.println("END_OF_CHALLENGE");
        out.println(report.toString());
        out.flush();
    
        // Calculate percentage mark
        double percentageMark = totalPossibleScore > 0 ? (double) totalScore / totalPossibleScore * 100 : 0;
    
        // Store the final result in the database
        storeAttemptResult(attemptNo, totalScore, totalTime, percentageMark);
    
        return report.toString();
    }
    private int scoreAnswer(String userAnswer, String correctAnswer, int maxMarks) {
    if (userAnswer.equals("-") || userAnswer.equals("")) {
        return 0;
    } else if (userAnswer.equalsIgnoreCase(correctAnswer)) {
        return maxMarks;
    } else {
        return -3;
    }
}

private void storeAttemptResult(int id, int totalScore, long totalTimeSeconds, double percentageMark) throws SQLException {
    String sql = "UPDATE Attempts SET score = ?, startTime = ?, endTime = CURRENT_TIMESTAMP, percentageMark = ? WHERE id = ?";
    try (PreparedStatement pstmt = con.prepareStatement(sql)) {
        pstmt.setInt(1, totalScore);
        
        // Convert totalTimeSeconds to TIME format (HH:MM:SS)
        int hours = (int) (totalTimeSeconds / 3600);
        int minutes = (int) ((totalTimeSeconds % 3600) / 60);
        int seconds = (int) (totalTimeSeconds % 60);
        String timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        pstmt.setString(2, timeString);
        
        pstmt.setDouble(3, percentageMark);
        pstmt.setInt(4, id);
        pstmt.executeUpdate();
    }
}

private List<Map<String, Object>> fetchRandomQuestions(int challengeNo) throws SQLException {
    List<Map<String, Object>> questions = new ArrayList<>();
    String sql = "SELECT questionid, question FROM Questions WHERE ChallengeNo = ? ORDER BY RAND() LIMIT 10";
    try (PreparedStatement pstmt = con.prepareStatement(sql)) {
        pstmt.setInt(1, challengeNo);
        try (ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> question = new HashMap<>();
                question.put("questionId", rs.getString("questionid"));
                question.put("question", rs.getString("question"));
                // question.put("answer", rs.getString("Answer"));
                // question.put("mark", rs.getInt("Mark"));
                questions.add(question);
            }
        }
    }
    return questions;
}

private int storeAttempt(int challengeNo) throws SQLException {
    String sql = "INSERT INTO Attempts (challengeNo, participantID, startTime, endTime, score, percentageMark) VALUES (?, ?, ?, ?, 0, 0)";
    try (PreparedStatement pstmt = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
        pstmt.setInt(1, challengeNo);
        pstmt.setInt(2, participantID); // Assuming you have a participantID variable
        
        Timestamp startTime = new Timestamp(System.currentTimeMillis());
        pstmt.setTimestamp(3, startTime);
        
        // Set endTime to a future time, we'll update it later when the attempt is finished
        Timestamp endTime = new Timestamp(startTime.getTime() + (30 * 60 * 1000)); // 30 minutes from now
        pstmt.setTimestamp(4, endTime);
        
        // We've added score = 0 and percentageMark = 0 directly in the SQL statement
        
        pstmt.executeUpdate();
        
        try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                return generatedKeys.getInt(1);
            } else {
                throw new SQLException("Creating attempt failed, no ID obtained.");
            }
        }
    }
}

    //checking if the participant has exceeded 3 attempts
    private boolean hasExceededAttempts(int challengeNo) throws SQLException {
        String checkAttemptsSql = "SELECT COUNT(*) as attempts FROM Attempt WHERE challengeNo = ? AND participantId = ?";
        try (PreparedStatement attemptStmt = con.prepareStatement(checkAttemptsSql)) {
            attemptStmt.setInt(1, challengeNo);
            attemptStmt.setInt(2, participantID);
            ResultSet attemptRs = attemptStmt.executeQuery();
            return attemptRs.next() && attemptRs.getInt("attempts") >= 3;
        }
    }
    //method to handle confirm yes/no username
    private String confirmApplicant(String decision, String username) {
        if (!isSchoolRepresentative) {
            return "You don't have permission to confirm applicants.";
        }
        boolean isApproved = decision.equalsIgnoreCase("yes");
        String targetTable = isApproved ? "Participant" : "Rejected";
        try {
            con.setAutoCommit(false);

            // Get applicant details
            String selectSql = "SELECT * FROM Applicants WHERE userName = ? AND schoolRegNo = ?";
            try (PreparedStatement selectStmt = con.prepareStatement(selectSql)) {
                selectStmt.setString(1, username);
                selectStmt.setInt(2, school_registration_number);
                ResultSet rs = selectStmt.executeQuery();
                if (!rs.next()) {
                    con.rollback();
                    return "No applicant found with username: " + username;
                }
                // Insert into target table
                String insertSql;
                //String email ="select email from applicants where username=?";
                if (isApproved) {
                    insertSql = "INSERT INTO Participants ( firstName, lastName, email, dateOfBirth, schoolRegNo, userName, imagePath, password) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?)";  
                } else {
                    insertSql = "INSERT INTO Rejected (schoolRegNo, email,userName, imagePath, firstName, lastName, password, dateOfBirth) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                }
                try (PreparedStatement insertStmt = con.prepareStatement(insertSql)) {
                    if (isApproved) {
                        insertStmt.setString(1, rs.getString("firstName"));
                        insertStmt.setString(2, rs.getString("lastName"));
                        insertStmt.setString(3, rs.getString("email"));
                        insertStmt.setDate(4, rs.getDate("dateOfBirth"));
                        insertStmt.setInt(5, rs.getInt("schoolRegNo"));
                        insertStmt.setString(6, rs.getString("username"));
                        insertStmt.setString(7, rs.getString("imagePath"));
                        insertStmt.setString(8, rs.getString("password"));
                       // String email="select email from participants where username=?";
                       String email = rs.getString("email");
                       String userName = rs.getString("username");
                       sendAcceptanceEmail(email,userName);
                    
                    } else {
                        
                        insertStmt.setInt(1, rs.getInt("schoolRegNo"));
                        insertStmt.setString(2, rs.getString("email"));
                        //insertStmt.setInt(3, applicantID);
                        insertStmt.setString(3, rs.getString("userName"));
                        insertStmt.setString(4, rs.getString("imagePath"));
                        insertStmt.setString(5, rs.getString("firstName"));
                        insertStmt.setString(6, rs.getString("lastName"));
                        insertStmt.setString(7, rs.getString("password"));
                        insertStmt.setDate(8, rs.getDate("dateOfBirth"));
                        //String email ="select email from rejected where username=?";
                        String email = rs.getString("email");
                        String userName = rs.getString("username");
                        sendRejectedEmail(email,username);
                    }
                    insertStmt.executeUpdate();
                }
                // Delete from Applicant table
                String deleteSql = "DELETE FROM Applicants WHERE username = ?";
                try (PreparedStatement deleteStmt = con.prepareStatement(deleteSql)) {
                    deleteStmt.setString(1, username);
                    deleteStmt.executeUpdate();
                }
                con.commit();
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
    //send an email to person who has been rejected
    private void sendRejectedEmail(String email,String username) {
        String host = "smtp.gmail.com";
        String from = "mathchallengesystem@gmail.com";
        String password = "aibj jdgj fvpl cfwb";   // Use the App Password generated earlier
    
        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
    
        Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject("Dear "+username);
            message.setText("OOPS! you have been rejected please try again later");
    
            Transport.send(message);
            System.out.println("Email sent successfully to " + email);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
    //sending an acceptance email to the confirmed yes participant
    private void sendAcceptanceEmail(String email,String username) {
        String host = "smtp.gmail.com";
        String from = "mathchallengesystem@gmail.com";
        String password = "aibj jdgj fvpl cfwb";
        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(from, password);
            }
        });
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject("Dear "+username);
            message.setText("congratulations you have been accepted please go ahead and attempt the challenges");
            Transport.send(message);
            System.out.println("Email sent successfully to " + email);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
    
   
    private void sendEmail(String emailAddress,String participantInfo) {
    String host = "smtp.gmail.com";
    String from = "mathchallengesystem@gmail.com";
    String password = "aibj jdgj fvpl cfwb";   // Use the App Password generated earlier

    // Remove this line:
    // emailAddress="Select emailAddress from schools where schoolRegNo=?";

    Properties properties = new Properties();
    properties.put("mail.smtp.host", host);
    properties.put("mail.smtp.port", "587");
    properties.put("mail.smtp.auth", "true");
    properties.put("mail.smtp.starttls.enable", "true");

    Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(from, password);
        }
    });

    try {
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailAddress));
        message.setSubject("Dear School Representative");
        String emailBody = "Hope this email finds you well\n"
        + "Kindly confirm the participant who registered under your school:\n\n"
        + participantInfo;

message.setText(emailBody);
       
        Transport.send(message);
        out.println("Email sent successfully to " + emailAddress);
    } catch (MessagingException e) {
        throw new RuntimeException(e);
    }
}
    //method for handling the view applicants command
    private String viewApplicants() {
        if (!isSchoolRepresentative) {
            return "You don't have permission to view applicants.\nEND_OF_MESSAGE";
        }
        StringBuilder result = new StringBuilder();
        result.append("List of Pending Applicants:\n");
        result.append(String.format("%-20s | %-20s\n", "Username", "Registration Number"));
        result.append("-".repeat(43)).append("\n");
        
        String sql = "SELECT userName, schoolRegNo FROM Applicants WHERE schoolRegNo = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setInt(1, school_registration_number);
            System.out.println("Executing query: " + pstmt); // Debug print
            ResultSet rs = pstmt.executeQuery();
            boolean hasApplicants = false;
            while (rs.next()) {
                hasApplicants = true;
                String username = rs.getString("userName");
                int regNo = rs.getInt("schoolRegNo");
                System.out.println("Found applicant: " + username); // Debug print
                result.append(String.format("%-20s | %-20d\n", username, regNo));
            }
            System.out.println("Has applicants: " + hasApplicants); // Debug print
            if (!hasApplicants) {
                result.append("No pending applicants found for your school.\n");
            } 
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error retrieving applicants: " + e.getMessage() + "\nEND_OF_MESSAGE";
        }
        result.append("END_OF_MESSAGE");
        return result.toString();
    }}
