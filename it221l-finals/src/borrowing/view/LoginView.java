package borrowing.view;

import borrowing.DAO.AuditLog;
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
        layout.setStyle("-fx-background-color:#f4f4f4;");

        Label lblHeader = new Label("CIS SYSTEM LOGIN");
        lblHeader.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:#2c3e50;");

        TextField    txtUser = new TextField();    txtUser.setPromptText("Username"); txtUser.setMaxWidth(250);
        PasswordField txtPass = new PasswordField(); txtPass.setPromptText("Password"); txtPass.setMaxWidth(250);

        Button btnLogin = new Button("Login");
        btnLogin.setPrefWidth(250);
        btnLogin.setStyle("-fx-background-color:#34495e;-fx-text-fill:white;-fx-font-weight:bold;");

        Label lblError = new Label();
        lblError.setStyle("-fx-text-fill:#e74c3c;");

        // Allow Enter key to trigger login
        txtPass.setOnAction(e -> btnLogin.fire());

        btnLogin.setOnAction(e -> {
            String username = txtUser.getText().trim();
            String password = txtPass.getText();

            if (username.isEmpty()) { lblError.setText("Username required."); return; }
            if (password.isEmpty()) { lblError.setText("Password required."); return; }

            PersonInter personDAO = new Person();
            String[] creds = personDAO.getCredentials(username);

            if (creds != null) {
                String storedHash = creds[0];
                String dbRole     = creds[1];
                String personId   = creds[2];

                if (password.equals(storedHash)) {
                    SessionContext.getInstance().login(personId, username, dbRole);

                    // Write audit log entry for successful login
                    AuditLog.log(personId, username, "LOGIN",
                            "Successful login with role: " + dbRole);

                    launchDashboard(dbRole, primaryStage, username, personId);
                } else {
                    lblError.setText("Invalid password.");
                    // Log failed attempt (no personId since we won't expose it)
                    AuditLog.log(null, username, "LOGIN_FAILED",
                            "Failed login attempt for username: " + username);
                }
            } else {
                lblError.setText("Account not found or inactive.");
            }
        });

        layout.getChildren().addAll(lblHeader, txtUser, txtPass, btnLogin, lblError);
        primaryStage.setScene(new Scene(layout, 400, 350));
        primaryStage.setTitle("Login — CIS Borrowing System");
        primaryStage.show();
    }

    private void launchDashboard(String role, Stage loginStage, String username, String personId) {
        Dashboard dashboard = new Dashboard();
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

    public static void main(String[] args) { launch(args); }
}