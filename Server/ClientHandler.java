package Server;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.sql.Date;
import java.text.*;
import java.text.ParseException;
import java.time.Duration;
import java.time.LocalTime;
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
    private String processRequest(String request) throws SQLException {
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
            // In your server's main loop or command handling method
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
    private String registerUser(String[] parts) throws SQLException {
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
            System.out.println("Invalid school registration number: " + parts[6]);
            return "Invalid school registration number";
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
                System.out.println("User registered successfully");
                sendParticipantEmail(email,username);
                sendEmail(email, password, sql);
                return "User registered successfully. Your password is: " + password;
               } else {
                System.out.println("Failed to register user");
                return "Failed to register user";
            }
        } catch (SQLException | ParseException e) {
            System.out.println("Error registering user: " + e.getMessage());
            e.printStackTrace();
            return "Error registering user: " + e.getMessage();
        }
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
                return "Login successful for user: " + username;
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
                    return "Login successful for school representative: " + username;
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
                return "Challenge is not open or does not exist.";
            }
            
            String challengeName = rs.getString("challengeName");
            String attemptDurationStr = rs.getString("Duration");
            
            LocalTime Duration = LocalTime.parse(attemptDurationStr, DateTimeFormatter.ofPattern("HH:mm:ss"));
            long durationInSeconds = Duration.toSecondOfDay();

            if (hasExceededAttempts(challengeNo)) {
                return "You have already attempted this challenge 3 times.";
            }
            List<Map<String, Object>> questions = fetchRandomQuestions(challengeNo);
            if (questions.isEmpty()) {
                return "No questions available for this challenge.";
            }
            String description = String.format("Challenge: %s\nDuration: %s",
                    challengeName, Duration.toString());
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

private List<Map<String, Object>> fetchRandomQuestions(int challengeNo) throws SQLException {
    List<Map<String, Object>> questions = new ArrayList<>();
    String sql = "SELECT q.questionNo, q.question, q.marks " +
                 "FROM Questions q " +
                 "JOIN Challenges cq ON q.questionNo = cq.numOfQuestions " +
                 "WHERE cq.challengeNo = ? " +
                 "ORDER BY RAND() " +
                 "LIMIT 10";
    try (PreparedStatement pstmt = con.prepareStatement(sql)) {
        pstmt.setInt(1, challengeNo);
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) {
            Map<String, Object> question = new HashMap<>();
            question.put("questionNo", rs.getInt("questionNo"));
            question.put("question", rs.getString("question"));
            question.put("marks", rs.getInt("marks"));
            questions.add(question);
        }
    }
    return questions;
}
    private String conductChallenge(List<Map<String, Object>> questions, long durationInSeconds, int attemptID) throws IOException, SQLException {
        int totalScore = 0;
        int totalMarks = 0;
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (durationInSeconds * 1000);
    
        for (int i = 0; i < questions.size(); i++) {
            Map<String, Object> question = questions.get(i);
            long currentTime = System.currentTimeMillis();
            if (currentTime >= endTime) {
                out.println("Time's up!");
                out.flush();
                break;
            }
    
            long remainingTime = endTime - currentTime;
            out.println(String.format("Question %d/%d", i + 1, questions.size()));
            out.println(question.get("question"));
            out.println(String.format("Remaining time: %s", formatDuration(remainingTime)));
            out.println("Enter your answer or '-' to skip:");
            out.flush();
    
            String userAnswer = readLineWithTimeout(remainingTime);
            if (userAnswer == null) {
                out.println("Time's up for this question!");
                out.flush();
                userAnswer = "-";
            }
    
            int questionNo = (int) question.get("questionNo");
            int score = evaluateAnswer(questionNo, userAnswer);
            storeAttemptQuestion(attemptID, questionNo, score, userAnswer);
            totalScore += score;
            totalMarks += (int) question.get("marks");
    
            out.println("Answer recorded. Moving to next question...");
            out.flush();
        }
    
        out.println("END_OF_CHALLENGE");
        out.flush();
    
        double percentageMark = (double) totalScore / totalMarks * 100;
        saveAttemptResult(attemptID, startTime, totalScore, percentageMark);
    
        return String.format("Challenge completed. Your score: %d (%.2f%%)", totalScore, percentageMark);
    }

    private void saveAttemptResult(int attemptID, long startTime, int totalScore, double percentageMark) throws SQLException {
        String saveAttemptSql = "UPDATE Attempt SET endTime = ?, score = ?, percentageMark = ? WHERE attemptID = ?";
        try (PreparedStatement saveStmt = con.prepareStatement(saveAttemptSql)) {
            saveStmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            saveStmt.setInt(2, totalScore);
            saveStmt.setDouble(3, percentageMark);
            saveStmt.setInt(4, attemptID);
            saveStmt.executeUpdate();
        }
    }

    private void storeAttemptQuestion(int attemptID, int questionNo, int score, String givenAnswer) throws SQLException {
        String insertSql = "INSERT INTO AttemptQuestion (attemptID, questionNo, score, givenAnswer) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = con.prepareStatement(insertSql)) {
            pstmt.setInt(1, attemptID);
            pstmt.setInt(2, questionNo);
            pstmt.setInt(3, score);
            pstmt.setString(4, givenAnswer);
            pstmt.executeUpdate();
        }
    }

    private int evaluateAnswer(int questionNo, String userAnswer) throws SQLException {
        String sql = "SELECT answer, marksAwarded FROM Answer WHERE questionNo = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setInt(1, questionNo);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String correctAnswer = rs.getString("answer");
                int marks = rs.getInt("marksAwarded");
                
                if (userAnswer.equals("-")) {
                    return 0;
                } else if (userAnswer.equalsIgnoreCase(correctAnswer)) {
                    return marks;
                } else {
                    return -3;
                }
            } else {
                throw new SQLException("No answer found for question " + questionNo);
            }
        }
    }

    private String readLineWithTimeout(long timeoutMillis) throws IOException {
        long startTime = System.currentTimeMillis();
        StringBuilder input = new StringBuilder();
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            if (in.ready()) {
                int c = in.read();
                if (c == -1 || c == '\n') {
                    break;
                }
                input.append((char) c);
            }
            try {
                Thread.sleep(100); // Small delay to prevent busy-waiting
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return input.length() > 0 ? input.toString() : null;
    }

    private String formatDuration(long millis) {
        Duration duration = Duration.ofMillis(millis);
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private int storeAttempt(int challengeNo) throws SQLException {
        String insertAttemptSql = "INSERT INTO Attempt (startTime, participantID, challengeNo, endTime, score, percentageMark) VALUES (?, ?, ?, NULL, NULL, NULL)";
        try (PreparedStatement pstmt = con.prepareStatement(insertAttemptSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            pstmt.setInt(2, participantID);
            pstmt.setInt(3, challengeNo);
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

    // private List<Map<String, Object>> fetchRandomQuestions(int challengeNo) throws SQLException {
    //     String questionSql = "SELECT q.questionNo, q.question, a.answer, a.marksAwarded FROM Question q JOIN Answer a ON q.questionNo = a.questionNo WHERE q.questionBankID = (SELECT questionBankID FROM Challenge WHERE challengeNo = ?) ORDER BY RAND() LIMIT 10";
    //     List<Map<String, Object>> questions = new ArrayList<>();
    //     try (PreparedStatement questionStmt = con.prepareStatement(questionSql)) {
    //         questionStmt.setInt(1, challengeNo);
    //         ResultSet questionRs = questionStmt.executeQuery();
    //         while (questionRs.next()) {
    //             Map<String, Object> question = new HashMap<>();
    //             question.put("questionNo", questionRs.getInt("questionNo"));
    //             question.put("question", questionRs.getString("question"));
    //             question.put("answer", questionRs.getString("answer"));
    //             question.put("marks", questionRs.getInt("marksAwarded"));
    //             questions.add(question);
    //         }
    //     }
    //     return questions;
    // }
//checking if the participant has exceeded 3 attempts
    private boolean hasExceededAttempts(int challengeNo) throws SQLException {
        String checkAttemptsSql = "SELECT COUNT(*) as attempts FROM Attempt WHERE challengeNo = ? AND participantID = ?";
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
                String email ="select email from applicants where username=?";
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
                        sendAcceptanceEmail(email,"applicant accepted","your registration has been accepted please you can continue with the challenges");
                    
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
                        sendRejectedEmail(email,"applicant rejected","Your registration has been rejected");
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
    private void sendRejectedEmail(String username, String subject, String body) {
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
            String email = getApplicantEmail(username);
            if (email != null && !email.isEmpty()) {
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(from));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
                message.setSubject(subject);
                message.setText(body);
    
                Transport.send(message);
                System.out.println("Rejection email sent successfully to " + email);
            } else {
                System.out.println("Failed to send rejection email: No email found for username " + username);
            }
        } catch (MessagingException e) {
            System.out.println("Failed to send rejection email: " + e.getMessage());
            e.printStackTrace();
        }
    }
    //fetching applicant email from the database
    private String getApplicantEmail(String username) {
        String sql = "SELECT email FROM Applicants WHERE userName = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("email");
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving applicant email: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    //sending an acceptance email to the confirmed yes participant
    private void sendAcceptanceEmail(String username, String subject, String body) {
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
            // First, get the email of the accepted applicant from the database
            String email = getApplicantEmail(username);
            
            if (email != null && !email.isEmpty()) {
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(from));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
                message.setSubject(subject);
                message.setText(body);
    
                Transport.send(message);
                System.out.println("Acceptance email sent successfully to " + email);
            } else {
                System.out.println("Failed to send acceptance email: No email found for username " + username);
            }
        } catch (MessagingException e) {
            System.out.println("Failed to send acceptance email: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
   
// email to send an email to a representative to inform about people that registered under their school
    private void sendEmail(String emailAddress, String body, String sql) {
        String host = "smtp.gmail.com";
        String from = "mathchallengesystem@gmail.com";
        String password = "aibj jdgj fvpl cfwb";   // Use the App Password generated earlier
        emailAddress="Select emailAddress from schools where schoolRegNo=?";
    
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
            message.setSubject("Dear schoolRepresentative");
            message.setText("hope this email finds you well\n kindly confrim this participant who registered under your school");
    
            Transport.send(message);
            System.out.println("Email sent successfully to " + emailAddress);
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
