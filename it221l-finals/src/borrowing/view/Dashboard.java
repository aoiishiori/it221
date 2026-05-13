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
import java.util.stream.Collectors;

/**
 * Dashboard — all UI fixes applied:
 *
 *   FIX 1  viewBorrowRecords()       — added borrowerId + classId + activityName columns
 *                                       so Admin can identify who is borrowing.
 *   FIX 2  viewEquipmentLogic()      — added Exact barcode search mode toggle.
 *                                       Refresh button reloads live data from DB.
 *   FIX 3  viewNewBorrowForm()       — replaced textarea with a two-pane equipment picker:
 *                                       left = AVAILABLE items (live from DB),
 *                                       right = selected items. No more manual barcode entry.
 *                                       Success alert now shows the generated Transaction ID.
 *   FIX 4  viewReturnForm()          — replaced broken "already returned" workaround with
 *                                       getTransactionById() which checks status properly.
 *                                       Also added a dropdown of open transactions at the top
 *                                       so the Custodian can pick one instead of typing IDs.
 *   FIX 5  viewBorrowerHistoryLogic()— added classId column + "View Items" button per row.
 *   FIX 6  viewActivityRequests()    — added "View Borrows" action button per activity row
 *                                       so Admin can see which borrows belong to an activity.
 *   FIX 7  viewOverdueReport()       — no logic change; kept as-is (already correct).
 */
public class Dashboard extends Application {

    private final EquipmentInter       equipmentDAO = new Equipment();
    private final BorrowInter          borrowDAO    = new Borrow();
    private final PersonInter          personDAO    = new Person();
    private final LabClassInter        labClassDAO  = new LabClass();
    private final ActivityRequestInter activityDAO  = new ActivityRequest();

    private String authenticatedRole;
    private String authenticatedUsername;
    private String authenticatedPersonName;
    private String loggedInUserId;

    private BorderPane mainLayout;

    // ----------------------------------------------------------------
    // Setters
    // ----------------------------------------------------------------
    public void setAuthenticatedRole(String role)      { this.authenticatedRole     = role; }
    public void setLoggedInUserId(String userId)       { this.loggedInUserId        = userId; }
    public void setAuthenticatedUsername(String uname) { this.authenticatedUsername = uname; }

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
            addNav(sidebar, "New Borrow",           this::viewNewBorrowForm);
            addNav(sidebar, "Process Return",        this::viewReturnForm);
            addNav(sidebar, "Transaction Records",   this::viewBorrowRecords);
            addNav(sidebar, "Overdue Report",        this::viewOverdueReport);
            addNav(sidebar, "Class Schedules",       this::viewLabClasses);
            addNav(sidebar, "Student Directory",     this::viewStudents);
        } else if ("ADMIN".equalsIgnoreCase(role)) {
            addNav(sidebar, "Master Equipment List", this::viewEquipmentLogic);
            addNav(sidebar, "All Borrow Records",    this::viewBorrowRecords);
            addNav(sidebar, "Activity Approvals",    this::viewActivityRequests);
            addNav(sidebar, "Overdue Report",        this::viewOverdueReport);
        } else if ("BORROWER".equalsIgnoreCase(role)) {
            addNav(sidebar, "My Borrow History",     this::viewBorrowerHistoryLogic);
            addNav(sidebar, "Request New Activity",  this::viewRequestForm);
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
    // FIX 2 — Equipment Inventory: exact-match toggle + live refresh
    // ================================================================
    private void viewEquipmentLogic() {
        VBox root = createContentRoot("Equipment Inventory");

        HBox filterBar = new HBox(10);
        filterBar.setPadding(new Insets(0, 0, 12, 0));
        filterBar.setAlignment(Pos.CENTER_LEFT);

        TextField txtSearch = new TextField();
        txtSearch.setPromptText("Search barcode or item name...");
        txtSearch.setPrefWidth(220);

        // FIX 2: search mode toggle
        ToggleGroup tgMode = new ToggleGroup();
        RadioButton rbContains = new RadioButton("Contains");
        RadioButton rbExact    = new RadioButton("Exact barcode");
        rbContains.setToggleGroup(tgMode);
        rbExact.setToggleGroup(tgMode);
        rbContains.setSelected(true);
        HBox modeBox = new HBox(8, rbContains, rbExact);
        modeBox.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> cbStatus = new ComboBox<>(FXCollections.observableArrayList(
                "ALL", "AVAILABLE", "BORROWED", "DAMAGED", "DECOMMISSIONED"));
        cbStatus.setValue("ALL");
        cbStatus.setPrefWidth(140);

        ComboBox<String> cbCategory = new ComboBox<>(FXCollections.observableArrayList(
                "ALL", "EQUIPMENT", "PERIPHERAL", "ACCESSORY"));
        cbCategory.setValue("ALL");
        cbCategory.setPrefWidth(130);

        Button btnSearch  = new Button("Search");
        // FIX 2: refresh hits DB fresh every time
        Button btnRefresh = new Button("↺ Refresh");

        filterBar.getChildren().addAll(
                new Label("Search:"), txtSearch, modeBox,
                new Label("Status:"), cbStatus,
                new Label("Category:"), cbCategory,
                btnSearch, btnRefresh
        );

        TableView<DataClass.Equipment> table = new TableView<>();
        setupColumns(table, "barcode", "itemName", "category", "brand", "model", "status", "remarks");

        // Load live on every entry to this tab
        Runnable reloadAll = () ->
                table.setItems(FXCollections.observableArrayList(equipmentDAO.getAllEquipment()));
        reloadAll.run();

        btnSearch.setOnAction(e -> {
            String keyword  = txtSearch.getText().trim();
            String status   = cbStatus.getValue();
            String category = cbCategory.getValue();

            List<DataClass.Equipment> results;

            if (!keyword.isEmpty()) {
                if (rbExact.isSelected()) {
                    // FIX 2: exact barcode match
                    results = equipmentDAO.getEquipmentByExactBarcode(keyword);
                } else {
                    results = equipmentDAO.searchEquipment("%" + keyword + "%");
                }
            } else {
                results = equipmentDAO.getAllEquipment();
            }

            if (!"ALL".equals(status)) {
                results = results.stream()
                        .filter(eq -> status.equalsIgnoreCase(eq.status))
                        .collect(Collectors.toList());
            }
            if (!"ALL".equals(category)) {
                results = results.stream()
                        .filter(eq -> category.equalsIgnoreCase(eq.category))
                        .collect(Collectors.toList());
            }
            table.setItems(FXCollections.observableArrayList(results));
        });

        btnRefresh.setOnAction(e -> {
            txtSearch.clear();
            cbStatus.setValue("ALL");
            cbCategory.setValue("ALL");
            rbContains.setSelected(true);
            reloadAll.run(); // FIX 2: always hits DB
        });

        root.getChildren().addAll(filterBar, table);
        mainLayout.setCenter(root);
    }

    // ================================================================
    // FIX 1 — Transaction Records: added borrowerId, classId, activityName columns
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
        Button btnRefresh = new Button("↺ Refresh");

        filterBar.getChildren().addAll(new Label("Status:"), cbStatus, btnFilter, btnRefresh);

        TableView<DataClass.BorrowRecord> table = new TableView<>();
        // FIX 1: borrowerId + classId + activityName added so Admin sees full identity
        setupColumns(table,
                "transactionId", "borrowerName", "borrowerId",
                "classId", "activityName",
                "borrowDate", "expectedReturn", "returnDate", "status");

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
    // FIX 5 — Borrower history: classId column + "View Items" per row
    // ================================================================
    private void viewBorrowerHistoryLogic() {
        VBox root = createContentRoot("My Borrowing History");

        TableView<DataClass.BorrowRecord> table = new TableView<>();
        // FIX 5: classId added; activityName retained
        setupColumns(table,
                "transactionId", "classId", "activityName",
                "borrowDate", "expectedReturn", "returnDate", "status");

        if (loggedInUserId != null) {
            table.setItems(FXCollections.observableArrayList(
                    borrowDAO.getRecordsByBorrowerId(loggedInUserId)));
        } else {
            table.setPlaceholder(new Label("Session error: user ID not found."));
        }

        // FIX 5: "View Items" button column
        TableColumn<DataClass.BorrowRecord, Void> itemsCol = new TableColumn<>("Items");
        itemsCol.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("View Items");
            {
                btn.setStyle("-fx-font-size:10px;");
                btn.setOnAction(e -> {
                    DataClass.BorrowRecord rec = getTableView().getItems().get(getIndex());
                    showItemsPopup(rec.getTransactionId(), "Transaction #" + rec.getTransactionId());
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
        table.getColumns().add(itemsCol);

        Button btnRefresh = new Button("↺ Refresh");
        btnRefresh.setOnAction(e -> {
            if (loggedInUserId != null)
                table.setItems(FXCollections.observableArrayList(
                        borrowDAO.getRecordsByBorrowerId(loggedInUserId)));
        });

        root.getChildren().addAll(btnRefresh, table);
        mainLayout.setCenter(root);
    }

    // ================================================================
    // FIX 6 — Activity Approvals: "View Borrows" button per activity
    // ================================================================
    private void viewActivityRequests() {
        VBox root = createContentRoot("Activity Request Approvals");

        String summary = activityDAO.getActivitySummary();
        Label lblSummary = new Label("Summary — " + summary);
        lblSummary.setStyle("-fx-font-size: 12px; -fx-text-fill: #555; -fx-padding: 0 0 10 0;");

        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(0, 0, 12, 0));
        Button btnRefresh = new Button("↺ Refresh");
        buttonBox.getChildren().add(btnRefresh);

        TableView<DataClass.ActivityRecord> table = new TableView<>();
        setupColumns(table, "requestId", "activityName", "activityType",
                "activityDate", "requesterName", "status");

        // Approve / Reject column
        TableColumn<DataClass.ActivityRecord, Void> approveCol = new TableColumn<>("Approve/Reject");
        approveCol.setCellFactory(param -> new TableCell<>() {
            private final Button btnApprove = new Button("APPROVE");
            private final Button btnReject  = new Button("REJECT");
            private final HBox   container  = new HBox(6, btnApprove, btnReject);
            {
                btnApprove.setStyle("-fx-background-color:#27ae60;-fx-text-fill:white;-fx-font-size:10px;-fx-font-weight:bold;");
                btnReject .setStyle("-fx-background-color:#e74c3c;-fx-text-fill:white;-fx-font-size:10px;-fx-font-weight:bold;");
                container.setAlignment(Pos.CENTER);
                btnApprove.setOnAction(e -> handleStatusUpdate(
                        getTableView().getItems().get(getIndex()).getRequestId(), "APPROVED", table));
                btnReject .setOnAction(e -> handleStatusUpdate(
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

        // FIX 6: "View Borrows" button per activity row
        TableColumn<DataClass.ActivityRecord, Void> borrowsCol = new TableColumn<>("Borrows");
        borrowsCol.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("View Borrows");
            {
                btn.setStyle("-fx-font-size:10px;");
                btn.setOnAction(e -> {
                    DataClass.ActivityRecord rec = getTableView().getItems().get(getIndex());
                    showActivityBorrowsPopup(rec.getRequestId(), rec.getActivityName());
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        table.getColumns().addAll(approveCol, borrowsCol);
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
    // Students / Lab Classes
    // ================================================================
    private void viewStudents() {
        VBox root = createContentRoot("Student Directory");
        TableView<DataClass.StudentRecord> table = new TableView<>();
        setupColumns(table, "personId", "lastName", "firstName", "email");
        table.setItems(FXCollections.observableArrayList(personDAO.getAllStudents()));
        root.getChildren().add(table);
        mainLayout.setCenter(root);
    }

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
    // Overdue Report — calls stored procedure
    // ================================================================
    private void viewOverdueReport() {
        VBox root = createContentRoot("Overdue / Problematic Transactions");

        Label info = new Label(
                "Click 'Run Overdue Check' to invoke sp_mark_overdue_transactions() via " +
                        "CallableStatement. This updates any BORROWED transaction whose expected_return " +
                        "< today to OVERDUE in the database. The table then reloads automatically.");
        info.setStyle("-fx-font-size: 12px; -fx-text-fill: #666; -fx-wrap-text: true;");

        Button btnRun     = new Button("Run Overdue Check (Stored Procedure)");
        Button btnRefresh = new Button("↺ Refresh List Only");
        btnRun.setStyle("-fx-background-color:#c0392b;-fx-text-fill:white;-fx-font-weight:bold;");

        HBox btnBar = new HBox(10, btnRun, btnRefresh);
        btnBar.setPadding(new Insets(8, 0, 12, 0));

        TableView<DataClass.UnreturnedRecord> table = new TableView<>();
        setupColumns(table, "transactionId", "borrowerName", "borrowerId",
                "expectedReturn", "status", "items");

        Runnable reload = () -> table.setItems(
                FXCollections.observableArrayList(borrowDAO.getAllProblematicRecords()));

        btnRun.setOnAction(e -> {
            borrowDAO.markOverdueTransactions();
            reload.run();
            new Alert(Alert.AlertType.INFORMATION,
                    "Overdue check complete. Statuses updated in borrow_transaction.\n" +
                            "Transaction Records and Borrower History will reflect changes on next load.")
                    .showAndWait();
        });
        btnRefresh.setOnAction(e -> reload.run());
        reload.run();

        root.getChildren().addAll(info, btnBar, table);
        mainLayout.setCenter(root);
    }

    // ================================================================
    // FIX 3 — New Borrow Form: two-pane equipment picker replaces textarea
    // ================================================================
    private void viewNewBorrowForm() {
        VBox root = createContentRoot("New Borrow Transaction");

        // ---- Borrower lookup row ----
        GridPane topForm = new GridPane();
        topForm.setHgap(16); topForm.setVgap(12);

        TextField txtBorrowerId = new TextField();
        txtBorrowerId.setPromptText("e.g. 2021-00001 or FAC-001");
        Label lblBorrowerInfo = new Label("Enter a person ID and click Look Up.");
        lblBorrowerInfo.setStyle("-fx-font-size: 11px; -fx-text-fill: #555;");

        Button btnLookup = new Button("Look Up");
        btnLookup.setOnAction(e -> {
            String pid = txtBorrowerId.getText().trim();
            if (pid.isEmpty()) { lblBorrowerInfo.setText("Enter a person ID first."); return; }
            String name = personDAO.getPersonName(pid);
            if (name == null) {
                lblBorrowerInfo.setStyle("-fx-text-fill:#e74c3c; -fx-font-size:11px;");
                lblBorrowerInfo.setText("Person ID not found.");
                return;
            }
            int active = borrowDAO.countActiveBorrows(pid);
            if (active > 0) {
                lblBorrowerInfo.setStyle("-fx-text-fill:#e67e22; -fx-font-size:11px;");
                lblBorrowerInfo.setText(name + " — WARNING: " + active + " active/overdue borrow(s).");
            } else {
                lblBorrowerInfo.setStyle("-fx-text-fill:#27ae60; -fx-font-size:11px;");
                lblBorrowerInfo.setText(name + " — Clear to borrow.");
            }
        });

        // Context selection
        ToggleGroup tgContext = new ToggleGroup();
        RadioButton rbClass   = new RadioButton("Class-based");
        RadioButton rbRequest = new RadioButton("Activity Request");
        RadioButton rbWalkIn  = new RadioButton("Walk-in");
        rbClass.setToggleGroup(tgContext);
        rbRequest.setToggleGroup(tgContext);
        rbWalkIn.setToggleGroup(tgContext);
        rbWalkIn.setSelected(true);
        HBox radioBox = new HBox(10, rbClass, rbRequest, rbWalkIn);

        TextField txtClassId   = new TextField(); txtClassId.setPromptText("e.g. IT221L-A"); txtClassId.setDisable(true);
        TextField txtRequestId = new TextField(); txtRequestId.setPromptText("Request ID (integer)"); txtRequestId.setDisable(true);

        rbClass.setOnAction(e -> { txtClassId.setDisable(false); txtRequestId.setDisable(true); txtRequestId.clear(); });
        rbRequest.setOnAction(e -> { txtClassId.setDisable(true); txtClassId.clear(); txtRequestId.setDisable(false); });
        rbWalkIn.setOnAction(e -> { txtClassId.setDisable(true); txtClassId.clear(); txtRequestId.setDisable(true); txtRequestId.clear(); });

        DatePicker dpReturn = new DatePicker(LocalDate.now());
        dpReturn.setDayCellFactory(picker -> new DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });

        int row = 0;
        topForm.add(new Label("Borrower ID:"),     0, row); topForm.add(txtBorrowerId, 1, row); topForm.add(btnLookup, 2, row++);
        topForm.add(lblBorrowerInfo,               1, row++, 2, 1);
        topForm.add(new Label("Context:"),         0, row); topForm.add(radioBox, 1, row++, 2, 1);
        topForm.add(new Label("Class ID:"),        0, row); topForm.add(txtClassId, 1, row++);
        topForm.add(new Label("Request ID:"),      0, row); topForm.add(txtRequestId, 1, row++);
        topForm.add(new Label("Expected Return:"), 0, row); topForm.add(dpReturn, 1, row++);

        // ---- FIX 3: Two-pane equipment picker ----
        Label lblPickerTitle = new Label("Select Equipment (double-click to add/remove):");
        lblPickerTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 10 0 4 0;");

        // Left: available equipment (live from DB)
        Label lblAvailable = new Label("Available Equipment");
        lblAvailable.setStyle("-fx-font-weight: bold;");

        TableView<DataClass.Equipment> tblAvailable = new TableView<>();
        setupColumns(tblAvailable, "barcode", "itemName", "category", "brand", "model");
        tblAvailable.setPrefHeight(220);

        Button btnRefreshPicker = new Button("↺ Reload Available");
        Runnable reloadAvailable = () ->
                tblAvailable.setItems(FXCollections.observableArrayList(equipmentDAO.getAvailableEquipment()));
        reloadAvailable.run();
        btnRefreshPicker.setOnAction(e -> reloadAvailable.run());

        // Right: selected items
        Label lblSelected = new Label("Selected Items");
        lblSelected.setStyle("-fx-font-weight: bold;");

        ObservableList<DataClass.Equipment> selectedItems = FXCollections.observableArrayList();
        TableView<DataClass.Equipment> tblSelected = new TableView<>();
        setupColumns(tblSelected, "barcode", "itemName", "category");
        tblSelected.setPrefHeight(220);
        tblSelected.setItems(selectedItems);

        // Double-click available → add to selected (and remove from available list display)
        tblAvailable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                DataClass.Equipment item = tblAvailable.getSelectionModel().getSelectedItem();
                if (item != null && !selectedItems.contains(item)) {
                    selectedItems.add(item);
                    tblAvailable.getItems().remove(item);
                }
            }
        });

        // Double-click selected → remove (return to available display)
        tblSelected.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                DataClass.Equipment item = tblSelected.getSelectionModel().getSelectedItem();
                if (item != null) {
                    selectedItems.remove(item);
                    tblAvailable.getItems().add(item);
                    FXCollections.sort(tblAvailable.getItems(),
                            (a, b) -> a.barcode.compareTo(b.barcode));
                }
            }
        });

        VBox leftPane  = new VBox(6, lblAvailable, btnRefreshPicker, tblAvailable);
        VBox rightPane = new VBox(6, lblSelected,
                new Label("(Double-click to remove)"), tblSelected);
        leftPane.setPrefWidth(500);
        rightPane.setPrefWidth(350);

        HBox pickerPane = new HBox(16, leftPane, rightPane);

        // ---- Submit ----
        Button btnSubmit = new Button("CONFIRM BORROW");
        btnSubmit.setStyle("-fx-background-color:#27ae60;-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:13px;");
        btnSubmit.setPadding(new Insets(10, 20, 10, 20));

        btnSubmit.setOnAction(e -> {
            String borrowerId = txtBorrowerId.getText().trim();
            if (borrowerId.isEmpty()) { showError("Borrower ID is required."); return; }
            if (personDAO.getPersonName(borrowerId) == null) { showError("Borrower ID does not exist."); return; }
            if (selectedItems.isEmpty()) { showError("Select at least one item to borrow."); return; }
            if (dpReturn.getValue() == null || dpReturn.getValue().isBefore(LocalDate.now())) {
                showError("Expected return date must be today or later."); return;
            }

            String  classId   = rbClass.isSelected() ? txtClassId.getText().trim() : null;
            Integer requestId = null;
            if (rbRequest.isSelected() && !txtRequestId.getText().trim().isEmpty()) {
                try { requestId = Integer.parseInt(txtRequestId.getText().trim()); }
                catch (NumberFormatException nfe) { showError("Request ID must be a whole number."); return; }
            }

            List<String> barcodes = selectedItems.stream()
                    .map(eq -> eq.barcode)
                    .collect(Collectors.toList());

            // FIX 3: returns int transaction ID now
            int newId = borrowDAO.createBorrowTransaction(
                    borrowerId, loggedInUserId, classId, requestId,
                    dpReturn.getValue().toString(), barcodes);

            if (newId > 0) {
                new Alert(Alert.AlertType.INFORMATION,
                        "Borrow transaction created!\nTransaction ID: #" + newId +
                                "\n\nGive this ID to the borrower for the return process.")
                        .showAndWait();
                viewEquipmentLogic();
            } else {
                showError("Transaction failed. One or more items may no longer be AVAILABLE, " +
                        "or the borrower / class / request ID does not exist.");
            }
        });

        root.getChildren().addAll(topForm, new Separator(), lblPickerTitle, pickerPane, btnSubmit);
        mainLayout.setCenter(root);
    }

    // ================================================================
    // FIX 4 — Return Form: proper status check + open transaction dropdown
    // ================================================================
    private void viewReturnForm() {
        VBox root = createContentRoot("Process Equipment Return");

        // ---- FIX 4a: dropdown of open (BORROWED + OVERDUE) transactions ----
        Label lblDropHint = new Label(
                "Select an open transaction from the list below, or type its ID directly:");
        lblDropHint.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");

        // Combine BORROWED + OVERDUE unreturned records
        List<DataClass.UnreturnedRecord> openList = new ArrayList<>();
        openList.addAll(borrowDAO.getUnreturnedByStatus("BORROWED"));
        openList.addAll(borrowDAO.getUnreturnedByStatus("OVERDUE"));

        ComboBox<String> cbOpenTxns = new ComboBox<>();
        cbOpenTxns.setPromptText("— pick an open transaction —");
        cbOpenTxns.setPrefWidth(480);
        for (DataClass.UnreturnedRecord ur : openList) {
            cbOpenTxns.getItems().add(
                    "#" + ur.transactionId + " | " + ur.borrowerName +
                            " | Due: " + ur.expectedReturn + " | " + ur.status +
                            " | Items: " + ur.items);
        }

        // Step 1: look up by ID
        GridPane step1 = new GridPane();
        step1.setHgap(12); step1.setVgap(10);

        TextField txtTxnId = new TextField();
        txtTxnId.setPromptText("Transaction ID");
        txtTxnId.setPrefWidth(120);
        Button btnLoad = new Button("Load");

        step1.add(new Label("Transaction ID:"), 0, 0);
        step1.add(txtTxnId, 1, 0);
        step1.add(btnLoad, 2, 0);

        // Auto-fill txtTxnId when dropdown selection changes
        cbOpenTxns.setOnAction(e -> {
            String selected = cbOpenTxns.getValue();
            if (selected != null && selected.startsWith("#")) {
                String idStr = selected.substring(1, selected.indexOf(' '));
                txtTxnId.setText(idStr);
            }
        });

        Label lblTxnInfo = new Label("Select a transaction or enter its ID and click Load.");
        lblTxnInfo.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");

        VBox itemsBox = new VBox(8);

        ComboBox<String> cbOverallStatus = new ComboBox<>(FXCollections.observableArrayList(
                "RETURNED", "RETURNED_WITH_ISSUE"));
        cbOverallStatus.setValue("RETURNED");
        cbOverallStatus.setDisable(true);

        Button btnReturn = new Button("CONFIRM RETURN");
        btnReturn.setStyle("-fx-background-color:#2980b9;-fx-text-fill:white;-fx-font-weight:bold;");
        btnReturn.setDisable(true);

        final int[]    loadedTxnId    = {-1};
        final List<DataClass.BorrowItem>   loadedItems    = new ArrayList<>();
        final List<ComboBox<String>>       conditionBoxes = new ArrayList<>();
        final List<TextField>              damageFields   = new ArrayList<>();

        btnLoad.setOnAction(e -> {
            String idStr = txtTxnId.getText().trim();
            if (idStr.isEmpty()) { showError("Transaction ID required."); return; }
            int txnId;
            try { txnId = Integer.parseInt(idStr); }
            catch (NumberFormatException ex) { showError("Transaction ID must be an integer."); return; }

            // FIX 4b: proper status check using getTransactionById()
            DataClass.BorrowRecord txn = borrowDAO.getTransactionById(txnId);
            if (txn == null) {
                lblTxnInfo.setText("Transaction #" + txnId + " not found.");
                return;
            }
            String txnStatus = txn.getStatus();
            if ("RETURNED".equalsIgnoreCase(txnStatus) || "RETURNED_WITH_ISSUE".equalsIgnoreCase(txnStatus)) {
                lblTxnInfo.setText("Transaction #" + txnId + " has already been returned (status: " + txnStatus + ").");
                btnReturn.setDisable(true);
                cbOverallStatus.setDisable(true);
                itemsBox.getChildren().clear();
                return;
            }

            List<DataClass.BorrowItem> items = borrowDAO.getItemsByTransactionId(txnId);
            if (items.isEmpty()) {
                lblTxnInfo.setText("No items found for transaction #" + txnId + ".");
                return;
            }

            loadedTxnId[0] = txnId;
            loadedItems.clear();   loadedItems.addAll(items);
            conditionBoxes.clear(); damageFields.clear();
            itemsBox.getChildren().clear();

            lblTxnInfo.setText("Transaction #" + txnId + " — Borrower: " +
                    txn.getBorrowerName() + " | " + items.size() + " item(s):");

            for (DataClass.BorrowItem item : items) {
                Label lbl = new Label(item.barcode + " — " + item.itemName +
                        " (checked out condition: " + item.conditionOut + ")");
                lbl.setStyle("-fx-font-weight:bold;");

                ComboBox<String> cbCond = new ComboBox<>(
                        FXCollections.observableArrayList("Good", "Damaged"));
                cbCond.setValue("Good");

                TextField tfNotes = new TextField();
                tfNotes.setPromptText("Damage notes (if damaged)");
                tfNotes.setDisable(true);
                tfNotes.setPrefWidth(240);

                cbCond.setOnAction(ev -> tfNotes.setDisable(!"Damaged".equals(cbCond.getValue())));
                conditionBoxes.add(cbCond);
                damageFields.add(tfNotes);

                HBox itemRow = new HBox(10, lbl, new Label("Condition:"), cbCond, tfNotes);
                itemRow.setAlignment(Pos.CENTER_LEFT);
                itemsBox.getChildren().add(itemRow);
            }

            cbOverallStatus.setDisable(false);
            btnReturn.setDisable(false);
        });

        btnReturn.setOnAction(e -> {
            if (loadedTxnId[0] == -1) { showError("Load a transaction first."); return; }

            List<DataClass.BorrowItemReturnInfo> returnItems = new ArrayList<>();
            for (int i = 0; i < loadedItems.size(); i++) {
                String cond  = conditionBoxes.get(i).getValue();
                String notes = damageFields.get(i).getText().trim();
                if (notes.length() > 255) { showError("Damage notes exceed 255 characters."); return; }
                returnItems.add(new DataClass.BorrowItemReturnInfo(
                        loadedItems.get(i).barcode, cond,
                        notes.isEmpty() ? null : notes));
            }

            boolean hasDamage = returnItems.stream()
                    .anyMatch(ri -> "Damaged".equalsIgnoreCase(ri.conditionIn));
            String finalStatus = hasDamage ? "RETURNED_WITH_ISSUE" : cbOverallStatus.getValue();

            boolean ok = borrowDAO.returnBorrowTransaction(
                    loadedTxnId[0], loggedInUserId, finalStatus, returnItems);

            if (ok) {
                new Alert(Alert.AlertType.INFORMATION,
                        "Return processed successfully.\nStatus: " + finalStatus).showAndWait();
                viewEquipmentLogic();
            } else {
                showError("Return failed. Please try again.");
            }
        });

        HBox statusRow = new HBox(12, new Label("Overall Status:"), cbOverallStatus, btnReturn);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        statusRow.setPadding(new Insets(12, 0, 0, 0));

        root.getChildren().addAll(
                lblDropHint, cbOpenTxns, new Separator(),
                step1, lblTxnInfo, new Separator(),
                itemsBox, statusRow);
        mainLayout.setCenter(root);
    }

    // ================================================================
    // Borrower — Request New Activity
    // ================================================================
    private void viewRequestForm() {
        VBox root = createContentRoot("Submit New Activity Request");
        GridPane form = new GridPane();
        form.setHgap(20); form.setVgap(15);

        TextField txtActivityName = new TextField();
        txtActivityName.setPromptText("Activity name (max 100 chars)");

        ComboBox<String> cbType = new ComboBox<>(FXCollections.observableArrayList(
                "RECRUITMENT", "CERTIFICATION", "MEETING", "TRAINING", "OTHER"));
        cbType.setValue("TRAINING"); cbType.setPrefWidth(200);

        DatePicker datePicker = new DatePicker();
        datePicker.setDayCellFactory(picker -> new DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now()));
            }
        });

        TextField txtLocation = new TextField();
        txtLocation.setPromptText("Room/Facility (max 100 chars)");

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
                    txtActivityName.getText().trim(), cbType.getValue(),
                    datePicker.getValue().toString(),
                    txtLocation.getText().trim(), loggedInUserId,
                    txtNotes.getText().trim());

            if (result != -1) {
                new Alert(Alert.AlertType.INFORMATION,
                        "Request submitted successfully!\nRequest ID: " + result).showAndWait();
                viewBorrowerHistoryLogic();
            } else {
                showError("Submission failed. Please check your inputs.");
            }
        });

        root.getChildren().addAll(form, btnSubmit);
        mainLayout.setCenter(root);
    }

    // ================================================================
    // Popup helpers
    // ================================================================

    /** FIX 5/6 — shows items for a given transaction in a popup window. */
    private void showItemsPopup(int transactionId, String title) {
        Stage popup = new Stage();
        popup.setTitle("Items — " + title);
        VBox box = new VBox(10);
        box.setPadding(new Insets(16));

        TableView<DataClass.BorrowItem> tbl = new TableView<>();
        setupColumns(tbl, "barcode", "itemName", "conditionOut", "conditionIn", "damageNotes");
        tbl.setItems(FXCollections.observableArrayList(
                borrowDAO.getItemsByTransactionId(transactionId)));
        tbl.setPrefHeight(220);
        tbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        box.getChildren().addAll(
                new Label("Transaction #" + transactionId + " — borrowed items:"), tbl);
        popup.setScene(new Scene(box, 700, 280));
        popup.show();
    }

    /** FIX 6 — shows borrow transactions linked to an activity request. */
    private void showActivityBorrowsPopup(int requestId, String activityName) {
        Stage popup = new Stage();
        popup.setTitle("Borrows for: " + activityName);
        VBox box = new VBox(10);
        box.setPadding(new Insets(16));

        List<DataClass.BorrowRecord> records = borrowDAO.getRecordsByRequestId(requestId);

        if (records.isEmpty()) {
            box.getChildren().add(new Label("No borrow transactions linked to this activity yet."));
        } else {
            TableView<DataClass.BorrowRecord> tbl = new TableView<>();
            setupColumns(tbl, "transactionId", "borrowerName", "borrowerId",
                    "borrowDate", "expectedReturn", "returnDate", "status");
            tbl.setItems(FXCollections.observableArrayList(records));
            tbl.setPrefHeight(240);
            tbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            box.getChildren().addAll(
                    new Label("Activity: " + activityName + " (Request #" + requestId + ")"), tbl);
        }
        popup.setScene(new Scene(box, 900, 320));
        popup.show();
    }

    // ================================================================
    // Utility helpers
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