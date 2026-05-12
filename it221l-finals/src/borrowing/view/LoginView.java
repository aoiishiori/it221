package borrowing.view;
import java.sql.*;

import borrowing.DAO.Person;
import borrowing.Interface.PersonInter;
import borrowing.util.SessionContext;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginView extends Application {

    @Override
    public void start(Stage primaryStage) {

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(40));
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #f4f4f4;");

        Label lblHeader = new Label("CIS SYSTEM LOGIN");
        lblHeader.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        TextField txtUser = new TextField();
        txtUser.setPromptText("Username");
        txtUser.setMaxWidth(250);

        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("Password");
        txtPass.setMaxWidth(250);

        Button btnLogin = new Button("Login");
        btnLogin.setPrefWidth(250);
        btnLogin.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-font-weight: bold;");

        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill: #e74c3c;");

        btnLogin.setOnAction(e -> {
            String username = txtUser.getText().trim();
            String password = txtPass.getText();

            // 1. VALIDATION
            if (username.isEmpty()) {
                lblError.setText("Username required.");
                return;
            }
            if (password.isEmpty()) {
                lblError.setText("Password required.");
                return;
            }

            // 2. DATABASE CALL
            PersonInter personDAO = new Person();
            // Assuming getCredentials returns [hash, role, person_id]
            String[] creds = personDAO.getCredentials(username);

            if (creds != null) {
                String storedHash = creds[0];
                String dbRole = creds[1];
                String personId = creds[2]; // Use the ID returned from the DAO

                if (password.equals(storedHash)) {
                    // 3. STORE IN SESSION (Global Access)
                    SessionContext.getInstance().login(personId, username, dbRole);

                    // 4. LAUNCH (Now passing all 3 required arguments)
                    launchDashboard(dbRole, primaryStage, username, personId);
                } else {
                    lblError.setText("Invalid password.");
                }
            } else {
                lblError.setText("Account not found or inactive.");
            }
        });

        layout.getChildren().addAll(lblHeader, txtUser, txtPass, btnLogin, lblError);
        primaryStage.setScene(new Scene(layout, 400, 350));
        primaryStage.setTitle("Login");
        primaryStage.show();
    }

    // FIXED: Method signature now matches the call above
    private void launchDashboard(String role, Stage loginStage, String username, String personId) {
        Dashboard dashboard = new Dashboard();

        // Pass info to dashboard instance
        dashboard.setAuthenticatedRole(role);
        dashboard.setAuthenticatedUsername(username);
        dashboard.setLoggedInUserId(personId);

        try {
            dashboard.start(new Stage());
            loginStage.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void launchDashboard(String role, Stage loginStage, String username) {  // ADD username parameter
        Dashboard dashboard = new Dashboard();
        dashboard.setAuthenticatedRole(role);
        dashboard.setAuthenticatedUsername(username);

        // GET PERSON ID FROM USERNAME
        String personId = getPersonIdFromUsername(username);
        dashboard.setLoggedInUserId(personId);

        try {
            dashboard.start(new Stage());
            loginStage.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    private String getPersonIdFromUsername(String username) {
        String sql = "SELECT person_id FROM user_account WHERE username = ?";
        try (Connection conn = borrowing.util.Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("person_id");
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to get person_id: " + e.getMessage());
        }
        return null;
    }


    public static void main(String[] args) { launch(args); }
}