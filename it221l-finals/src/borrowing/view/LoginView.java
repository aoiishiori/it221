package borrowing.view;

import borrowing.DAO.Person;
import borrowing.Interface.PersonInter;
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

            PersonInter personDAO = new Person();
            String[] creds = personDAO.getCredentials(username);

            if (creds != null) {
                String storedHash = creds[0];
                String dbRole = creds[1]; // Taken directly from user_account role column

                // For testing: Compare plain text input with DB hash
                // If DB says 'test', type 'test' in the password field
                if (password.equals(storedHash)) {
                    launchDashboard(dbRole, primaryStage);
                } else {
                    lblError.setText("Invalid password.");
                }
            } else {
                lblError.setText("Account not found or inactive.");
            }
        });

        layout.getChildren().addAll(lblHeader, txtUser, txtPass, btnLogin, lblError);

        Scene scene = new Scene(layout, 400, 350);
        primaryStage.setTitle("Login");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void launchDashboard(String role, Stage loginStage) {
        Dashboard dashboard = new Dashboard();
        dashboard.setAuthenticatedRole(role);
        try {
            dashboard.start(new Stage());
            loginStage.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) { launch(args); }
}