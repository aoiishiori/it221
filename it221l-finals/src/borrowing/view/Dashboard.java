package borrowing.view;
import java.sql.*;

import borrowing.DAO.*;
import borrowing.Interface.*;
import borrowing.model.DataClass;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;

public class Dashboard extends Application {

    private final EquipmentInter equipmentDAO = new Equipment();
    private final BorrowInter borrowDAO = new Borrow();
    private final PersonInter personDAO = new Person();
    private final LabClassInter labClassDAO = new LabClass();

    private String authenticatedRole;
    private String authenticatedUsername;
    private String authenticatedPersonName;
    private String loggedInUserId;
    private BorderPane mainLayout;

    public void setAuthenticatedRole(String role) {
        this.authenticatedRole = role;
    }

    public void setAuthenticatedUsername(String username) {
        this.authenticatedUsername = username;
        // Get person name from username
        String sql = "SELECT p.person_id, p.last_name, p.first_name FROM person p " +
                "LEFT JOIN user_account ua ON p.person_id = ua.person_id " +
                "WHERE ua.username = ?";
        try (Connection conn = borrowing.util.Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.authenticatedPersonName = rs.getString("last_name") + ", " + rs.getString("first_name");
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] " + e.getMessage());
            this.authenticatedPersonName = "Unknown";
        }
    }

    public void setLoggedInUserId(String userId) {
        this.loggedInUserId = userId;
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("CIS Facility & Equipment Borrowing System");
        mainLayout = new BorderPane();

        if (authenticatedRole != null) {
            buildDashboard(authenticatedRole);
        } else {
            javafx.application.Platform.runLater(() -> {
                new LoginView().start(new Stage());
                primaryStage.close();
            });
            return;
        }

        Scene scene = new Scene(mainLayout, 1300, 850);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void buildDashboard(String role) {
        VBox sidebar = new VBox(12);
        sidebar.setPadding(new Insets(25));
        sidebar.setStyle("-fx-background-color: #2c3e50;");
        sidebar.setPrefWidth(280);

        // SHOW LOGGED IN USER
        String displayText = authenticatedPersonName != null ?
                authenticatedPersonName + " (" + authenticatedUsername + ")" :
                "Logged in as: " + role;
        Label lblRole = new Label("✓ " + displayText);
        lblRole.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold; -fx-font-size: 13px;");

        Label lblRole2 = new Label("Role: " + role.toUpperCase());
        lblRole2.setStyle("-fx-text-fill: #ecf0f1; -fx-font-size: 11px;");

        sidebar.getChildren().addAll(lblRole, lblRole2, new Separator());

        // Role-based navigation with improved labels
        if ("CUSTODIAN".equalsIgnoreCase(role)) {
            addNav(sidebar, "Equipment Inventory", this::viewEquipmentLogic);
            addNav(sidebar, "Transaction Records", this::viewBorrowRecords);
            addNav(sidebar, "Class Schedules", this::viewLabClasses);
            addNav(sidebar, "Student Directory", this::viewStudents);
        } else if ("ADMIN".equalsIgnoreCase(role)) {
            addNav(sidebar, "Master Equipment List", this::viewEquipmentLogic);
            addNav(sidebar, "All Borrow Records", this::viewBorrowRecords);
            addNav(sidebar, "Activity Approvals",  this::viewActivityRequests);
        } else if ("BORROWER".equalsIgnoreCase(role)) {
            addNav(sidebar, "My Borrow History", this::viewBorrowerHistoryLogic);
            addNav(sidebar, "Request New Activity", this::viewRequestForm);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button btnLogout = new Button("LOGOUT");
        btnLogout.setMaxWidth(Double.MAX_VALUE);
        btnLogout.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
        btnLogout.setOnAction(e -> {
            new LoginView().start(new Stage());
            ((Stage) mainLayout.getScene().getWindow()).close();
        });

        sidebar.getChildren().addAll(spacer, btnLogout);
        mainLayout.setLeft(sidebar);

        if ("BORROWER".equalsIgnoreCase(role)) viewBorrowerHistoryLogic();
        else viewEquipmentLogic();
    }

    private void addNav(VBox sidebar, String text, Runnable action) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPadding(new Insets(8, 10, 8, 10));
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ecf0f1; -fx-alignment: center-left; -fx-font-size: 13px;");

        // Hover effects to make it more pleasing
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-alignment: center-left;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ecf0f1; -fx-alignment: center-left;"));

        btn.setOnAction(e -> action.run());
        sidebar.getChildren().add(btn);
    }


    private void viewEquipmentLogic() {
        VBox root = createContentRoot("Equipment Inventory Status");

        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(0, 0, 15, 0));
        Button btnRefresh = new Button("🔄 REFRESH");
        btnRefresh.setStyle("-fx-padding: 8px 16px; -fx-font-size: 12px;");
        buttonBox.getChildren().add(btnRefresh);

        TableView<DataClass.Equipment> table = new TableView<>();

        // Headers will be: "Barcode", "Item Name", "Category", "Status"
        setupColumns(table, "barcode", "itemName", "category", "status");
        table.setItems(FXCollections.observableArrayList(equipmentDAO.getAllEquipment()));

        btnRefresh.setOnAction(e -> {
            table.setItems(FXCollections.observableArrayList(equipmentDAO.getAllEquipment()));
        });

        root.getChildren().addAll(buttonBox, table);
        mainLayout.setCenter(root);
    }

    private void viewBorrowRecords() {
        VBox root = createContentRoot("Comprehensive Borrow Records");

        // ADD REFRESH BUTTON
        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(0, 0, 15, 0));
        Button btnRefresh = new Button("🔄 REFRESH");
        btnRefresh.setStyle("-fx-padding: 8px 16px; -fx-font-size: 12px;");
        buttonBox.getChildren().add(btnRefresh);

        TableView<DataClass.BorrowRecord> table = new TableView<>();

        // Headers will be: "Transaction Id", "Borrower Name", "Borrow Date", "Status"
        setupColumns(table, "transactionId", "borrowerName", "borrowDate", "status");
        table.setItems(FXCollections.observableArrayList(borrowDAO.getAllRecords()));

        // REFRESH BUTTON ACTION
        btnRefresh.setOnAction(e -> {
            table.setItems(FXCollections.observableArrayList(borrowDAO.getAllRecords()));
        });

        root.getChildren().addAll(buttonBox, table);
        mainLayout.setCenter(root);
    }

    private void viewBorrowerHistoryLogic() {
        VBox root = createContentRoot("My Personal Borrowing History");
        TableView<DataClass.BorrowRecord> table = new TableView<>();

        // Headers will be: "Transaction Id", "Borrow Date", "Status"
        setupColumns(table, "transactionId", "borrowDate", "status");
        table.setItems(FXCollections.observableArrayList(borrowDAO.getAllRecords()));

        root.getChildren().add(table);
        mainLayout.setCenter(root);
    }

    private void viewStudents() {
        VBox root = createContentRoot("Student Roster & Contact Information");
        TableView<DataClass.StudentRecord> table = new TableView<>();

        // Headers will be: "Person Id", "Last Name", "First Name", "Email"
        setupColumns(table, "personId", "lastName", "firstName", "email");
        table.setItems(FXCollections.observableArrayList(personDAO.getAllStudents()));

        root.getChildren().add(table);
        mainLayout.setCenter(root);
    }

    private void viewLabClasses() {
        VBox root = createContentRoot("Laboratory Class Schedules");
        TableView<DataClass.LabClassRecord> table = new TableView<>();

        // Headers will be: "Class Id", "Course Code", "Section", "Faculty Name"
        setupColumns(table, "classId", "courseCode", "section", "facultyName");
        table.setItems(FXCollections.observableArrayList(labClassDAO.getAllClasses()));

        root.getChildren().add(table);
        mainLayout.setCenter(root);
    }

    private void viewActivityRequests() {
        VBox root = createContentRoot("Activity Approval & Requests");

        // FIX: Move the DAO declaration to the top so all buttons can see it
        ActivityRequestInter activityDAO = new ActivityRequest();

        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(0, 0, 15, 0));
        Button btnRefresh = new Button("🔄 REFRESH");
        btnRefresh.setStyle("-fx-padding: 8px 16px; -fx-font-size: 12px;");
        buttonBox.getChildren().add(btnRefresh);

        TableView<DataClass.ActivityRecord> table = new TableView<>();
        setupColumns(table, "requestId", "activityName", "activityType", "activityDate", "status");

        // Define the Actions Column (Approve/Reject)
        TableColumn<DataClass.ActivityRecord, Void> actionCol = new TableColumn<>("ACTIONS");
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button btnApprove = new Button("APPROVE");
            private final Button btnReject = new Button("REJECT");
            private final HBox container = new HBox(10, btnApprove, btnReject);

            {
                btnApprove.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold;");
                btnReject.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold;");
                container.setAlignment(Pos.CENTER);

                btnApprove.setOnAction(e -> {
                    DataClass.ActivityRecord record = getTableView().getItems().get(getIndex());
                    handleStatusUpdate(record.getRequestId(), "APPROVED", table);
                });

                btnReject.setOnAction(e -> {
                    DataClass.ActivityRecord record = getTableView().getItems().get(getIndex());
                    handleStatusUpdate(record.getRequestId(), "REJECTED", table);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    DataClass.ActivityRecord record = getTableView().getItems().get(getIndex());
                    if ("PENDING".equalsIgnoreCase(record.getStatus())) {
                        setGraphic(container);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });

        table.getColumns().add(actionCol);

        // Set initial items
        table.setItems(FXCollections.observableArrayList(activityDAO.getAllActivities()));

        // Refresh button logic
        btnRefresh.setOnAction(e -> {
            table.setItems(FXCollections.observableArrayList(activityDAO.getAllActivities()));
        });

        // Add everything to the layout
        root.getChildren().addAll(buttonBox, table);
        mainLayout.setCenter(root);
    }

    // approval logic for activity requests
    private void handleStatusUpdate(int requestId, String status, TableView<DataClass.ActivityRecord> table) {
        ActivityRequestInter dao = new ActivityRequest();
        // Assuming the current logged in user is the "approvedBy"
        if (dao.updateActivityStatus(requestId, status, "STAFF-001")) { // was ADMIN-USER but it does not exist lol
            // Refresh table data
            table.setItems(FXCollections.observableArrayList(dao.getAllActivities()));
            new Alert(Alert.AlertType.INFORMATION, "Request " + status).show();
        } else {
            new Alert(Alert.AlertType.ERROR, "Failed to update status.").show();
        }
    }

    private void viewRequestForm() {
        VBox root = createContentRoot("Submit New Activity Request");
        GridPane form = new GridPane();
        form.setHgap(20);
        form.setVgap(15);

        // Form Inputs
        TextField txtActivityName = new TextField();
        txtActivityName.setPromptText("Enter Activity Name");

        ComboBox<String> cbType = new ComboBox<>(FXCollections.observableArrayList(
                "RECRUITMENT", "CERTIFICATION", "MEETING", "TRAINING", "OTHER"
        ));
        cbType.setValue("TRAINING");
        cbType.setPrefWidth(200);

        // --- DATE PICKER WITH RESTRICTION ---
        DatePicker datePicker = new DatePicker();
        datePicker.setPromptText("Select a future date");

        // Disable past dates
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(java.time.LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                // Disable dates before today
                setDisable(empty || date.isBefore(java.time.LocalDate.now()));
                if (date.isBefore(java.time.LocalDate.now())) {
                    setStyle("-fx-background-color: #f2f2f2; -fx-text-fill: #b3b3b3;");
                }
            }
        });

        TextField txtLocation = new TextField();
        txtLocation.setPromptText("Room/Facility Name");

        TextArea txtNotes = new TextArea();
        txtNotes.setPromptText("Optional notes...");
        txtNotes.setPrefHeight(80);

        // Layout
        form.add(new Label("ACTIVITY NAME:"), 0, 0);
        form.add(txtActivityName, 1, 0);
        form.add(new Label("ACTIVITY TYPE:"), 0, 1);
        form.add(cbType, 1, 1);
        form.add(new Label("ACTIVITY DATE:"), 0, 2);
        form.add(datePicker, 1, 2);
        form.add(new Label("LOCATION:"), 0, 3);
        form.add(txtLocation, 1, 3);
        form.add(new Label("REMARKS/NOTES:"), 0, 4);
        form.add(txtNotes, 1, 4);

        Button btnSubmit = new Button("SUBMIT REQUEST");
        btnSubmit.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");

        btnSubmit.setOnAction(e -> {
            if (txtActivityName.getText().isEmpty() || datePicker.getValue() == null) {
                new Alert(Alert.AlertType.WARNING, "Activity name and a valid future date are required.").show();
                return;
            }

            ActivityRequestInter activityDAO = new ActivityRequest();
            String date = datePicker.getValue().toString();

            // Pass the request to the DAO [cite: 172]
            int result = activityDAO.addActivity(
                    txtActivityName.getText(),
                    cbType.getValue(),
                    date,
                    txtLocation.getText(),
                    loggedInUserId != null ? loggedInUserId : "2021-00001",  // USE ACTUAL USER
                    txtNotes.getText()
            );

            if (result != -1) {
                new Alert(Alert.AlertType.INFORMATION, "Request Successfully Submitted!").show();
                viewBorrowerHistoryLogic();
            }
        });

        root.getChildren().addAll(form, btnSubmit);
        mainLayout.setCenter(root);
    }

    // --- REUSABLE UI HELPERS ---

    private VBox createContentRoot(String title) {
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: white;");

        Label lblTitle = new Label(title);
        lblTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        root.getChildren().add(lblTitle);
        return root;
    }

    /**
     * setupColumns - Automatically adds spaces to PascalCase field names.
     * Example: "transactionId" -> "TRANSACTION ID"
     */
    private <T> void setupColumns(TableView<T> table, String... fields) {
        table.getColumns().clear();
        for (String field : fields) {
            // Regex adds a space before capital letters to separate words
            String spacedHeader = field.replaceAll("([a-z])([A-Z])", "$1 $2").toUpperCase();

            TableColumn<T, String> col = new TableColumn<>(spacedHeader);
            col.setCellValueFactory(new PropertyValueFactory<>(field));
            table.getColumns().add(col);
        }
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void showPlaceholder(String title) {
        mainLayout.setCenter(new StackPane(new Label(title + " Module Under Construction")));
    }

    public static void main(String[] args) { launch(args); }
}