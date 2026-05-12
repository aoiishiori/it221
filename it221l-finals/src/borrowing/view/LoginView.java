package borrowing.view;

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

            // 1. Basic validation before hitting the database
            if (username.isEmpty()) {
                lblError.setText("Username required.");
                return;
            }
            if (password.isEmpty()) {
                lblError.setText("Password required.");
                return;
            }

            // 2. Database lookup
            PersonInter personDAO = new Person();

            /**
             * FIX #2: getCredentials now correctly returns [hash, role, person_id].
             * Previously, creds[2] (person_id) did not exist, causing an
             * ArrayIndexOutOfBoundsException the moment any user tried to log in.
             *
             * Also removed the duplicate launchDashboard() overload that took only 3
             * parameters. Having two versions of the same method with different signatures
             * but almost identical bodies is confusing and error-prone.
             */
            String[] creds = personDAO.getCredentials(username);

            if (creds != null) {
                String storedHash = creds[0];  // password_hash from DB
                String dbRole     = creds[1];  // role: CUSTODIAN, ADMIN, or BORROWER
                String personId   = creds[2];  // person_id now safely available

                if (password.equals(storedHash)) {
                    // 3. Persist login info in the global SessionContext singleton
                    SessionContext.getInstance().login(personId, username, dbRole);

                    // 4. Open Dashboard and close the login window
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

    /**
     * FIX #3: Only one launchDashboard() method now — the one that accepts all four
     * needed values. The old 3-param overload that called getPersonIdFromUsername()
     * separately (making a second round-trip to the database) is removed entirely.
     */
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