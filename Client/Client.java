package Client;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;

public class Client {
    private Socket soc;
    private PrintWriter out;
    private BufferedReader in;
    private Scanner scanner;

    public void startConnection(String ip, int port) throws IOException {
        soc = new Socket(ip, port);
        out = new PrintWriter(soc.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(soc.getInputStream()));
        scanner = new Scanner(System.in);
    }

    public String sendMessage(String msg) throws IOException {
        out.println(msg);
        return in.readLine();
    }

    public void stopConnection() throws IOException {
        in.close();
        out.close();
        soc.close();
        scanner.close();
    }

    public void run() {
        try {
            while (true) {
                System.out.println("Welcome to the International Education System Mathematics Competition System");
                System.out.println("\tMenu");
                System.out.println(" Login: login username password");
                System.out.println(" Register: Register username firstname lastname emailAddress date_of_birth school_registration_number image_file.png");
                System.out.println(" Logout: logout");
                System.out.println(" View Challenges: ViewChallenges");
                System.out.println(" Attempt Challenge (for participants only): attemptChallenge challengeNumber");
                System.out.println(" View Applicants (for school representatives only): viewApplicants");
                System.out.println(" Confirm Applicant (for school representatives only): confirm yes/no username");
                System.out.println(" Exit: exit");
                System.out.println("Enter command to continue ");
                System.out.print(">>> ");

                String command = scanner.nextLine().trim();
                if (command.equalsIgnoreCase("exit")) {
                    break;
                }
            executeCommand(command);
            }
        } catch (IOException e) {
            System.out.println("Error in client: " + e.getMessage());
        } finally {
            try {
                stopConnection();
            } catch (IOException e) {
                System.out.println("Error closing connection: " + e.getMessage());
            }
        }
    }
    //method for processing commands
    private void executeCommand(String command) throws IOException {
        String[] partcommand = command.split(" ");
        String action = partcommand[0].toLowerCase();

        switch (action) {
            case "register":
                if (partcommand.length != 8) {
                    System.out.println("Invalid registration format. Use: Register username firstname lastname emailAddress date_of_birth school_registration_number image_file.png");
                } else {
                    String[] registerArgs = Arrays.copyOfRange(partcommand, 1, partcommand.length);
                    register(registerArgs);
                   
                }
                break;
            case "login":
                if (partcommand.length == 3) {
                    login(partcommand[1], partcommand[2]);
                } else if (partcommand.length == 2 && partcommand[1].contains("@")) {
                    loginSchoolRepresentative(partcommand[1]);
                } else {
                    System.out.println("Invalid login format. Use: login username password (for regular users) or login email@school.com (for school representatives)");
                }
                break;
            case "logout":
                logout();
                break;
            case "viewchallenges":
                viewChallenges();
                break;
            case "attemptchallenge":
                if (partcommand.length != 2) {
                    System.out.println("Invalid format. Use: attemptChallenge challengeNumber");
                } else {
                    attemptChallenge(partcommand[1]);
                }
                break;
            case "viewapplicants":
                viewApplicants();
                break;
            case "confirm":
                if (partcommand.length != 3 || (!partcommand[1].equals("yes") && !partcommand[1].equals("no"))) {
                    System.out.println("Invalid format. Use: confirm yes/no username");
                } else {
                    confirmApplicant(partcommand[1],partcommand[2]);
                }
                break;
            default:
                System.out.println("Invalid choice. Enter one of the functions ");
        }
    }

    private void login(String username, String password) throws IOException {
        String response = sendMessage("LOGIN " + username + " " + password);
        System.out.println(response);
    }
    
    private void loginSchoolRepresentative(String email) throws IOException {
        String response = sendMessage("LOGIN " + email);
        System.out.println(response);
        if (response.contains("password has been generated")) {
            System.out.print("Please enter the password sent to your email: ");
            String password = scanner.nextLine();
            response = sendMessage("LOGIN " + email + " " + password);
            System.out.println(response);
        }
    }

    private void logout() throws IOException {
        String response = sendMessage("LOGOUT");
        System.out.println(response);
    }

    private void register(String[] args) throws IOException {
        if (args.length != 7) {
            System.out.println("Invalid number of arguments for registration.");
            return;
        }
    
        String username = args[0];
        String firstname = args[1];
        String lastname = args[2];
        String emailAddress = args[3];
        String dateOfBirth = args[4];
        String schoolRegistrationNumber = args[5];
        String imageFileName = args[6];
    
        // Construct the registration message
        String message = String.join(" ", args);
        System.out.println("Sending registration message: " + message);
        String response = sendMessage("REGISTER " + message);
        System.out.println("Server response: " + response);
    }

    private void viewChallenges() throws IOException {
        String response = sendMessage("VIEW_CHALLENGES");
        if (response.startsWith("No challenges") || response.startsWith("Error")) {
            System.out.println(response);
        } else {
            System.out.println("Available Challenges:");
            System.out.println(response);
        }
    }

    private void attemptChallenge(String challengeNumber) throws IOException {
        try {
            String response = sendMessage("ATTEMPT_CHALLENGE " + challengeNumber);
            System.out.println(response);
            String startPrompt = in.readLine();
            System.out.println(startPrompt);
    
            System.out.println("Press Enter to start the challenge...");
            scanner.nextLine(); // Wait for user to press Enter
            out.println("start");
            out.flush();
    
            while (true) {
                String line = in.readLine();
                if (line == null || line.equals("END_OF_CHALLENGE")) {
                    break;
                }
                System.out.println(line);
    
                if (line.startsWith("Enter your answer")) {
                    System.out.print("Your answer: ");
                    String answer = scanner.nextLine();
                    out.println(answer);
                    out.flush();
                }
    
                if (line.equals("oh sorry! time is done")) {
                    System.out.println("Challenge ended due to time limit.");
                    break;
                }
            }
    
            String finalResult = in.readLine();
            System.out.println(finalResult);
        } catch (IOException e) {
            System.err.println("Error during challenge attempt: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void viewApplicants() throws IOException {
        String response = sendMessage("VIEW_APPLICANTS");
        System.out.println(response);
    }

    private void confirmApplicant(String decision, String username) throws IOException {
        String response = sendMessage("CONFIRM_APPLICANT " + decision + " " + username);
        System.out.println(response);
    }

    public static void main(String[] args) {
        Client client = new Client();
        try {
            client.startConnection("localhost", 6546); 
            client.run();
        } catch (IOException e) {
            System.out.println("Error connecting to server: " + e.getMessage());
        }
    }
}