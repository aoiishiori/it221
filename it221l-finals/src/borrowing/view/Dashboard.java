package borrowing.view;

import borrowing.DAO.*;
import borrowing.Interface.*;
import borrowing.model.DataClass;
import borrowing.util.SessionContext;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Dashboard — main application window.
 *
 * Changes from original:
 *   1. viewEquipmentLogic()     — added search TextField + status ComboBox filter
 *   2. viewActivityRequests()   — added requesterName column; shows summary via Statement
 *   3. viewNewBorrowForm()      — NEW: full borrow checkout form for Custodian
 *   4. viewReturnForm()         — NEW: full return / condition-recording form for Custodian
 *   5. Custodian nav updated    — added "New Borrow", "Process Return", "Overdue Report"
 *   6. Admin nav updated        — added "Overdue Report"
 *   7. viewOverdueReport()      — NEW: calls sp_mark_overdue_transactions then shows results
 */
public class Dashboard extends Application {

    private final EquipmentInter      equipmentDAO = new Equipment();
    private final BorrowInter         borrowDAO    = new Borrow();
    private final PersonInter         personDAO    = new Person();
    private final LabClassInter       labClassDAO  = new LabClass();
    private final ActivityRequestInter activityDAO = new ActivityRequest();

    private String authenticatedRole;
    private String authenticatedUsername;
    private String authenticatedPersonName;
    private String loggedInUserId;

    private BorderPane mainLayout;

    // ----------------------------------------------------------------
    // Setters
    // ----------------------------------------------------------------
    public void setAuthenticatedRole(String role)     { this.authenticatedRole     = role; }
    public void setLoggedInUserId(String userId)      { this.loggedInUserId        = userId; }
    public void setAuthenticatedUsername(String uname){ this.authenticatedUsername = uname; }

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
        resolvePersonName();
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

    // ----------------------------------------------------------------
    // Sidebar
    // ----------------------------------------------------------------
    private void buildDashboard(String role) {
        VBox sidebar = new VBox(10);
        sidebar.setPadding(new Insets(25));
        sidebar.setStyle("-fx-background-color: #2c3e50;");
        sidebar.setPrefWidth(240);

        String displayText = (authenticatedPersonName != null)
                ? authenticatedPersonName + "\n(" + authenticatedUsername + ")"
                : "Logged in as: " + role;

        Label lblName = new Label("✓ " + displayText);
        lblName.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold; -fx-font-size: 12px;");
        Label lblRole = new Label("Role: " + role.toUpperCase());
        lblRole.setStyle("-fx-text-fill: #ecf0f1; -fx-font-size: 11px;");
        sidebar.getChildren().addAll(lblName, lblRole, new Separator());

        if ("CUSTODIAN".equalsIgnoreCase(role)) {
            addNav(sidebar, "Equipment Inventory",  this::viewEquipmentLogic);
            addNav(sidebar, "New Borrow",           this::viewNewBorrowForm);   // NEW
            addNav(sidebar, "Process Return",        this::viewReturnForm);      // NEW
            addNav(sidebar, "Transaction Records",   this::viewBorrowRecords);
            addNav(sidebar, "Overdue Report",        this::viewOverdueReport);   // NEW
            addNav(sidebar, "Class Schedules",       this::viewLabClasses);
            addNav(sidebar, "Student Directory",     this::viewStudents);
        } else if ("ADMIN".equalsIgnoreCase(role)) {
            addNav(sidebar, "Master Equipment List", this::viewEquipmentLogic);
            addNav(sidebar, "All Borrow Records",   this::viewBorrowRecords);
            addNav(sidebar, "Activity Approvals",   this::viewActivityRequests);
            addNav(sidebar, "Overdue Report",        this::viewOverdueReport);   // NEW
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
            SessionContext.getInstance().logout();
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
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ecf0f1; " +
                "-fx-alignment: center-left; -fx-font-size: 12px;");
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: #34495e; -fx-text-fill: white; -fx-alignment: center-left; -fx-font-size: 12px;"));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #ecf0f1; -fx-alignment: center-left; -fx-font-size: 12px;"));
        btn.setOnAction(e -> action.run());
        sidebar.getChildren().add(btn);
    }

    // ================================================================
    // VIEW: Equipment Inventory  (FIX: added search + status filter)
    // ================================================================
    private void viewEquipmentLogic() {
        VBox root = createContentRoot("Equipment Inventory");

        // --- Filter bar ---
        HBox filterBar = new HBox(10);
        filterBar.setPadding(new Insets(0, 0, 12, 0));
        filterBar.setAlignment(Pos.CENTER_LEFT);

        TextField txtSearch = new TextField();
        txtSearch.setPromptText("Search barcode or item name...");
        txtSearch.setPrefWidth(240);

        ComboBox<String> cbStatus = new ComboBox<>(FXCollections.observableArrayList(
                "ALL", "AVAILABLE", "BORROWED", "DAMAGED", "DECOMMISSIONED"));
        cbStatus.setValue("ALL");
        cbStatus.setPrefWidth(150);

        ComboBox<String> cbCategory = new ComboBox<>(FXCollections.observableArrayList(
                "ALL", "EQUIPMENT", "PERIPHERAL", "ACCESSORY"));
        cbCategory.setValue("ALL");
        cbCategory.setPrefWidth(150);

        Button btnSearch  = new Button("Search");
        Button btnRefresh = new Button("Clear / Refresh");

        filterBar.getChildren().addAll(
                new Label("Search:"), txtSearch,
                new Label("Status:"), cbStatus,
                new Label("Category:"), cbCategory,
                btnSearch, btnRefresh
        );

        // --- Table ---
        TableView<DataClass.Equipment> table = new TableView<>();
        setupColumns(table, "barcode", "itemName", "category", "brand", "model", "status", "remarks");

        ObservableList<DataClass.Equipment> all =
                FXCollections.observableArrayList(equipmentDAO.getAllEquipment());
        table.setItems(all);

        // Search action
        btnSearch.setOnAction(e -> {
            String keyword  = txtSearch.getText().trim();
            String status   = cbStatus.getValue();
            String category = cbCategory.getValue();

            List<DataClass.Equipment> results;

            // Start from keyword search if provided, otherwise all
            if (!keyword.isEmpty()) {
                results = equipmentDAO.searchEquipment("%" + keyword + "%");
            } else {
                results = equipmentDAO.getAllEquipment();
            }

            // Apply status filter
            if (!"ALL".equals(status)) {
                results = results.stream()
                        .filter(eq -> status.equalsIgnoreCase(eq.status))
                        .collect(java.util.stream.Collectors.toList());
            }

            // Apply category filter
            if (!"ALL".equals(category)) {
                results = results.stream()
                        .filter(eq -> category.equalsIgnoreCase(eq.category))
                        .collect(java.util.stream.Collectors.toList());
            }

            table.setItems(FXCollections.observableArrayList(results));
        });

        btnRefresh.setOnAction(e -> {
            txtSearch.clear();
            cbStatus.setValue("ALL");
            cbCategory.setValue("ALL");
            table.setItems(FXCollections.observableArrayList(equipmentDAO.getAllEquipment()));
        });

        root.getChildren().addAll(filterBar, table);
        mainLayout.setCenter(root);
    }

    // ================================================================
    // VIEW: Transaction Records
    // ================================================================
    private void viewBorrowRecords() {
        VBox root = createContentRoot("Borrow Transaction Records");

        HBox filterBar = new HBox(10);
        filterBar.setPadding(new Insets(0, 0, 12, 0));
        filterBar.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> cbStatus = new ComboBox<>(FXCollections.observableArrayList(
                "ALL", "BORROWED", "RETURNED", "RETURNED_WITH_ISSUE", "OVERDUE"));
        cbStatus.setValue("ALL");
        Button btnFilter  = new Button("Filter");
        Button btnRefresh = new Button("Clear");

        filterBar.getChildren().addAll(new Label("Status:"), cbStatus, btnFilter, btnRefresh);

        TableView<DataClass.BorrowRecord> table = new TableView<>();
        setupColumns(table, "transactionId", "borrowerName", "borrowDate", "expectedReturn", "returnDate", "status");

        table.setItems(FXCollections.observableArrayList(borrowDAO.getAllRecords()));

        btnFilter.setOnAction(e -> {
            String status = cbStatus.getValue();
            if ("ALL".equals(status))
                table.setItems(FXCollections.observableArrayList(borrowDAO.getAllRecords()));
            else
                table.setItems(FXCollections.observableArrayList(borrowDAO.getRecordsByStatus(status)));
        });

        btnRefresh.setOnAction(e -> {
            cbStatus.setValue("ALL");
            table.setItems(FXCollections.observableArrayList(borrowDAO.getAllRecords()));
        });

        root.getChildren().addAll(filterBar, table);
        mainLayout.setCenter(root);
    }

    // ================================================================
    // VIEW: Borrower's own history
    // ================================================================
    private void viewBorrowerHistoryLogic() {
        VBox root = createContentRoot("My Borrowing History");
        TableView<DataClass.BorrowRecord> table = new TableView<>();
        setupColumns(table, "transactionId", "borrowDate", "expectedReturn", "returnDate", "activityName", "status");

        if (loggedInUserId != null) {
            table.setItems(FXCollections.observableArrayList(
                    borrowDAO.getRecordsByBorrowerId(loggedInUserId)));
        } else {
            table.setPlaceholder(new Label("Session error: user ID not found."));
        }

        root.getChildren().add(table);
        mainLayout.setCenter(root);
    }

    // ================================================================
    // VIEW: Activity Approvals  (FIX: added requesterName column + summary)
    // ================================================================
    private void viewActivityRequests() {
        VBox root = createContentRoot("Activity Request Approvals");

        // Summary label via plain Statement (demonstrated in ActivityRequest.getActivitySummary())
        String summary = activityDAO.getActivitySummary();
        Label lblSummary = new Label("Summary — " + summary);
        lblSummary.setStyle("-fx-font-size: 12px; -fx-text-fill: #555; -fx-padding: 0 0 10 0;");

        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(0, 0, 12, 0));
        Button btnRefresh = new Button("Refresh");
        buttonBox.getChildren().add(btnRefresh);

        TableView<DataClass.ActivityRecord> table = new TableView<>();
        // FIX: added requesterName column
        setupColumns(table, "requestId", "activityName", "activityType", "activityDate",
                "requesterName", "status");

        // Approve / Reject action column
        TableColumn<DataClass.ActivityRecord, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button btnApprove = new Button("APPROVE");
            private final Button btnReject  = new Button("REJECT");
            private final HBox   container  = new HBox(6, btnApprove, btnReject);
            {
                btnApprove.setStyle("-fx-background-color:#27ae60;-fx-text-fill:white;-fx-font-size:10px;-fx-font-weight:bold;");
                btnReject .setStyle("-fx-background-color:#e74c3c;-fx-text-fill:white;-fx-font-size:10px;-fx-font-weight:bold;");
                container.setAlignment(Pos.CENTER);
                btnApprove.setOnAction(e -> handleStatusUpdate(
                        getTableView().getItems().get(getIndex()).getRequestId(), "APPROVED", table));
                btnReject.setOnAction(e -> handleStatusUpdate(
                        getTableView().getItems().get(getIndex()).getRequestId(), "REJECTED", table));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                DataClass.ActivityRecord rec = getTableView().getItems().get(getIndex());
                setGraphic("PENDING".equalsIgnoreCase(rec.getStatus()) ? container : null);
            }
        });
        table.getColumns().add(actionCol);
        table.setItems(FXCollections.observableArrayList(activityDAO.getAllActivities()));

        btnRefresh.setOnAction(e -> {
            table.setItems(FXCollections.observableArrayList(activityDAO.getAllActivities()));
            lblSummary.setText("Summary — " + activityDAO.getActivitySummary());
        });

        root.getChildren().addAll(lblSummary, buttonBox, table);
        mainLayout.setCenter(root);
    }

    private void handleStatusUpdate(int requestId, String status,
                                    TableView<DataClass.ActivityRecord> table) {
        String approverPersonId = (loggedInUserId != null) ? loggedInUserId : "STAFF-001";
        if (activityDAO.updateActivityStatus(requestId, status, approverPersonId)) {
            table.setItems(FXCollections.observableArrayList(activityDAO.getAllActivities()));
            new Alert(Alert.AlertType.INFORMATION, "Request " + status + ".").showAndWait();
        } else {
            new Alert(Alert.AlertType.ERROR,
                    "Update failed. The request may no longer be PENDING.").showAndWait();
        }
    }

    // ================================================================
    // VIEW: Students
    // ================================================================
    private void viewStudents() {
        VBox root = createContentRoot("Student Directory");
        TableView<DataClass.StudentRecord> table = new TableView<>();
        setupColumns(table, "personId", "lastName", "firstName", "email");
        table.setItems(FXCollections.observableArrayList(personDAO.getAllStudents()));
        root.getChildren().add(table);
        mainLayout.setCenter(root);
    }

    // ================================================================
    // VIEW: Lab Classes
    // ================================================================
    private void viewLabClasses() {
        VBox root = createContentRoot("Laboratory Class Schedules");
        TableView<DataClass.LabClassRecord> table = new TableView<>();
        setupColumns(table, "classId", "courseCode", "section", "semester",
                "schoolYear", "room", "schedule", "facultyName", "enrolledCount");
        table.setItems(FXCollections.observableArrayList(labClassDAO.getAllClasses()));
        root.getChildren().add(table);
        mainLayout.setCenter(root);
    }

    // ================================================================
    // VIEW: Overdue Report  (NEW — calls stored procedure)
    // ================================================================
    private void viewOverdueReport() {
        VBox root = createContentRoot("Overdue / Problematic Transactions");

        Label info = new Label("Clicking 'Run Check' calls the stored procedure " +
                "sp_mark_overdue_transactions() to update overdue statuses before displaying.");
        info.setStyle("-fx-font-size: 12px; -fx-text-fill: #666; -fx-wrap-text: true;");

        Button btnRun     = new Button("Run Overdue Check (Stored Procedure)");
        Button btnRefresh = new Button("Refresh List Only");

        btnRun.setStyle("-fx-background-color:#c0392b;-fx-text-fill:white;-fx-font-weight:bold;");

        HBox btnBar = new HBox(10, btnRun, btnRefresh);
        btnBar.setPadding(new Insets(8, 0, 12, 0));

        TableView<DataClass.UnreturnedRecord> table = new TableView<>();
        setupColumns(table, "transactionId", "borrowerName", "expectedReturn", "status", "items");

        Runnable reload = () -> table.setItems(FXCollections.observableArrayList(
                borrowDAO.getAllProblematicRecords()));

        btnRun.setOnAction(e -> {
            borrowDAO.markOverdueTransactions(); // CallableStatement → stored procedure
            reload.run();
            new Alert(Alert.AlertType.INFORMATION,
                    "Overdue check complete. Statuses updated.").showAndWait();
        });

        btnRefresh.setOnAction(e -> reload.run());
        reload.run();

        root.getChildren().addAll(info, btnBar, table);
        mainLayout.setCenter(root);
    }

    // ================================================================
    // VIEW: New Borrow Form  (NEW — core transaction, Custodian only)
    // ================================================================
    private void viewNewBorrowForm() {
        VBox root = createContentRoot("New Borrow Transaction");

        GridPane form = new GridPane();
        form.setHgap(16);
        form.setVgap(12);

        // --- Borrower ID ---
        TextField txtBorrowerId = new TextField();
        txtBorrowerId.setPromptText("e.g. 2021-00001 or FAC-001");
        Label lblBorrowerInfo = new Label();
        lblBorrowerInfo.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 11px;");

        Button btnLookup = new Button("Look Up");
        btnLookup.setOnAction(e -> {
            String pid = txtBorrowerId.getText().trim();
            if (pid.isEmpty()) {
                lblBorrowerInfo.setText("Enter a person ID first.");
                return;
            }
            String name = personDAO.getPersonName(pid);
            if (name == null) {
                lblBorrowerInfo.setStyle("-fx-text-fill:#e74c3c; -fx-font-size:11px;");
                lblBorrowerInfo.setText("Person ID not found.");
                return;
            }
            int active = borrowDAO.countActiveBorrows(pid);  // calls fn_count_active_borrows
            if (active > 0) {
                lblBorrowerInfo.setStyle("-fx-text-fill:#e67e22; -fx-font-size:11px;");
                lblBorrowerInfo.setText(name + " — WARNING: " + active +
                        " active/overdue borrow(s). Confirm before proceeding.");
            } else {
                lblBorrowerInfo.setStyle("-fx-text-fill:#27ae60; -fx-font-size:11px;");
                lblBorrowerInfo.setText(name + " — Clear to borrow.");
            }
        });

        // --- Context: Class OR Activity Request ---
        ToggleGroup tgContext = new ToggleGroup();
        RadioButton rbClass   = new RadioButton("Class-based");
        RadioButton rbRequest = new RadioButton("Activity Request-based");
        RadioButton rbWalkIn  = new RadioButton("Walk-in (no link)");
        rbClass.setToggleGroup(tgContext);
        rbRequest.setToggleGroup(tgContext);
        rbWalkIn.setToggleGroup(tgContext);
        rbWalkIn.setSelected(true);
        HBox radioBox = new HBox(10, rbClass, rbRequest, rbWalkIn);

        TextField txtClassId    = new TextField();
        txtClassId.setPromptText("e.g. IT221L-A");
        txtClassId.setDisable(true);

        TextField txtRequestId  = new TextField();
        txtRequestId.setPromptText("Activity Request ID (integer)");
        txtRequestId.setDisable(true);

        rbClass.setOnAction(e -> {
            txtClassId.setDisable(false);
            txtRequestId.setDisable(true);
            txtRequestId.clear();
        });
        rbRequest.setOnAction(e -> {
            txtClassId.setDisable(true);
            txtClassId.clear();
            txtRequestId.setDisable(false);
        });
        rbWalkIn.setOnAction(e -> {
            txtClassId.setDisable(true);
            txtClassId.clear();
            txtRequestId.setDisable(true);
            txtRequestId.clear();
        });

        // --- Expected Return Date ---
        DatePicker dpReturn = new DatePicker(LocalDate.now());
        dpReturn.setDayCellFactory(picker -> new DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });

        // --- Barcode list ---
        TextArea txtBarcodes = new TextArea();
        txtBarcodes.setPromptText("Enter one barcode per line:\nEQ-0001\nEQ-0009");
        txtBarcodes.setPrefHeight(100);

        // --- Form layout ---
        int row = 0;
        form.add(new Label("Borrower ID:"),    0, row); form.add(txtBorrowerId, 1, row); form.add(btnLookup, 2, row++);
        form.add(lblBorrowerInfo,              1, row++, 2, 1);
        form.add(new Label("Context:"),        0, row); form.add(radioBox, 1, row++, 2, 1);
        form.add(new Label("Class ID:"),       0, row); form.add(txtClassId, 1, row++);
        form.add(new Label("Request ID:"),     0, row); form.add(txtRequestId, 1, row++);
        form.add(new Label("Expected Return:"),0, row); form.add(dpReturn, 1, row++);
        form.add(new Label("Barcodes\n(one per line):"), 0, row); form.add(txtBarcodes, 1, row++, 2, 1);

        // --- Submit ---
        Button btnSubmit = new Button("CONFIRM BORROW");
        btnSubmit.setStyle("-fx-background-color:#27ae60;-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:13px;");

        btnSubmit.setOnAction(e -> {
            // Validate borrower
            String borrowerId = txtBorrowerId.getText().trim();
            if (borrowerId.isEmpty()) {
                showError("Borrower ID is required."); return;
            }
            if (personDAO.getPersonName(borrowerId) == null) {
                showError("Borrower ID does not exist."); return;
            }

            // Parse barcodes
            String[] lines = txtBarcodes.getText().split("\\n");
            List<String> barcodes = new ArrayList<>();
            for (String line : lines) {
                String bc = line.trim();
                if (!bc.isEmpty()) {
                    // Validate barcode length (VARCHAR 30)
                    if (bc.length() > 30) {
                        showError("Barcode '" + bc + "' exceeds 30 characters."); return;
                    }
                    barcodes.add(bc);
                }
            }
            if (barcodes.isEmpty()) {
                showError("At least one barcode is required."); return;
            }

            // Validate expected return
            if (dpReturn.getValue() == null || dpReturn.getValue().isBefore(LocalDate.now())) {
                showError("Expected return date must be today or later."); return;
            }

            // Parse optional class / request
            String  classId   = rbClass.isSelected()   ? txtClassId.getText().trim() : null;
            Integer requestId = null;
            if (rbRequest.isSelected() && !txtRequestId.getText().trim().isEmpty()) {
                try {
                    requestId = Integer.parseInt(txtRequestId.getText().trim());
                } catch (NumberFormatException nfe) {
                    showError("Request ID must be a whole number."); return;
                }
            }

            boolean ok = borrowDAO.createBorrowTransaction(
                    borrowerId,
                    loggedInUserId,           // custodian = currently logged-in user
                    classId,
                    requestId,
                    dpReturn.getValue().toString(),
                    barcodes
            );

            if (ok) {
                new Alert(Alert.AlertType.INFORMATION,
                        "Borrow transaction created successfully!").showAndWait();
                viewEquipmentLogic(); // navigate back to inventory
            } else {
                showError("Transaction failed. One or more items may not be AVAILABLE, " +
                        "or the borrower / class / request ID does not exist.");
            }
        });

        root.getChildren().addAll(form, btnSubmit);
        mainLayout.setCenter(root);
    }

    // ================================================================
    // VIEW: Return / Condition Form  (NEW)
    // ================================================================
    private void viewReturnForm() {
        VBox root = createContentRoot("Process Equipment Return");

        // Step 1: look up open transaction
        GridPane step1 = new GridPane();
        step1.setHgap(12); step1.setVgap(10);

        TextField txtTxnId = new TextField();
        txtTxnId.setPromptText("Transaction ID (integer)");
        Button btnLoad = new Button("Load Transaction");
        step1.add(new Label("Transaction ID:"), 0, 0);
        step1.add(txtTxnId, 1, 0);
        step1.add(btnLoad, 2, 0);

        Label lblTxnInfo = new Label("Enter a transaction ID and click Load.");
        lblTxnInfo.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");

        // Step 2: condition table (populated after load)
        VBox itemsBox = new VBox(8);

        // Step 3: overall status + submit
        ComboBox<String> cbOverallStatus = new ComboBox<>(FXCollections.observableArrayList(
                "RETURNED", "RETURNED_WITH_ISSUE"));
        cbOverallStatus.setValue("RETURNED");
        cbOverallStatus.setDisable(true);

        Button btnReturn = new Button("CONFIRM RETURN");
        btnReturn.setStyle("-fx-background-color:#2980b9;-fx-text-fill:white;-fx-font-weight:bold;");
        btnReturn.setDisable(true);

        // State holder
        final int[]    loadedTxnId = {-1};
        final List<DataClass.BorrowItem>    loadedItems   = new ArrayList<>();
        final List<ComboBox<String>>        conditionBoxes = new ArrayList<>();
        final List<TextField>               damageFields   = new ArrayList<>();

        btnLoad.setOnAction(e -> {
            String idStr = txtTxnId.getText().trim();
            if (idStr.isEmpty()) { showError("Transaction ID required."); return; }

            int txnId;
            try { txnId = Integer.parseInt(idStr); }
            catch (NumberFormatException ex) { showError("Transaction ID must be an integer."); return; }

            List<DataClass.BorrowItem> items = borrowDAO.getItemsByTransactionId(txnId);
            if (items.isEmpty()) {
                lblTxnInfo.setText("No items found for transaction " + txnId +
                        ". It may already be returned or does not exist.");
                return;
            }

            // Check it is still open
            List<DataClass.BorrowRecord> records = borrowDAO.getRecordsByBorrowerId(""); // workaround
            // Simpler: check the items' conditionIn — if all are filled the transaction was returned
            boolean alreadyReturned = items.stream()
                    .allMatch(i -> i.conditionIn != null && !i.conditionIn.equals("NOT RETURNED"));
            if (alreadyReturned) {
                lblTxnInfo.setText("Transaction " + txnId + " has already been returned.");
                return;
            }

            loadedTxnId[0] = txnId;
            loadedItems.clear(); loadedItems.addAll(items);
            conditionBoxes.clear(); damageFields.clear();
            itemsBox.getChildren().clear();

            lblTxnInfo.setText("Transaction " + txnId + " — " + items.size() + " item(s) loaded:");

            for (DataClass.BorrowItem item : items) {
                Label lbl = new Label(item.barcode + " — " + item.itemName +
                        " (out condition: " + item.conditionOut + ")");
                lbl.setStyle("-fx-font-weight:bold;");

                ComboBox<String> cbCond = new ComboBox<>(FXCollections.observableArrayList("Good", "Damaged"));
                cbCond.setValue("Good");

                TextField tfNotes = new TextField();
                tfNotes.setPromptText("Damage notes (if any)");
                tfNotes.setDisable(true);

                cbCond.setOnAction(ev -> tfNotes.setDisable(!"Damaged".equals(cbCond.getValue())));

                conditionBoxes.add(cbCond);
                damageFields.add(tfNotes);

                HBox row = new HBox(10, lbl, new Label("Condition:"), cbCond, tfNotes);
                row.setAlignment(Pos.CENTER_LEFT);
                itemsBox.getChildren().add(row);
            }

            cbOverallStatus.setDisable(false);
            btnReturn.setDisable(false);
        });

        btnReturn.setOnAction(e -> {
            if (loadedTxnId[0] == -1) { showError("Load a transaction first."); return; }

            List<DataClass.BorrowItemReturnInfo> returnItems = new ArrayList<>();
            for (int i = 0; i < loadedItems.size(); i++) {
                String cond   = conditionBoxes.get(i).getValue();
                String notes  = damageFields.get(i).getText().trim();
                // Validation: damage notes max VARCHAR(255)
                if (notes.length() > 255) { showError("Damage notes exceed 255 characters."); return; }
                returnItems.add(new DataClass.BorrowItemReturnInfo(
                        loadedItems.get(i).barcode, cond,
                        notes.isEmpty() ? null : notes));
            }

            // Auto-set status: if any item is Damaged → RETURNED_WITH_ISSUE
            boolean hasDamage = returnItems.stream()
                    .anyMatch(ri -> "Damaged".equalsIgnoreCase(ri.conditionIn));
            String finalStatus = hasDamage ? "RETURNED_WITH_ISSUE" : cbOverallStatus.getValue();

            boolean ok = borrowDAO.returnBorrowTransaction(
                    loadedTxnId[0], loggedInUserId, finalStatus, returnItems);

            if (ok) {
                new Alert(Alert.AlertType.INFORMATION,
                        "Return processed. Status: " + finalStatus).showAndWait();
                viewEquipmentLogic();
            } else {
                showError("Return failed. Please try again.");
            }
        });

        Separator sep = new Separator();
        sep.setPadding(new Insets(8, 0, 8, 0));

        HBox statusRow = new HBox(12, new Label("Overall Status:"), cbOverallStatus, btnReturn);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        statusRow.setPadding(new Insets(12, 0, 0, 0));

        root.getChildren().addAll(step1, lblTxnInfo, new Separator(), itemsBox, statusRow);
        mainLayout.setCenter(root);
    }

    // ================================================================
    // VIEW: Borrower — Request New Activity
    // ================================================================
    private void viewRequestForm() {
        VBox root = createContentRoot("Submit New Activity Request");
        GridPane form = new GridPane();
        form.setHgap(20); form.setVgap(15);

        TextField txtActivityName = new TextField();
        txtActivityName.setPromptText("Enter Activity Name (max 100 chars)");

        ComboBox<String> cbType = new ComboBox<>(FXCollections.observableArrayList(
                "RECRUITMENT", "CERTIFICATION", "MEETING", "TRAINING", "OTHER"));
        cbType.setValue("TRAINING"); cbType.setPrefWidth(200);

        DatePicker datePicker = new DatePicker();
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
                if (date.isBefore(LocalDate.now()))
                    setStyle("-fx-background-color:#f2f2f2;-fx-text-fill:#b3b3b3;");
            }
        });

        TextField txtLocation = new TextField();
        txtLocation.setPromptText("Room/Facility Name (max 100 chars)");

        TextArea txtNotes = new TextArea();
        txtNotes.setPromptText("Optional notes (max 255 chars)");
        txtNotes.setPrefHeight(80);

        form.add(new Label("Activity Name:"), 0, 0); form.add(txtActivityName, 1, 0);
        form.add(new Label("Activity Type:"), 0, 1); form.add(cbType, 1, 1);
        form.add(new Label("Activity Date:"), 0, 2); form.add(datePicker, 1, 2);
        form.add(new Label("Location:"),      0, 3); form.add(txtLocation, 1, 3);
        form.add(new Label("Notes:"),         0, 4); form.add(txtNotes, 1, 4);

        Button btnSubmit = new Button("SUBMIT REQUEST");
        btnSubmit.setStyle("-fx-background-color:#27ae60;-fx-text-fill:white;-fx-font-weight:bold;");

        btnSubmit.setOnAction(e -> {
            if (txtActivityName.getText().trim().isEmpty()) {
                showError("Activity name is required."); return;
            }
            if (txtActivityName.getText().length() > 100) {
                showError("Activity name must not exceed 100 characters."); return;
            }
            if (datePicker.getValue() == null) {
                showError("Please select a valid future date."); return;
            }
            if (txtLocation.getText().length() > 100) {
                showError("Location must not exceed 100 characters."); return;
            }
            if (txtNotes.getText().length() > 255) {
                showError("Notes must not exceed 255 characters."); return;
            }

            int result = activityDAO.addActivity(
                    txtActivityName.getText().trim(),
                    cbType.getValue(),
                    datePicker.getValue().toString(),
                    txtLocation.getText().trim(),
                    loggedInUserId,
                    txtNotes.getText().trim()
            );

            if (result != -1) {
                new Alert(Alert.AlertType.INFORMATION, "Request submitted! ID: " + result).showAndWait();
                viewBorrowerHistoryLogic();
            } else {
                showError("Submission failed. Please check your inputs.");
            }
        });

        root.getChildren().addAll(form, btnSubmit);
        mainLayout.setCenter(root);
    }

    // ================================================================
    // Helpers
    // ================================================================
    private VBox createContentRoot(String title) {
        VBox root = new VBox(16);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: white;");
        Label lbl = new Label(title);
        lbl.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:#2c3e50;");
        root.getChildren().add(lbl);
        return root;
    }

    /** Converts camelCase field name → readable column header, wires PropertyValueFactory. */
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

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }

    public static void main(String[] args) { launch(args); }
}