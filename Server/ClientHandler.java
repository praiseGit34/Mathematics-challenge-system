package Server;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.sql.Date;
import java.text.*;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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

    private String generateSchoolRepresentativePassword(String email) {
        try {
            String checkEmailSql = "SELECT * FROM School WHERE emailAddress = ?";
            try (PreparedStatement checkEmailStmt = con.prepareStatement(checkEmailSql)) {
                checkEmailStmt.setString(1, email);
                ResultSet emailRs = checkEmailStmt.executeQuery();
                if (!emailRs.next()) {
                    return "Error: Invalid email address.";
                }
            }

            String password = String.format("%05d", new Random().nextInt(100000));
            this.currentSchoolRepEmail = email;
            this.currentSchoolRepPassword = password;

            return "A new password has been generated and sent to your email: " + password + 
                   "\nPlease use this password to log in.";
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error generating password: " + e.getMessage();
        }
    }

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
        
        if (isRejectedApplicant(email, schoolRegNo)) {
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

    private String generateRandomPassword() {
        return String.format("%06d", new Random().nextInt(1000000));
    }

    private boolean isRejectedApplicant(String email, int schoolRegNo) throws SQLException {
        String sql = "SELECT * FROM Rejected WHERE emailAddress = ? AND schoolRegNo = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setInt(2, schoolRegNo);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        }
    }

    private boolean checkUserExists(String username, String email) throws SQLException {
        String query = "SELECT COUNT(*) AS count FROM Applicant WHERE userName = ? OR emailAddress = ?";
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

    private String loginUser(String username, String password) {
        try {
            String query = "SELECT * FROM Participant WHERE userName = ? AND password = ?";
            PreparedStatement stmt = con.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                participantID = rs.getInt("participantID");
                isSchoolRepresentative = false;
                return "Login successful for user: " + username;
            } else {
                // Check if it's a school representative
                query = "SELECT * FROM School WHERE emailAddress = ? AND password = ?";
                stmt = con.prepareStatement(query);
                stmt.setString(1, username);
                stmt.setString(2, password);
                rs = stmt.executeQuery();
                if (rs.next()) {
                    school_registration_number = rs.getInt("schoolRegNo");
                    isSchoolRepresentative = true;
                    return "Login successful for school representative: " + username;
                } else {
                    return "Invalid username or password.";
                }
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

    private String attemptChallenge(String challengeNumber) {
        if (!isAuthenticated() || isSchoolRepresentative) {
            return "You must be logged in as a participant to attempt a challenge.";
        }
        try {
            int challengeNo = Integer.parseInt(challengeNumber);
            
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

    private List<Map<String, Object>> fetchRandomQuestions(int challengeNo) throws SQLException {
        String questionSql = "SELECT q.questionNo, q.question, a.answer, a.marksAwarded FROM Question q JOIN Answer a ON q.questionNo = a.questionNo WHERE q.questionBankID = (SELECT questionBankID FROM Challenge WHERE challengeNo = ?) ORDER BY RAND() LIMIT 10";
        List<Map<String, Object>> questions = new ArrayList<>();
        try (PreparedStatement questionStmt = con.prepareStatement(questionSql)) {
            questionStmt.setInt(1, challengeNo);
            ResultSet questionRs = questionStmt.executeQuery();
            while (questionRs.next()) {
                Map<String, Object> question = new HashMap<>();
                question.put("questionNo", questionRs.getInt("questionNo"));
                question.put("question", questionRs.getString("question"));
                question.put("answer", questionRs.getString("answer"));
                question.put("marks", questionRs.getInt("marksAwarded"));
                questions.add(question);
            }
        }
        return questions;
    }

    private boolean hasExceededAttempts(int challengeNo) throws SQLException {
        String checkAttemptsSql = "SELECT COUNT(*) as attempts FROM Attempt WHERE challengeNo = ? AND participantID = ?";
        try (PreparedStatement attemptStmt = con.prepareStatement(checkAttemptsSql)) {
            attemptStmt.setInt(1, challengeNo);
            attemptStmt.setInt(2, participantID);
            ResultSet attemptRs = attemptStmt.executeQuery();
            return attemptRs.next() && attemptRs.getInt("attempts") >= 3;
        }
    }

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