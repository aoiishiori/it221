package borrowing.view;

import borrowing.DAO.*;
import borrowing.Interface.*;
import borrowing.model.DataClass;
import borrowing.util.SessionContext;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

// import java.util.List;

public class Dashboard extends Application {

    // DAOs — one instance per DAO class, reused for the lifetime of the dashboard
    private final EquipmentInter  equipmentDAO = new Equipment();
    private final BorrowInter     borrowDAO    = new Borrow();
    private final PersonInter     personDAO    = new Person();
    private final LabClassInter   labClassDAO  = new LabClass();

    // Set during login — describes WHO is using the dashboard
    private String authenticatedRole;
    private String authenticatedUsername;
    private String authenticatedPersonName;
    private String loggedInUserId;           // person_id from the person table

    private BorderPane mainLayout;

    // ----------------------------------------------------------------
    // Setters called by LoginView before start() runs
    // ----------------------------------------------------------------

    public void setAuthenticatedRole(String role)         { this.authenticatedRole = role; }
    public void setLoggedInUserId(String userId)          { this.loggedInUserId = userId; }

    /**
     * FIX #5: Removed the raw SQL query from setAuthenticatedUsername().
     * Business / DB logic does not belong inside a setter in a view class.
     * We now resolve the display name through the DAO layer (personDAO.getPersonName),
     * which is the correct architectural pattern.
     */
    public void setAuthenticatedUsername(String username) {
        this.authenticatedUsername = username;
        // loggedInUserId is set separately by setLoggedInUserId() before start() is called
    }

    // Called after both setAuthenticatedUsername and setLoggedInUserId have been called
    private void resolvePersonName() {
        if (loggedInUserId != null) {
            String name = personDAO.getPersonName(loggedInUserId);
            this.authenticatedPersonName = (name != null) ? name : "Unknown";
        } else {
            this.authenticatedPersonName = authenticatedUsername;
        }
    }

    // ----------------------------------------------------------------
    // JavaFX entry point
    // ----------------------------------------------------------------

    @Override
    public void start(Stage primaryStage) {
        resolvePersonName(); // Safe to call now — loggedInUserId is already set

        primaryStage.setTitle("CIS Facility & Equipment Borrowing System");
        mainLayout = new BorderPane();

        if (authenticatedRole != null) {
            buildDashboard(authenticatedRole);
        } else {
            // No role means we were launched without going through login — redirect
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

    // ----------------------------------------------------------------
    // Sidebar / navigation
    // ----------------------------------------------------------------

    private void buildDashboard(String role) {
        VBox sidebar = new VBox(12);
        sidebar.setPadding(new Insets(25));
        sidebar.setStyle("-fx-background-color: #2c3e50;");
        sidebar.setPrefWidth(280);

        String displayText = (authenticatedPersonName != null)
                ? authenticatedPersonName + " (" + authenticatedUsername + ")"
                : "Logged in as: " + role;

        Label lblName = new Label("✓ " + displayText);
        lblName.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold; -fx-font-size: 13px;");

        Label lblRole = new Label("Role: " + role.toUpperCase());
        lblRole.setStyle("-fx-text-fill: #ecf0f1; -fx-font-size: 11px;");

        sidebar.getChildren().addAll(lblName, lblRole, new Separator());

        // Role-based navigation
        if ("CUSTODIAN".equalsIgnoreCase(role)) {
            addNav(sidebar, "Equipment Inventory",  this::viewEquipmentLogic);
            addNav(sidebar, "Transaction Records",  this::viewBorrowRecords);
            addNav(sidebar, "Class Schedules",      this::viewLabClasses);
            addNav(sidebar, "Student Directory",    this::viewStudents);
        } else if ("ADMIN".equalsIgnoreCase(role)) {
            addNav(sidebar, "Master Equipment List", this::viewEquipmentLogic);
            addNav(sidebar, "All Borrow Records",    this::viewBorrowRecords);
            addNav(sidebar, "Activity Approvals",    this::viewActivityRequests);
        } else if ("BORROWER".equalsIgnoreCase(role)) {
            addNav(sidebar, "My Borrow History",    this::viewBorrowerHistoryLogic);
            addNav(sidebar, "Request New Activity", this::viewRequestForm);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button btnLogout = new Button("LOGOUT");
        btnLogout.setMaxWidth(Double.MAX_VALUE);
        btnLogout.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
        btnLogout.setOnAction(e -> {
            SessionContext.getInstance().logout(); // Clear session data
            new LoginView().start(new Stage());
            ((Stage) mainLayout.getScene().getWindow()).close();
        });

        sidebar.getChildren().addAll(spacer, btnLogout);
        mainLayout.setLeft(sidebar);

        // Default view when the dashboard first opens
        if ("BORROWER".equalsIgnoreCase(role)) viewBorrowerHistoryLogic();
        else viewEquipmentLogic();
    }

    private void addNav(VBox sidebar, String text, Runnable action) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPadding(new Insets(8, 10, 8, 10));
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ecf0f1; " +
                "-fx-alignment: center-left; -fx-font-size: 13px;");
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: #34495e; -fx-text-fill: white; -fx-alignment: center-left;"));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #ecf0f1; -fx-alignment: center-left;"));
        btn.setOnAction(e -> action.run());
        sidebar.getChildren().add(btn);
    }

    // ----------------------------------------------------------------
    // View methods
    // ----------------------------------------------------------------

    private void viewEquipmentLogic() {
        VBox root = createContentRoot("Equipment Inventory Status");

        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(0, 0, 15, 0));
        Button btnRefresh = new Button("🔄 REFRESH");
        btnRefresh.setStyle("-fx-padding: 8px 16px; -fx-font-size: 12px;");
        buttonBox.getChildren().add(btnRefresh);

        TableView<DataClass.Equipment> table = new TableView<>();
        setupColumns(table, "barcode", "itemName", "category", "status");
        table.setItems(FXCollections.observableArrayList(equipmentDAO.getAllEquipment()));

        btnRefresh.setOnAction(e ->
                table.setItems(FXCollections.observableArrayList(equipmentDAO.getAllEquipment())));

        root.getChildren().addAll(buttonBox, table);
        mainLayout.setCenter(root);
    }

    private void viewBorrowRecords() {
        VBox root = createContentRoot("Comprehensive Borrow Records");

        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(0, 0, 15, 0));
        Button btnRefresh = new Button("🔄 REFRESH");
        btnRefresh.setStyle("-fx-padding: 8px 16px; -fx-font-size: 12px;");
        buttonBox.getChildren().add(btnRefresh);

        TableView<DataClass.BorrowRecord> table = new TableView<>();
        setupColumns(table, "transactionId", "borrowerName", "borrowDate", "status");
        table.setItems(FXCollections.observableArrayList(borrowDAO.getAllRecords()));

        btnRefresh.setOnAction(e ->
                table.setItems(FXCollections.observableArrayList(borrowDAO.getAllRecords())));

        root.getChildren().addAll(buttonBox, table);
        mainLayout.setCenter(root);
    }

    /**
     * FIX #6: Previously this called borrowDAO.getAllRecords(), which dumps the
     * ENTIRE transaction table to a borrower — showing other people's records.
     *
     * Now it calls getRecordsByBorrowerId(loggedInUserId), so each borrower sees
     * ONLY their own transactions.
     */
    private void viewBorrowerHistoryLogic() {
        VBox root = createContentRoot("My Personal Borrowing History");
        TableView<DataClass.BorrowRecord> table = new TableView<>();
        setupColumns(table, "transactionId", "borrowDate", "expectedReturn", "status");

        if (loggedInUserId != null) {
            table.setItems(FXCollections.observableArrayList(
                    borrowDAO.getRecordsByBorrowerId(loggedInUserId)));
        } else {
            // Fallback — should not happen if login flow is correct
            table.setPlaceholder(new Label("Session error: user ID not found."));
        }

        root.getChildren().add(table);
        mainLayout.setCenter(root);
    }

    private void viewStudents() {
        VBox root = createContentRoot("Student Roster & Contact Information");
        TableView<DataClass.StudentRecord> table = new TableView<>();
        setupColumns(table, "personId", "lastName", "firstName", "email");
        table.setItems(FXCollections.observableArrayList(personDAO.getAllStudents()));
        root.getChildren().add(table);
        mainLayout.setCenter(root);
    }

    private void viewLabClasses() {
        VBox root = createContentRoot("Laboratory Class Schedules");
        TableView<DataClass.LabClassRecord> table = new TableView<>();
        setupColumns(table, "classId", "courseCode", "section", "facultyName");
        table.setItems(FXCollections.observableArrayList(labClassDAO.getAllClasses()));
        root.getChildren().add(table);
        mainLayout.setCenter(root);
    }

    private void viewActivityRequests() {
        VBox root = createContentRoot("Activity Approval & Requests");
        ActivityRequestInter activityDAO = new ActivityRequest();

        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(0, 0, 15, 0));
        Button btnRefresh = new Button("🔄 REFRESH");
        btnRefresh.setStyle("-fx-padding: 8px 16px; -fx-font-size: 12px;");
        buttonBox.getChildren().add(btnRefresh);

        TableView<DataClass.ActivityRecord> table = new TableView<>();
        setupColumns(table, "requestId", "activityName", "activityType", "activityDate", "status");

        // Action column with Approve / Reject buttons per row
        TableColumn<DataClass.ActivityRecord, Void> actionCol = new TableColumn<>("ACTIONS");
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button btnApprove = new Button("APPROVE");
            private final Button btnReject  = new Button("REJECT");
            private final HBox container    = new HBox(10, btnApprove, btnReject);

            {
                btnApprove.setStyle(
                        "-fx-background-color: #27ae60; -fx-text-fill: white; " +
                                "-fx-font-size: 10px; -fx-font-weight: bold;");
                btnReject.setStyle(
                        "-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                                "-fx-font-size: 10px; -fx-font-weight: bold;");
                container.setAlignment(Pos.CENTER);

                btnApprove.setOnAction(e -> {
                    DataClass.ActivityRecord rec = getTableView().getItems().get(getIndex());
                    handleStatusUpdate(rec.getRequestId(), "APPROVED", table);
                });
                btnReject.setOnAction(e -> {
                    DataClass.ActivityRecord rec = getTableView().getItems().get(getIndex());
                    handleStatusUpdate(rec.getRequestId(), "REJECTED", table);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    DataClass.ActivityRecord rec = getTableView().getItems().get(getIndex());
                    // Only show buttons for requests still awaiting a decision
                    setGraphic("PENDING".equalsIgnoreCase(rec.getStatus()) ? container : null);
                }
            }
        });

        table.getColumns().add(actionCol);
        table.setItems(FXCollections.observableArrayList(activityDAO.getAllActivities()));
        btnRefresh.setOnAction(e ->
                table.setItems(FXCollections.observableArrayList(activityDAO.getAllActivities())));

        root.getChildren().addAll(buttonBox, table);
        mainLayout.setCenter(root);
    }

    /**
     * FIX #7: The approver is now taken from the active session (loggedInUserId)
     * instead of the hardcoded string "STAFF-001".
     * Hardcoding a person_id as the approver means EVERY approval in the system
     * would be attributed to Miguel Hernandez (STAFF-001), regardless of who is
     * actually logged in as ADMIN.
     */
    private void handleStatusUpdate(int requestId, String status,
                                    TableView<DataClass.ActivityRecord> table) {
        ActivityRequestInter dao = new ActivityRequest();
        String approverPersonId = (loggedInUserId != null) ? loggedInUserId : "STAFF-001";

        if (dao.updateActivityStatus(requestId, status, approverPersonId)) {
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

        TextField txtActivityName = new TextField();
        txtActivityName.setPromptText("Enter Activity Name");

        ComboBox<String> cbType = new ComboBox<>(FXCollections.observableArrayList(
                "RECRUITMENT", "CERTIFICATION", "MEETING", "TRAINING", "OTHER"));
        cbType.setValue("TRAINING");
        cbType.setPrefWidth(200);

        DatePicker datePicker = new DatePicker();
        datePicker.setPromptText("Select a future date");
        // Disable past dates — borrowers cannot request past activities
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(java.time.LocalDate date, boolean empty) {
                super.updateItem(date, empty);
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

        form.add(new Label("ACTIVITY NAME:"), 0, 0); form.add(txtActivityName, 1, 0);
        form.add(new Label("ACTIVITY TYPE:"), 0, 1); form.add(cbType,           1, 1);
        form.add(new Label("ACTIVITY DATE:"), 0, 2); form.add(datePicker,       1, 2);
        form.add(new Label("LOCATION:"),      0, 3); form.add(txtLocation,      1, 3);
        form.add(new Label("REMARKS/NOTES:"), 0, 4); form.add(txtNotes,         1, 4);

        Button btnSubmit = new Button("SUBMIT REQUEST");
        btnSubmit.setStyle(
                "-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");

        btnSubmit.setOnAction(e -> {
            if (txtActivityName.getText().isEmpty() || datePicker.getValue() == null) {
                new Alert(Alert.AlertType.WARNING,
                        "Activity name and a valid future date are required.").show();
                return;
            }

            ActivityRequestInter activityDAO = new ActivityRequest();

            int result = activityDAO.addActivity(
                    txtActivityName.getText(),
                    cbType.getValue(),
                    datePicker.getValue().toString(),
                    txtLocation.getText(),
                    loggedInUserId,   // Always use the real session user, never a hardcoded fallback
                    txtNotes.getText()
            );

            if (result != -1) {
                new Alert(Alert.AlertType.INFORMATION, "Request Successfully Submitted!").show();
                viewBorrowerHistoryLogic();  // Navigate back to the borrower's history
            } else {
                new Alert(Alert.AlertType.ERROR, "Failed to submit request.").show();
            }
        });

        root.getChildren().addAll(form, btnSubmit);
        mainLayout.setCenter(root);
    }

    // ----------------------------------------------------------------
    // Reusable UI helpers
    // ----------------------------------------------------------------

    private VBox createContentRoot(String title) {
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: white;");
        Label lblTitle = new Label(title);
        lblTitle.setStyle(
                "-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        root.getChildren().add(lblTitle);
        return root;
    }

    /**
     * setupColumns — converts a camelCase field name into a readable column header.
     * Example: "transactionId" → "TRANSACTION ID"
     * Uses JavaFX PropertyValueFactory which calls the matching getter automatically
     * (e.g. field "borrowerName" → calls getBorrowerName() on the row object).
     */
    private <T> void setupColumns(TableView<T> table, String... fields) {
        table.getColumns().clear();
        for (String field : fields) {
            String header = field.replaceAll("([a-z])([A-Z])", "$1 $2").toUpperCase();
            TableColumn<T, String> col = new TableColumn<>(header);
            col.setCellValueFactory(new PropertyValueFactory<>(field));
            table.getColumns().add(col);
        }
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    public static void main(String[] args) { launch(args); }
}