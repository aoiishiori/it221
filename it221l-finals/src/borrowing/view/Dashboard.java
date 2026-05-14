package borrowing.view;

import borrowing.DAO.*;
import borrowing.Interface.*;
import borrowing.model.DataClass;
import borrowing.util.SessionContext;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
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
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Dashboard v3 — changes:
 *
 *  [FIX-BRQ]  "Borrow Requests" split into:
 *               viewPendingBorrowRequests()  — PENDING only; row vanishes after approve/reject
 *               viewApprovedBorrowRequests() — APPROVED / REJECTED archive with Copy TXN ID
 *
 *  [FIX-SEL]  Borrower request form Selected table now shows barcode, itemName,
 *             category, BRAND, MODEL (full detail).
 *
 *  [FIX-WARN] Borrower landing page shows colour-coded banners for
 *             overdue items, active borrows, and pending requests.
 *
 *  [QOL]      Row colour-coding in all transaction tables.
 *             Custodian New Borrow selected table also shows brand+model.
 *             handleStatusUpdate now refreshes summary label.
 *             Legend badges on calendar.
 */
public class Dashboard extends Application {

    private final EquipmentInter       equipmentDAO     = new Equipment();
    private final BorrowInter          borrowDAO        = new Borrow();
    private final PersonInter          personDAO        = new Person();
    private final LabClassInter        labClassDAO      = new LabClass();
    private final ActivityRequestInter activityDAO      = new ActivityRequest();
    private final BorrowRequestDAO     borrowRequestDAO = new BorrowRequestDAO();
    private final AuditLog             auditLogDAO      = new AuditLog();

    private String authenticatedRole;
    private String authenticatedUsername;
    private String authenticatedPersonName;
    private String loggedInUserId;
    private BorderPane mainLayout;

    public void setAuthenticatedRole(String r)     { this.authenticatedRole     = r; }
    public void setLoggedInUserId(String id)       { this.loggedInUserId        = id; }
    public void setAuthenticatedUsername(String u) { this.authenticatedUsername = u; }

    private void resolvePersonName() {
        if (loggedInUserId != null) {
            String n = personDAO.getPersonName(loggedInUserId);
            authenticatedPersonName = (n != null) ? n : "Unknown";
        } else {
            authenticatedPersonName = authenticatedUsername;
        }
    }

    @Override
    public void start(Stage stage) {
        resolvePersonName();
        stage.setTitle("CIS Facility & Equipment Borrowing System");
        mainLayout = new BorderPane();
        if (authenticatedRole != null) buildDashboard(authenticatedRole);
        else {
            javafx.application.Platform.runLater(() -> { new LoginView().start(new Stage()); stage.close(); });
            return;
        }
        stage.setScene(new Scene(mainLayout, 1380, 900));
        stage.show();
    }

    // ================================================================
    // SIDEBAR
    // ================================================================
    private void buildDashboard(String role) {
        VBox sb = new VBox(6);
        sb.setPadding(new Insets(18));
        sb.setStyle("-fx-background-color:#1a252f;");
        sb.setPrefWidth(228);

        Label lblName = new Label("✓ " + authenticatedPersonName);
        lblName.setStyle("-fx-text-fill:#2ecc71;-fx-font-weight:bold;-fx-font-size:12px;-fx-wrap-text:true;");
        Label lblRole = new Label("Role: " + role.toUpperCase());
        lblRole.setStyle("-fx-text-fill:#bdc3c7;-fx-font-size:11px;");
        Label lblUser = new Label("@" + authenticatedUsername);
        lblUser.setStyle("-fx-text-fill:#566573;-fx-font-size:10px;");
        sb.getChildren().addAll(lblName, lblRole, lblUser, new Separator());

        boolean isCust  = "CUSTODIAN".equalsIgnoreCase(role);
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        boolean isBorr  = "BORROWER".equalsIgnoreCase(role);

        if (isCust || isAdmin) {
            navSec(sb, "EQUIPMENT");
            nav(sb, "📦  Inventory",          this::viewEquipmentLogic);
            nav(sb, "➕  Add Equipment",       this::viewAddEquipmentForm);
            navSec(sb, "TRANSACTIONS");
            nav(sb, "🆕  New Borrow",          this::viewNewBorrowForm);
            nav(sb, "↩️  Process Return",      this::viewReturnForm);
            nav(sb, "📋  All Records",          this::viewBorrowRecords);
            nav(sb, "⚠️  Overdue Report",      this::viewOverdueReport);
            navSec(sb, "BORROW REQUESTS");
            nav(sb, "📥  Pending Requests",    this::viewPendingBorrowRequests);
            nav(sb, "✅  Approved Requests",   this::viewApprovedBorrowRequests);
            navSec(sb, "ACTIVITY");
            nav(sb, "🗓️  Activity Approvals",  this::viewActivityRequests);
            navSec(sb, "REFERENCE");
            nav(sb, "📅  Return Calendar",     this::viewCalendar);
            nav(sb, "🏫  Class Schedules",     this::viewLabClasses);
            nav(sb, "🎓  Student Directory",   this::viewStudents);
        }
        if (isAdmin) {
            navSec(sb, "ADMIN");
            nav(sb, "👤  User Accounts",       this::viewUserAccounts);
            nav(sb, "📜  Audit Log",           this::viewAuditLog);
        }
        if (isBorr) {
            navSec(sb, "MY ACCOUNT");
            nav(sb, "📋  My History",          this::viewBorrowerHistoryLogic);
            nav(sb, "🆕  Request Borrow",      this::viewBorrowerRequestForm);
            nav(sb, "📄  My Requests",         this::viewMyBorrowRequests);
            nav(sb, "📝  Request Activity",    this::viewRequestForm);
            nav(sb, "📅  Return Calendar",     this::viewCalendar);
        }

        Region spacer = new Region(); VBox.setVgrow(spacer, Priority.ALWAYS);
        Button btnLogout = new Button("⏻  LOGOUT");
        btnLogout.setMaxWidth(Double.MAX_VALUE);
        btnLogout.setStyle("-fx-background-color:#c0392b;-fx-text-fill:white;-fx-font-weight:bold;");
        btnLogout.setOnAction(e -> {
            AuditLog.log(loggedInUserId, authenticatedUsername, "LOGOUT", "User logged out");
            SessionContext.getInstance().logout();
            new LoginView().start(new Stage());
            ((Stage) mainLayout.getScene().getWindow()).close();
        });
        sb.getChildren().addAll(spacer, btnLogout);
        mainLayout.setLeft(sb);

        if (isBorr) viewBorrowerHistoryLogic(); else viewEquipmentLogic();
    }

    private void navSec(VBox sb, String text) {
        Label l = new Label("── " + text + " ──");
        l.setStyle("-fx-text-fill:#566573;-fx-font-size:9px;-fx-font-weight:bold;-fx-padding:8 0 2 0;");
        sb.getChildren().add(l);
    }
    private void nav(VBox sb, String text, Runnable action) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPadding(new Insets(7, 10, 7, 10));
        String n = "-fx-background-color:transparent;-fx-text-fill:#ecf0f1;-fx-alignment:center-left;-fx-font-size:12px;";
        String h = "-fx-background-color:#2c3e50;-fx-text-fill:white;-fx-alignment:center-left;-fx-font-size:12px;";
        btn.setStyle(n);
        btn.setOnMouseEntered(e -> btn.setStyle(h));
        btn.setOnMouseExited(e  -> btn.setStyle(n));
        btn.setOnAction(e -> action.run());
        sb.getChildren().add(btn);
    }

    // ================================================================
    // [FIX-WARN] BORROWER LANDING
    // ================================================================
    private void viewBorrowerHistoryLogic() {
        VBox root = createContentRoot("📋 My Borrowing History");

        if (loggedInUserId != null) {
            List<DataClass.BorrowRecord> myRecords = borrowDAO.getRecordsByBorrowerId(loggedInUserId);
            long overdueCnt  = myRecords.stream().filter(r -> "OVERDUE".equals(r.status)).count();
            long borrowedCnt = myRecords.stream().filter(r -> "BORROWED".equals(r.status)).count();
            long pendingReqs = borrowRequestDAO.getRequestsByRequester(loggedInUserId)
                    .stream().filter(r -> "PENDING".equals(r.status)).count();

            if (overdueCnt > 0) {
                root.getChildren().add(banner(
                        "🚨  OVERDUE — ACTION REQUIRED",
                        "You have " + overdueCnt + " overdue item(s) that are past their return date. " +
                                "Please return them to the custodian immediately. " +
                                "Continued non-return may result in sanctions.",
                        "#c0392b", "#fadbd8", "#922b21"));
            }
            if (borrowedCnt > 0) {
                root.getChildren().add(banner(
                        "ℹ️  ACTIVE BORROW REMINDER",
                        "You currently have " + borrowedCnt + " item(s) checked out. " +
                                "Please return them by the expected return date shown in the table below.",
                        "#d68910", "#fef9e7", "#9a7d0a"));
            }
            if (pendingReqs > 0) {
                root.getChildren().add(banner(
                        "⏳  PENDING BORROW REQUEST",
                        "You have " + pendingReqs + " borrow request(s) awaiting custodian approval. " +
                                "Check \"My Requests\" for updates.",
                        "#1a5276", "#d6eaf8", "#154360"));
            }
        }

        TableView<DataClass.BorrowRecord> table = new TableView<>();
        setupCols(table, "transactionId","classId","activityName",
                "borrowDate","expectedReturn","returnDate","status");
        colorBorrowRows(table);

        TableColumn<DataClass.BorrowRecord, Void> itemsCol = new TableColumn<>("Items");
        itemsCol.setCellFactory(p -> new TableCell<DataClass.BorrowRecord, Void>() {
            private final Button btn = new Button("View Items");
            { btn.setStyle("-fx-font-size:10px;");
                btn.setOnAction(e -> {
                    DataClass.BorrowRecord r = getTableView().getItems().get(getIndex());
                    showItemsPopup(r.getTransactionId(), "Transaction #" + r.getTransactionId());
                }); }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty); setGraphic(empty ? null : btn);
            }
        });
        table.getColumns().add(itemsCol);

        Runnable reload = () -> {
            if (loggedInUserId != null)
                table.setItems(FXCollections.observableArrayList(
                        borrowDAO.getRecordsByBorrowerId(loggedInUserId)));
        };
        reload.run();

        Button btnRefresh = new Button("↺ Refresh");
        btnRefresh.setOnAction(e -> reload.run());
        root.getChildren().addAll(new HBox(10, btnRefresh), table);
        mainLayout.setCenter(root);
    }

    private VBox banner(String title, String body,
                        String borderColor, String bgColor, String titleColor) {
        VBox box = new VBox(4);
        box.setPadding(new Insets(10, 14, 10, 14));
        box.setStyle("-fx-background-color:" + bgColor + ";" +
                "-fx-border-color:" + borderColor + ";" +
                "-fx-border-width:0 0 0 5;-fx-border-radius:4;-fx-background-radius:4;");
        Label lt = new Label(title);
        lt.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:" + titleColor + ";");
        Label lb = new Label(body);
        lb.setStyle("-fx-font-size:12px;-fx-text-fill:#333;-fx-wrap-text:true;");
        lb.setMaxWidth(920);
        box.getChildren().addAll(lt, lb);
        return box;
    }

    // ================================================================
    // EQUIPMENT INVENTORY
    // ================================================================
    private void viewEquipmentLogic() {
        VBox root = createContentRoot("📦 Equipment Inventory");

        TextField txtSearch = new TextField(); txtSearch.setPromptText("Search barcode or item name..."); txtSearch.setPrefWidth(220);
        ToggleGroup tg = new ToggleGroup();
        RadioButton rbCont  = new RadioButton("Contains"); rbCont.setToggleGroup(tg);  rbCont.setSelected(true);
        RadioButton rbExact = new RadioButton("Exact barcode"); rbExact.setToggleGroup(tg);
        ComboBox<String> cbStat = new ComboBox<>(FXCollections.observableArrayList("ALL","AVAILABLE","BORROWED","DAMAGED","DECOMMISSIONED"));
        cbStat.setValue("ALL"); cbStat.setPrefWidth(140);
        ComboBox<String> cbCat = new ComboBox<>(FXCollections.observableArrayList("ALL","EQUIPMENT","PERIPHERAL","ACCESSORY"));
        cbCat.setValue("ALL"); cbCat.setPrefWidth(130);
        Button btnSrch = new Button("🔍 Search");
        Button btnRef  = new Button("↺ Refresh");

        HBox filterBar = new HBox(10, new Label("Search:"), txtSearch, new HBox(8, rbCont, rbExact),
                new Label("Status:"), cbStat, new Label("Category:"), cbCat, btnSrch, btnRef);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(0, 0, 12, 0));

        TableView<DataClass.Equipment> table = new TableView<>();
        setupCols(table, "barcode","itemName","category","brand","model","status","remarks");
        table.setRowFactory(tv -> new TableRow<DataClass.Equipment>() {
            @Override protected void updateItem(DataClass.Equipment item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) { setStyle(""); return; }
                switch (item.status) {
                    case "BORROWED":       setStyle("-fx-background-color:#fef9e7;"); break;
                    case "DAMAGED":        setStyle("-fx-background-color:#fadbd8;"); break;
                    case "DECOMMISSIONED": setStyle("-fx-background-color:#f2f3f4;"); break;
                    default:               setStyle(""); break;
                }
            }
        });

        Runnable reload = () -> table.setItems(FXCollections.observableArrayList(equipmentDAO.getAllEquipment()));
        reload.run();

        btnSrch.setOnAction(e -> {
            String kw  = txtSearch.getText().trim();
            String st  = cbStat.getValue();
            String cat = cbCat.getValue();
            List<DataClass.Equipment> res = kw.isEmpty() ? equipmentDAO.getAllEquipment()
                    : (rbExact.isSelected() ? equipmentDAO.getEquipmentByExactBarcode(kw)
                    : equipmentDAO.searchEquipment("%" + kw + "%"));
            if (!"ALL".equals(st))  res = res.stream().filter(q -> st.equalsIgnoreCase(q.status)).collect(Collectors.toList());
            if (!"ALL".equals(cat)) res = res.stream().filter(q -> cat.equalsIgnoreCase(q.category)).collect(Collectors.toList());
            table.setItems(FXCollections.observableArrayList(res));
        });
        btnRef.setOnAction(e -> { txtSearch.clear(); cbStat.setValue("ALL"); cbCat.setValue("ALL"); rbCont.setSelected(true); reload.run(); });

        root.getChildren().addAll(filterBar, table);
        mainLayout.setCenter(root);
    }

    // ================================================================
    // ADD EQUIPMENT
    // ================================================================
    private void viewAddEquipmentForm() {
        VBox root = createContentRoot("➕ Add New Equipment");
        GridPane form = new GridPane(); form.setHgap(16); form.setVgap(14);
        TextField txtName    = new TextField(); txtName.setPromptText("e.g. Laptop (max 100 chars)");
        ComboBox<String> cbCat = new ComboBox<>(FXCollections.observableArrayList("EQUIPMENT","PERIPHERAL","ACCESSORY"));
        cbCat.setValue("EQUIPMENT"); cbCat.setPrefWidth(180);
        TextField txtBrand   = new TextField(); txtBrand.setPromptText("e.g. Dell");
        TextField txtModel   = new TextField(); txtModel.setPromptText("e.g. Inspiron 15");
        DatePicker dpAcq     = new DatePicker(LocalDate.now());
        TextField txtRemarks = new TextField(); txtRemarks.setPromptText("Optional notes");
        Label lblPrev = new Label("Barcode auto-generated as next EQ-XXXX.");
        lblPrev.setStyle("-fx-font-size:11px;-fx-text-fill:#7f8c8d;");
        int r = 0;
        form.add(new Label("Item Name:"),        0, r); form.add(txtName,    1, r++);
        form.add(new Label("Category:"),         0, r); form.add(cbCat,      1, r++);
        form.add(new Label("Brand:"),            0, r); form.add(txtBrand,   1, r++);
        form.add(new Label("Model:"),            0, r); form.add(txtModel,   1, r++);
        form.add(new Label("Acquisition Date:"), 0, r); form.add(dpAcq,      1, r++);
        form.add(new Label("Remarks:"),          0, r); form.add(txtRemarks, 1, r++);
        form.add(lblPrev,                        1, r++);
        Button btnAdd = styledBtn("✅ Add Equipment", "#27ae60");
        btnAdd.setOnAction(e -> {
            String name = txtName.getText().trim();
            if (name.isEmpty() || name.length() > 100)      { showError("Item name required (≤100 chars)."); return; }
            if (txtBrand.getText().length()   > 50)          { showError("Brand ≤ 50 chars."); return; }
            if (txtModel.getText().length()   > 50)          { showError("Model ≤ 50 chars."); return; }
            if (txtRemarks.getText().length() > 255)         { showError("Remarks ≤ 255 chars."); return; }
            Equipment eqDAO = (Equipment) equipmentDAO;
            String bc = eqDAO.addEquipment(name, cbCat.getValue(), txtBrand.getText().trim(),
                    txtModel.getText().trim(),
                    dpAcq.getValue() != null ? dpAcq.getValue().toString() : null,
                    txtRemarks.getText().trim());
            if (bc != null) {
                AuditLog.log(loggedInUserId, authenticatedUsername, "ADD_EQUIPMENT", "equipment", bc,
                        "Added: " + name + " [" + cbCat.getValue() + "] bc=" + bc);
                new Alert(Alert.AlertType.INFORMATION, "✅ Equipment added!\nBarcode: " + bc + "\nStatus: AVAILABLE").showAndWait();
                viewEquipmentLogic();
            } else { showError("Failed to add equipment."); }
        });
        root.getChildren().addAll(form, btnAdd);
        mainLayout.setCenter(root);
    }

    // ================================================================
    // ALL BORROW RECORDS
    // ================================================================
    private void viewBorrowRecords() {
        VBox root = createContentRoot("📋 Borrow Transaction Records");
        ComboBox<String> cbStat = new ComboBox<>(FXCollections.observableArrayList(
                "ALL","BORROWED","RETURNED","RETURNED_WITH_ISSUE","OVERDUE"));
        cbStat.setValue("ALL");
        Button btnFilter = new Button("Filter"); Button btnRef = new Button("↺ Refresh");
        HBox bar = new HBox(10, new Label("Status:"), cbStat, btnFilter, btnRef);
        bar.setPadding(new Insets(0, 0, 12, 0)); bar.setAlignment(Pos.CENTER_LEFT);
        TableView<DataClass.BorrowRecord> table = new TableView<>();
        setupCols(table, "transactionId","borrowerName","borrowerId","classId","activityName",
                "borrowDate","expectedReturn","returnDate","status");
        colorBorrowRows(table);
        table.setItems(FXCollections.observableArrayList(borrowDAO.getAllRecords()));
        btnFilter.setOnAction(e -> {
            String s = cbStat.getValue();
            table.setItems(FXCollections.observableArrayList(
                    "ALL".equals(s) ? borrowDAO.getAllRecords() : borrowDAO.getRecordsByStatus(s)));
        });
        btnRef.setOnAction(e -> { cbStat.setValue("ALL"); table.setItems(FXCollections.observableArrayList(borrowDAO.getAllRecords())); });
        root.getChildren().addAll(bar, table);
        mainLayout.setCenter(root);
    }

    // ================================================================
    // [FIX-BRQ] PENDING BORROW REQUESTS — row disappears after action
    // ================================================================
    private void viewPendingBorrowRequests() {
        VBox root = createContentRoot("📥 Pending Borrow Requests");
        Label hint = new Label("After approving or rejecting, the row moves to Approved Requests.");
        hint.setStyle("-fx-font-size:12px;-fx-text-fill:#555;");

        TableView<DataClass.BorrowRequestRecord> table = new TableView<>();
        addCol(table, "Req #",       "borrowReqId");
        addCol(table, "Requester",   "requesterName");
        addCol(table, "Type",        "personType");
        addCol(table, "Items",       "itemBarcodes");
        addCol(table, "Purpose",     "purpose");
        addCol(table, "Needed Date", "neededDate");
        addCol(table, "Return By",   "expectedReturn");
        addCol(table, "Submitted",   "createdAt");

        TableColumn<DataClass.BorrowRequestRecord, Void> actionCol = new TableColumn<>("Action");
        actionCol.setMinWidth(170);
        actionCol.setCellFactory(p -> new TableCell<DataClass.BorrowRequestRecord, Void>() {
            private final Button btnA = new Button("✅ APPROVE");
            private final Button btnR = new Button("❌ REJECT");
            private final HBox box    = new HBox(6, btnA, btnR);
            {
                btnA.setStyle("-fx-background-color:#27ae60;-fx-text-fill:white;-fx-font-size:10px;-fx-font-weight:bold;");
                btnR.setStyle("-fx-background-color:#e74c3c;-fx-text-fill:white;-fx-font-size:10px;-fx-font-weight:bold;");
                box.setAlignment(Pos.CENTER);
                btnA.setOnAction(e -> handleBorrowReqAction(getTableView().getItems().get(getIndex()), true,  table));
                btnR.setOnAction(e -> handleBorrowReqAction(getTableView().getItems().get(getIndex()), false, table));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty); setGraphic(empty ? null : box);
            }
        });
        table.getColumns().add(actionCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Runnable reload = () -> table.setItems(FXCollections.observableArrayList(borrowRequestDAO.getPendingRequests()));
        reload.run();
        Button btnRef = new Button("↺ Refresh"); btnRef.setOnAction(e -> reload.run());
        root.getChildren().addAll(hint, new HBox(10, btnRef), table);
        mainLayout.setCenter(root);
    }

    private void handleBorrowReqAction(DataClass.BorrowRequestRecord rec,
                                       boolean approve,
                                       TableView<DataClass.BorrowRequestRecord> table) {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle(approve ? "Approve Request" : "Reject Request");
        dlg.setHeaderText((approve ? "Approving" : "Rejecting") +
                " request #" + rec.borrowReqId + " — " + rec.requesterName);
        dlg.setContentText("Notes (optional):");
        String notes = dlg.showAndWait().orElse("");
        boolean success = false;

        if (approve) {
            int txnId = borrowRequestDAO.approveRequest(
                    rec.borrowReqId, authenticatedUsername, loggedInUserId, notes, (Borrow) borrowDAO);
            if (txnId > 0) {
                AuditLog.log(loggedInUserId, authenticatedUsername, "APPROVE_BORROW_REQUEST",
                        "borrow_request", String.valueOf(rec.borrowReqId),
                        "Approved → TXN#" + txnId + " for " + rec.requesterName);
                new Alert(Alert.AlertType.INFORMATION,
                        "✅ Request approved!\n\nTransaction #" + txnId + " created.\n" +
                                "Give Transaction ID #" + txnId + " to the borrower.").showAndWait();
                success = true;
            } else {
                showError("Approval failed. Items may no longer be AVAILABLE.");
            }
        } else {
            if (borrowRequestDAO.rejectRequest(rec.borrowReqId, loggedInUserId, notes)) {
                AuditLog.log(loggedInUserId, authenticatedUsername, "REJECT_BORROW_REQUEST",
                        "borrow_request", String.valueOf(rec.borrowReqId),
                        "Rejected for " + rec.requesterName);
                new Alert(Alert.AlertType.INFORMATION, "Request rejected.").showAndWait();
                success = true;
            } else { showError("Rejection failed. Request may no longer be PENDING."); }
        }

        // [FIX-BRQ] Remove row immediately on success — it lives in Approved tab now
        if (success) table.getItems().remove(rec);
    }

    // ================================================================
    // [FIX-BRQ] APPROVED / REJECTED REQUESTS
    // ================================================================
    private void viewApprovedBorrowRequests() {
        VBox root = createContentRoot("✅ Approved & Rejected Borrow Requests");
        Label hint = new Label("Processed requests. Approved rows show the Transaction ID for follow-up.");
        hint.setStyle("-fx-font-size:12px;-fx-text-fill:#555;");

        TableView<DataClass.BorrowRequestRecord> table = new TableView<>();
        addCol(table, "Req #",       "borrowReqId");
        addCol(table, "Requester",   "requesterName");
        addCol(table, "Items",       "itemBarcodes");
        addCol(table, "Purpose",     "purpose");
        addCol(table, "Needed Date", "neededDate");
        addCol(table, "Return By",   "expectedReturn");
        addCol(table, "Status",      "status");
        addCol(table, "Notes",       "reviewNotes");
        addCol(table, "TXN #",       "transactionId");
        addCol(table, "Submitted",   "createdAt");

        table.setRowFactory(tv -> new TableRow<DataClass.BorrowRequestRecord>() {
            @Override protected void updateItem(DataClass.BorrowRequestRecord item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) { setStyle(""); return; }
                if ("APPROVED".equals(item.status)) setStyle("-fx-background-color:#eafaf1;");
                else if ("REJECTED".equals(item.status)) setStyle("-fx-background-color:#fadbd8;");
                else setStyle("");
            }
        });

        // Copy TXN ID button for approved rows
        TableColumn<DataClass.BorrowRequestRecord, Void> copyCol = new TableColumn<>("Copy TXN");
        copyCol.setMinWidth(95);
        copyCol.setCellFactory(p -> new TableCell<DataClass.BorrowRequestRecord, Void>() {
            private final Button btn = new Button("📋 Copy ID");
            { btn.setStyle("-fx-font-size:10px;");
                btn.setOnAction(e -> {
                    DataClass.BorrowRequestRecord rec = getTableView().getItems().get(getIndex());
                    if (rec.transactionId != null) {
                        javafx.scene.input.Clipboard cb = javafx.scene.input.Clipboard.getSystemClipboard();
                        javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
                        cc.putString(String.valueOf(rec.transactionId)); cb.setContent(cc);
                        new Alert(Alert.AlertType.INFORMATION,
                                "Transaction ID #" + rec.transactionId + " copied to clipboard.").showAndWait();
                    }
                }); }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                DataClass.BorrowRequestRecord rec = getTableView().getItems().get(getIndex());
                setGraphic("APPROVED".equals(rec.status) && rec.transactionId != null ? btn : null);
            }
        });
        table.getColumns().add(copyCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Runnable reload = () -> {
            List<DataClass.BorrowRequestRecord> processed = borrowRequestDAO.getAllRequests()
                    .stream().filter(r -> !"PENDING".equals(r.status)).collect(Collectors.toList());
            table.setItems(FXCollections.observableArrayList(processed));
        };
        reload.run();
        Button btnRef = new Button("↺ Refresh"); btnRef.setOnAction(e -> reload.run());
        root.getChildren().addAll(hint, new HBox(10, btnRef), table);
        mainLayout.setCenter(root);
    }

    // ================================================================
    // [FIX-SEL] BORROWER: Submit Borrow Request — Selected shows brand+model
    // ================================================================
    private void viewBorrowerRequestForm() {
        VBox root = createContentRoot("🆕 Request Equipment to Borrow");
        Label info = new Label(
                "Double-click an item to select it. Double-click a selected item to remove it. " +
                        "Your request will be reviewed by a custodian.");
        info.setStyle("-fx-font-size:12px;-fx-text-fill:#555;-fx-wrap-text:true;");

        // Available
        Label lblA = new Label("Available Equipment  (double-click → add)");
        lblA.setStyle("-fx-font-weight:bold;");
        TableView<DataClass.Equipment> tblA = new TableView<>();
        setupCols(tblA, "barcode","itemName","category","brand","model");
        tblA.setPrefHeight(200); tblA.setPrefWidth(560);
        Button btnRld = new Button("↺ Reload");
        Runnable rld = () -> tblA.setItems(FXCollections.observableArrayList(equipmentDAO.getAvailableEquipment()));
        rld.run(); btnRld.setOnAction(e -> rld.run());

        // [FIX-SEL] Selected — now shows barcode, itemName, category, brand, model
        Label lblS = new Label("Selected Items  (double-click → remove)");
        lblS.setStyle("-fx-font-weight:bold;");
        ObservableList<DataClass.Equipment> sel = FXCollections.observableArrayList();
        TableView<DataClass.Equipment> tblS = new TableView<>();
        setupCols(tblS, "barcode","itemName","category","brand","model");
        tblS.setPrefHeight(200); tblS.setPrefWidth(560); tblS.setItems(sel);

        tblA.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                DataClass.Equipment it = tblA.getSelectionModel().getSelectedItem();
                if (it != null && !sel.contains(it)) { sel.add(it); tblA.getItems().remove(it); }
            }
        });
        tblS.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                DataClass.Equipment it = tblS.getSelectionModel().getSelectedItem();
                if (it != null) {
                    sel.remove(it); tblA.getItems().add(it);
                    FXCollections.sort(tblA.getItems(), (a, b) -> a.barcode.compareTo(b.barcode));
                }
            }
        });

        HBox picker = new HBox(18, new VBox(6, lblA, btnRld, tblA), new VBox(6, lblS, tblS));

        GridPane form = new GridPane(); form.setHgap(16); form.setVgap(12);
        DatePicker dpNeed   = new DatePicker(LocalDate.now().plusDays(1));
        DatePicker dpReturn = new DatePicker(LocalDate.now().plusDays(1));
        dpNeed.setDayCellFactory(p -> new DateCell() {
            @Override public void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty); setDisable(empty || d.isBefore(LocalDate.now()));
            }
        });
        dpReturn.setDayCellFactory(p -> new DateCell() {
            @Override public void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty); setDisable(empty || d.isBefore(LocalDate.now()));
            }
        });
        TextField txtPurpose = new TextField(); txtPurpose.setPromptText("Purpose / reason (max 255 chars)"); txtPurpose.setPrefWidth(320);
        form.add(new Label("Needed Date:"), 0, 0); form.add(dpNeed,      1, 0);
        form.add(new Label("Return By:"),   0, 1); form.add(dpReturn,    1, 1);
        form.add(new Label("Purpose:"),     0, 2); form.add(txtPurpose,  1, 2);

        Button btnSub = styledBtn("📤 Submit Request", "#2980b9");
        btnSub.setOnAction(e -> {
            if (sel.isEmpty())        { showError("Select at least one item."); return; }
            if (dpNeed.getValue() == null) { showError("Select a needed date."); return; }
            if (dpReturn.getValue() == null || dpReturn.getValue().isBefore(dpNeed.getValue())) {
                showError("Return date must be on or after needed date."); return;
            }
            if (txtPurpose.getText().length() > 255) { showError("Purpose ≤ 255 chars."); return; }
            String barcodes = sel.stream().map(q -> q.barcode).collect(Collectors.joining(","));
            int reqId = borrowRequestDAO.submitRequest(loggedInUserId, barcodes,
                    txtPurpose.getText().trim(), dpNeed.getValue().toString(), dpReturn.getValue().toString());
            if (reqId > 0) {
                AuditLog.log(loggedInUserId, authenticatedUsername, "SUBMIT_BORROW_REQUEST",
                        "borrow_request", String.valueOf(reqId), "Requested: " + barcodes);
                new Alert(Alert.AlertType.INFORMATION,
                        "✅ Request submitted!\nRequest ID: #" + reqId +
                                "\nA custodian will review your request. Check \"My Requests\" for updates.").showAndWait();
                viewMyBorrowRequests();
            } else { showError("Submission failed. Items may no longer be available. Try reloading."); }
        });

        root.getChildren().addAll(info, new Separator(), picker, new Separator(), form, btnSub);
        mainLayout.setCenter(root);
    }

    // ================================================================
    // BORROWER: My Requests
    // ================================================================
    private void viewMyBorrowRequests() {
        VBox root = createContentRoot("📄 My Borrow Requests");
        TableView<DataClass.BorrowRequestRecord> table = new TableView<>();
        addCol(table, "Req #",       "borrowReqId");
        addCol(table, "Items",       "itemBarcodes");
        addCol(table, "Purpose",     "purpose");
        addCol(table, "Needed Date", "neededDate");
        addCol(table, "Return By",   "expectedReturn");
        addCol(table, "Status",      "status");
        addCol(table, "Notes",       "reviewNotes");
        addCol(table, "TXN #",       "transactionId");
        addCol(table, "Submitted",   "createdAt");
        table.setRowFactory(tv -> new TableRow<DataClass.BorrowRequestRecord>() {
            @Override protected void updateItem(DataClass.BorrowRequestRecord item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) { setStyle(""); return; }
                if ("APPROVED".equals(item.status))  setStyle("-fx-background-color:#eafaf1;");
                else if ("REJECTED".equals(item.status)) setStyle("-fx-background-color:#fadbd8;");
                else if ("PENDING".equals(item.status))  setStyle("-fx-background-color:#fef9e7;");
                else setStyle("");
            }
        });
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        Runnable reload = () -> {
            if (loggedInUserId != null)
                table.setItems(FXCollections.observableArrayList(
                        borrowRequestDAO.getRequestsByRequester(loggedInUserId)));
        };
        reload.run();
        Button btnRef = new Button("↺ Refresh"); btnRef.setOnAction(e -> reload.run());
        Label legend = new Label("🟡 Pending  |  🟢 Approved (TXN# = your transaction)  |  🔴 Rejected");
        legend.setStyle("-fx-font-size:11px;-fx-text-fill:#555;-fx-padding:4 0 0 0;");
        root.getChildren().addAll(new HBox(10, btnRef), legend, table);
        mainLayout.setCenter(root);
    }

    // ================================================================
    // ACTIVITY APPROVALS
    // ================================================================
    private void viewActivityRequests() {
        VBox root = createContentRoot("🗓️ Activity Request Approvals");
        Label lblSummary = new Label("Summary — " + activityDAO.getActivitySummary());
        lblSummary.setStyle("-fx-font-size:12px;-fx-text-fill:#555;-fx-padding:0 0 10 0;");

        TableView<DataClass.ActivityRecord> table = new TableView<>();
        setupCols(table, "requestId","activityName","activityType","activityDate","requesterName","status");

        TableColumn<DataClass.ActivityRecord, Void> approveCol = new TableColumn<>("Approve/Reject");
        approveCol.setMinWidth(165);
        approveCol.setCellFactory(p -> new TableCell<DataClass.ActivityRecord, Void>() {
            private final Button btnA = new Button("✅ APPROVE"), btnR = new Button("❌ REJECT");
            private final HBox box = new HBox(6, btnA, btnR);
            {
                btnA.setStyle("-fx-background-color:#27ae60;-fx-text-fill:white;-fx-font-size:10px;-fx-font-weight:bold;");
                btnR.setStyle("-fx-background-color:#e74c3c;-fx-text-fill:white;-fx-font-size:10px;-fx-font-weight:bold;");
                box.setAlignment(Pos.CENTER);
                btnA.setOnAction(e -> handleActivityStatus(getTableView().getItems().get(getIndex()).getRequestId(), "APPROVED", table, lblSummary));
                btnR.setOnAction(e -> handleActivityStatus(getTableView().getItems().get(getIndex()).getRequestId(), "REJECTED", table, lblSummary));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                DataClass.ActivityRecord rec = getTableView().getItems().get(getIndex());
                setGraphic("PENDING".equalsIgnoreCase(rec.getStatus()) ? box : null);
            }
        });
        TableColumn<DataClass.ActivityRecord, Void> borrowsCol = new TableColumn<>("Borrows");
        borrowsCol.setCellFactory(p -> new TableCell<DataClass.ActivityRecord, Void>() {
            private final Button btn = new Button("View Borrows");
            { btn.setStyle("-fx-font-size:10px;");
                btn.setOnAction(e -> {
                    DataClass.ActivityRecord r = getTableView().getItems().get(getIndex());
                    showActivityBorrowsPopup(r.getRequestId(), r.getActivityName());
                }); }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty); setGraphic(empty ? null : btn);
            }
        });
        table.getColumns().addAll(approveCol, borrowsCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(FXCollections.observableArrayList(activityDAO.getAllActivities()));

        Button btnRef = new Button("↺ Refresh");
        btnRef.setOnAction(e -> {
            table.setItems(FXCollections.observableArrayList(activityDAO.getAllActivities()));
            lblSummary.setText("Summary — " + activityDAO.getActivitySummary());
        });
        root.getChildren().addAll(lblSummary, new HBox(10, btnRef), table);
        mainLayout.setCenter(root);
    }

    private void handleActivityStatus(int requestId, String status,
                                      TableView<DataClass.ActivityRecord> table, Label lblSummary) {
        String approver = (loggedInUserId != null) ? loggedInUserId : "STAFF-001";
        if (activityDAO.updateActivityStatus(requestId, status, approver)) {
            AuditLog.log(loggedInUserId, authenticatedUsername, status + "_ACTIVITY",
                    "activity_request", String.valueOf(requestId),
                    "Activity #" + requestId + " → " + status);
            table.setItems(FXCollections.observableArrayList(activityDAO.getAllActivities()));
            lblSummary.setText("Summary — " + activityDAO.getActivitySummary());
            new Alert(Alert.AlertType.INFORMATION, "Request " + status + ".").showAndWait();
        } else { showError("Update failed. The request may no longer be PENDING."); }
    }

    // ================================================================
    // NEW BORROW FORM
    // ================================================================
    private void viewNewBorrowForm() {
        VBox root = createContentRoot("🆕 New Borrow Transaction");
        Person pDAO = (Person) personDAO;
        List<String> personEntries = pDAO.getAllPersonsForPicker();

        ComboBox<String> cbBorrower = new ComboBox<>(FXCollections.observableArrayList(personEntries));
        cbBorrower.setEditable(true); cbBorrower.setPromptText("Type name or ID to filter..."); cbBorrower.setPrefWidth(360);
        TextField txtBId = new TextField(); txtBId.setPromptText("Or type person ID directly"); txtBId.setPrefWidth(220);
        cbBorrower.getEditor().textProperty().addListener((obs, o, newVal) -> {
            if (newVal == null) return;
            String low = newVal.toLowerCase();
            cbBorrower.setItems(FXCollections.observableArrayList(
                    personEntries.stream().filter(s -> s.toLowerCase().contains(low)).collect(Collectors.toList())));
            cbBorrower.show();
        });
        cbBorrower.setOnAction(e -> {
            String v = cbBorrower.getValue();
            if (v != null && v.contains("|")) txtBId.setText(v.substring(0, v.indexOf("|")).trim());
        });

        Label lblInfo = new Label("Select or enter a borrower, then click Look Up.");
        lblInfo.setStyle("-fx-font-size:11px;-fx-text-fill:#555;");
        Button btnLookup = new Button("Look Up");
        btnLookup.setOnAction(e -> {
            String pid = resolveBId(cbBorrower, txtBId);
            if (pid.isEmpty()) { lblInfo.setText("Enter a person ID first."); return; }
            String name = personDAO.getPersonName(pid);
            if (name == null) { lblInfo.setStyle("-fx-text-fill:#e74c3c;-fx-font-size:11px;"); lblInfo.setText("❌ Not found: " + pid); return; }
            int active = borrowDAO.countActiveBorrows(pid);
            if (active > 0) {
                lblInfo.setStyle("-fx-text-fill:#e67e22;-fx-font-size:11px;");
                lblInfo.setText("⚠ " + name + " — " + active + " active/overdue borrow(s).");
            } else {
                lblInfo.setStyle("-fx-text-fill:#27ae60;-fx-font-size:11px;");
                lblInfo.setText("✓ " + name + " — clear to borrow.");
            }
        });

        ToggleGroup tgCtx = new ToggleGroup();
        RadioButton rbCls = new RadioButton("Class-based"); rbCls.setToggleGroup(tgCtx);
        RadioButton rbReq = new RadioButton("Activity Request"); rbReq.setToggleGroup(tgCtx);
        RadioButton rbWlk = new RadioButton("Walk-in"); rbWlk.setToggleGroup(tgCtx); rbWlk.setSelected(true);
        TextField txtCls = new TextField(); txtCls.setPromptText("e.g. IT221L-A"); txtCls.setDisable(true);
        TextField txtRId = new TextField(); txtRId.setPromptText("Request ID (integer)"); txtRId.setDisable(true);
        rbCls.setOnAction(e -> { txtCls.setDisable(false); txtRId.setDisable(true);  txtRId.clear(); });
        rbReq.setOnAction(e -> { txtCls.setDisable(true);  txtCls.clear(); txtRId.setDisable(false); });
        rbWlk.setOnAction(e -> { txtCls.setDisable(true);  txtCls.clear(); txtRId.setDisable(true); txtRId.clear(); });

        DatePicker dpRet = new DatePicker(LocalDate.now());
        dpRet.setDayCellFactory(p -> new DateCell() {
            @Override public void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty); setDisable(empty || d.isBefore(LocalDate.now()));
            }
        });

        GridPane topForm = new GridPane(); topForm.setHgap(16); topForm.setVgap(12);
        int r = 0;
        topForm.add(new Label("Borrower (picker):"),    0, r); topForm.add(cbBorrower, 1, r); topForm.add(btnLookup, 2, r++);
        topForm.add(new Label("Borrower ID (manual):"), 0, r); topForm.add(txtBId,     1, r++);
        topForm.add(lblInfo,                            1, r++, 2, 1);
        topForm.add(new Label("Context:"),              0, r); topForm.add(new HBox(10, rbCls, rbReq, rbWlk), 1, r++, 2, 1);
        topForm.add(new Label("Class ID:"),             0, r); topForm.add(txtCls,     1, r++);
        topForm.add(new Label("Request ID:"),           0, r); topForm.add(txtRId,     1, r++);
        topForm.add(new Label("Expected Return:"),      0, r); topForm.add(dpRet,      1, r++);

        Label lblPick = new Label("Select Equipment  (double-click to add / remove):");
        lblPick.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-padding:10 0 4 0;");

        TableView<DataClass.Equipment> tblAvail = new TableView<>();
        setupCols(tblAvail, "barcode","itemName","category","brand","model");
        tblAvail.setPrefHeight(200);
        Button btnRld = new Button("↺ Reload Available");
        Runnable rldAvail = () -> tblAvail.setItems(FXCollections.observableArrayList(equipmentDAO.getAvailableEquipment()));
        rldAvail.run(); btnRld.setOnAction(e -> rldAvail.run());

        ObservableList<DataClass.Equipment> selItems = FXCollections.observableArrayList();
        TableView<DataClass.Equipment> tblSel = new TableView<>();
        // [FIX-SEL] brand and model included
        setupCols(tblSel, "barcode","itemName","category","brand","model");
        tblSel.setPrefHeight(200); tblSel.setItems(selItems);

        tblAvail.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                DataClass.Equipment it = tblAvail.getSelectionModel().getSelectedItem();
                if (it != null && !selItems.contains(it)) { selItems.add(it); tblAvail.getItems().remove(it); }
            }
        });
        tblSel.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                DataClass.Equipment it = tblSel.getSelectionModel().getSelectedItem();
                if (it != null) {
                    selItems.remove(it); tblAvail.getItems().add(it);
                    FXCollections.sort(tblAvail.getItems(), (a, b) -> a.barcode.compareTo(b.barcode));
                }
            }
        });

        HBox pickerPane = new HBox(16,
                new VBox(6, new Label("Available Equipment"), btnRld, tblAvail),
                new VBox(6, new Label("Selected Items (brand & model shown)"),
                        new Label("Double-click to remove"), tblSel));

        Button btnSub = styledBtn("✅ CONFIRM BORROW", "#27ae60");
        btnSub.setOnAction(e -> {
            String bId = resolveBId(cbBorrower, txtBId);
            if (bId.isEmpty())                           { showError("Borrower ID required."); return; }
            if (personDAO.getPersonName(bId) == null)    { showError("Borrower ID not found."); return; }
            if (selItems.isEmpty())                      { showError("Select at least one item."); return; }
            if (dpRet.getValue() == null || dpRet.getValue().isBefore(LocalDate.now())) {
                showError("Return date must be today or later."); return;
            }
            String classId = rbCls.isSelected() ? txtCls.getText().trim() : null;
            Integer reqId  = null;
            if (rbReq.isSelected() && !txtRId.getText().trim().isEmpty()) {
                try { reqId = Integer.parseInt(txtRId.getText().trim()); }
                catch (NumberFormatException ex) { showError("Request ID must be a whole number."); return; }
            }
            List<String> barcodes = selItems.stream().map(q -> q.barcode).collect(Collectors.toList());
            int newId = borrowDAO.createBorrowTransaction(bId, loggedInUserId, classId, reqId,
                    dpRet.getValue().toString(), barcodes);
            if (newId > 0) {
                AuditLog.log(loggedInUserId, authenticatedUsername, "CREATE_BORROW",
                        "borrow_transaction", String.valueOf(newId),
                        "Borrower: " + bId + " | Items: " + String.join(",", barcodes));
                new Alert(Alert.AlertType.INFORMATION,
                        "✅ Borrow transaction created!\nTransaction ID: #" + newId +
                                "\n\nGive ID #" + newId + " to the borrower.").showAndWait();
                viewEquipmentLogic();
            } else { showError("Transaction failed. Items may no longer be AVAILABLE."); }
        });

        root.getChildren().addAll(topForm, new Separator(), lblPick, pickerPane, btnSub);
        mainLayout.setCenter(root);
    }

    private String resolveBId(ComboBox<String> cb, TextField tf) {
        String m = tf.getText().trim(); if (!m.isEmpty()) return m;
        String v = cb.getValue();
        if (v != null && v.contains("|")) return v.substring(0, v.indexOf("|")).trim();
        return v != null ? v.trim() : "";
    }

    // ================================================================
    // RETURN FORM
    // ================================================================
    private void viewReturnForm() {
        VBox root = createContentRoot("↩️ Process Equipment Return");
        Label hint = new Label("Select an open transaction from the list, or type its ID:");
        hint.setStyle("-fx-font-size:12px;-fx-text-fill:#555;");

        List<DataClass.UnreturnedRecord> openList = new ArrayList<>();
        openList.addAll(borrowDAO.getUnreturnedByStatus("BORROWED"));
        openList.addAll(borrowDAO.getUnreturnedByStatus("OVERDUE"));

        ComboBox<String> cbTxn = new ComboBox<>(); cbTxn.setPromptText("— pick an open transaction —"); cbTxn.setPrefWidth(520);
        for (DataClass.UnreturnedRecord ur : openList)
            cbTxn.getItems().add("#" + ur.transactionId + " | " + ur.borrowerName +
                    " | Due: " + ur.expectedReturn + " | " + ur.status + " | " + ur.items);

        TextField txtTId = new TextField(); txtTId.setPromptText("Transaction ID"); txtTId.setPrefWidth(120);
        Button btnLoad = new Button("Load");
        cbTxn.setOnAction(e -> {
            String s = cbTxn.getValue();
            if (s != null && s.startsWith("#")) txtTId.setText(s.substring(1, s.indexOf(' ')));
        });

        GridPane step1 = new GridPane(); step1.setHgap(12); step1.setVgap(10);
        step1.add(new Label("Transaction ID:"), 0, 0); step1.add(txtTId, 1, 0); step1.add(btnLoad, 2, 0);

        Label lblInfo = new Label("Select or enter a transaction, then click Load.");
        lblInfo.setStyle("-fx-font-size:12px;-fx-text-fill:#555;");
        VBox itemsBox = new VBox(8);
        ComboBox<String> cbOvStat = new ComboBox<>(FXCollections.observableArrayList("RETURNED","RETURNED_WITH_ISSUE"));
        cbOvStat.setValue("RETURNED"); cbOvStat.setDisable(true);
        Button btnRet = new Button("✅ CONFIRM RETURN");
        btnRet.setStyle("-fx-background-color:#2980b9;-fx-text-fill:white;-fx-font-weight:bold;"); btnRet.setDisable(true);

        final int[] loadedId = {-1};
        final List<DataClass.BorrowItem> loadedItems = new ArrayList<>();
        final List<ComboBox<String>> condBoxes = new ArrayList<>();
        final List<TextField> noteFields = new ArrayList<>();

        btnLoad.setOnAction(e -> {
            String ids = txtTId.getText().trim();
            if (ids.isEmpty()) { showError("Transaction ID required."); return; }
            int txnId; try { txnId = Integer.parseInt(ids); } catch (NumberFormatException ex) { showError("ID must be integer."); return; }
            DataClass.BorrowRecord txn = borrowDAO.getTransactionById(txnId);
            if (txn == null) { lblInfo.setText("Transaction #" + txnId + " not found."); return; }
            if ("RETURNED".equalsIgnoreCase(txn.getStatus()) || "RETURNED_WITH_ISSUE".equalsIgnoreCase(txn.getStatus())) {
                lblInfo.setText("Transaction #" + txnId + " already returned (" + txn.getStatus() + ").");
                btnRet.setDisable(true); cbOvStat.setDisable(true); itemsBox.getChildren().clear(); return;
            }
            List<DataClass.BorrowItem> items = borrowDAO.getItemsByTransactionId(txnId);
            if (items.isEmpty()) { lblInfo.setText("No items found for #" + txnId + "."); return; }
            loadedId[0] = txnId; loadedItems.clear(); loadedItems.addAll(items);
            condBoxes.clear(); noteFields.clear(); itemsBox.getChildren().clear();
            lblInfo.setText("Transaction #" + txnId + " — Borrower: " + txn.getBorrowerName() + " | " + items.size() + " item(s):");
            for (DataClass.BorrowItem item : items) {
                Label lbl = new Label(item.barcode + " — " + item.itemName + " (out: " + item.conditionOut + ")");
                lbl.setStyle("-fx-font-weight:bold;");
                ComboBox<String> cbC = new ComboBox<>(FXCollections.observableArrayList("Good","Damaged")); cbC.setValue("Good");
                TextField tfN = new TextField(); tfN.setPromptText("Damage notes"); tfN.setDisable(true); tfN.setPrefWidth(240);
                cbC.setOnAction(ev -> tfN.setDisable(!"Damaged".equals(cbC.getValue())));
                condBoxes.add(cbC); noteFields.add(tfN);
                itemsBox.getChildren().add(new HBox(10, lbl, new Label("Condition:"), cbC, tfN) {{ setAlignment(Pos.CENTER_LEFT); }});
            }
            cbOvStat.setDisable(false); btnRet.setDisable(false);
        });

        btnRet.setOnAction(e -> {
            if (loadedId[0] == -1) { showError("Load a transaction first."); return; }
            List<DataClass.BorrowItemReturnInfo> retItems = new ArrayList<>();
            for (int i = 0; i < loadedItems.size(); i++) {
                String notes = noteFields.get(i).getText().trim();
                if (notes.length() > 255) { showError("Damage notes ≤ 255 chars."); return; }
                retItems.add(new DataClass.BorrowItemReturnInfo(
                        loadedItems.get(i).barcode, condBoxes.get(i).getValue(),
                        notes.isEmpty() ? null : notes));
            }
            boolean dmg = retItems.stream().anyMatch(ri -> "Damaged".equalsIgnoreCase(ri.conditionIn));
            String finalStat = dmg ? "RETURNED_WITH_ISSUE" : cbOvStat.getValue();
            if (borrowDAO.returnBorrowTransaction(loadedId[0], loggedInUserId, finalStat, retItems)) {
                AuditLog.log(loggedInUserId, authenticatedUsername, "PROCESS_RETURN",
                        "borrow_transaction", String.valueOf(loadedId[0]),
                        "Status: " + finalStat + (dmg ? " | Damage recorded" : ""));
                new Alert(Alert.AlertType.INFORMATION, "✅ Return processed!\nStatus: " + finalStat).showAndWait();
                viewEquipmentLogic();
            } else { showError("Return failed. Please try again."); }
        });

        HBox statRow = new HBox(12, new Label("Overall Status:"), cbOvStat, btnRet);
        statRow.setAlignment(Pos.CENTER_LEFT); statRow.setPadding(new Insets(12, 0, 0, 0));
        root.getChildren().addAll(hint, cbTxn, new Separator(), step1, lblInfo, new Separator(), itemsBox, statRow);
        mainLayout.setCenter(root);
    }

    // ================================================================
    // OVERDUE REPORT
    // ================================================================
    private void viewOverdueReport() {
        VBox root = createContentRoot("⚠️ Overdue / Problematic Transactions");
        VBox explainBox = new VBox(5);
        explainBox.setStyle("-fx-background-color:#fef9e7;-fx-border-color:#f39c12;-fx-border-width:0 0 0 5;-fx-padding:12;-fx-background-radius:4;-fx-border-radius:4;");
        Label et = new Label("ℹ️  How does this page work?"); et.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:#d35400;");
        Label eb = new Label(
                "STEP 1 — Click \"Run Overdue Check\". This calls the stored procedure " +
                        "sp_mark_overdue_transactions() which scans BORROWED transactions and changes " +
                        "those past their due date to OVERDUE.\n\n" +
                        "STEP 2 — The table shows all OVERDUE (not returned, past due) and RETURNED_WITH_ISSUE " +
                        "(returned with damage) transactions.\n\n" +
                        "⚠ If you skip Step 1, late borrows still show as BORROWED until the check runs.");
        eb.setStyle("-fx-font-size:12px;-fx-text-fill:#555;-fx-wrap-text:true;"); eb.setMaxWidth(900);
        explainBox.getChildren().addAll(et, eb);

        Button btnRun = new Button("▶ Run Overdue Check (Stored Procedure)");
        Button btnRef = new Button("↺ Refresh List Only");
        btnRun.setStyle("-fx-background-color:#c0392b;-fx-text-fill:white;-fx-font-weight:bold;");
        HBox btnBar = new HBox(10, btnRun, btnRef); btnBar.setPadding(new Insets(10, 0, 12, 0));

        TableView<DataClass.UnreturnedRecord> table = new TableView<>();
        setupCols(table, "transactionId","borrowerName","borrowerId","expectedReturn","status","items");
        table.setRowFactory(tv -> new TableRow<DataClass.UnreturnedRecord>() {
            @Override protected void updateItem(DataClass.UnreturnedRecord item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) { setStyle(""); return; }
                setStyle("OVERDUE".equals(item.status) ? "-fx-background-color:#fadbd8;" : "-fx-background-color:#fdf2e9;");
            }
        });
        Runnable reload = () -> table.setItems(FXCollections.observableArrayList(borrowDAO.getAllProblematicRecords()));
        btnRun.setOnAction(e -> {
            borrowDAO.markOverdueTransactions();
            AuditLog.log(loggedInUserId, authenticatedUsername, "RUN_OVERDUE_CHECK", null, null,
                    "sp_mark_overdue_transactions executed");
            reload.run();
            new Alert(Alert.AlertType.INFORMATION,
                    "✅ Overdue check complete!\nBORROWED transactions past due are now OVERDUE.\nTable refreshed.").showAndWait();
        });
        btnRef.setOnAction(e -> reload.run());
        reload.run();
        root.getChildren().addAll(explainBox, btnBar, table);
        mainLayout.setCenter(root);
    }

    // ================================================================
    // CALENDAR
    // ================================================================
    private void viewCalendar() {
        VBox root = createContentRoot("📅 Return Date Calendar");
        final LocalDate[] cur = {LocalDate.now().withDayOfMonth(1)};
        Label lblM = new Label(); lblM.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#2c3e50;");
        Button btnPrev = new Button("◀ Prev"); Button btnNext = new Button("Next ▶");
        btnPrev.setStyle("-fx-background-color:#34495e;-fx-text-fill:white;");
        btnNext.setStyle("-fx-background-color:#34495e;-fx-text-fill:white;");
        HBox nav = new HBox(16, btnPrev, lblM, btnNext); nav.setAlignment(Pos.CENTER);

        HBox legend = new HBox(20,
                legendBadge("🟡 BORROWED due",  "#fef9e7", "#f39c12"),
                legendBadge("🔴 OVERDUE due",   "#fadbd8", "#e74c3c"),
                legendBadge("🔵 Today",         "#d6eaf8", "#2980b9"));
        legend.setPadding(new Insets(8, 0, 10, 0));

        GridPane cal = new GridPane(); cal.setHgap(4); cal.setVgap(4);

        TableView<DataClass.BorrowRecord> detTbl = new TableView<>();
        detTbl.setPrefHeight(180);
        addCol(detTbl, "TXN#",       "transactionId");
        addCol(detTbl, "Borrower",   "borrowerName");
        addCol(detTbl, "Return Due", "expectedReturn");
        addCol(detTbl, "Status",     "status");
        detTbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Label lblDet = new Label("Click a day to see transactions due on that date.");
        lblDet.setStyle("-fx-font-size:12px;-fx-text-fill:#555;-fx-padding:8 0 4 0;");

        List<DataClass.BorrowRecord> openBorrows = new ArrayList<>();
        if ("BORROWER".equalsIgnoreCase(authenticatedRole) && loggedInUserId != null) {
            borrowDAO.getRecordsByBorrowerId(loggedInUserId).stream()
                    .filter(b -> "BORROWED".equals(b.status) || "OVERDUE".equals(b.status))
                    .forEach(openBorrows::add);
        } else {
            openBorrows.addAll(borrowDAO.getRecordsByStatus("BORROWED"));
            openBorrows.addAll(borrowDAO.getRecordsByStatus("OVERDUE"));
        }

        Runnable rebuildCal = () -> {
            cal.getChildren().clear();
            YearMonth ym = YearMonth.from(cur[0]);
            lblM.setText(ym.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + ym.getYear());
            String[] dh = {"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
            for (int d = 0; d < 7; d++) {
                Label h = new Label(dh[d]); h.setStyle("-fx-font-weight:bold;-fx-font-size:11px;-fx-text-fill:#7f8c8d;-fx-min-width:82;-fx-alignment:center;"); cal.add(h, d, 0);
            }
            int startDow = cur[0].getDayOfWeek().getValue() % 7;
            for (int day = 1; day <= ym.lengthOfMonth(); day++) {
                LocalDate date = cur[0].withDayOfMonth(day);
                final String ds = date.toString();
                int col = (startDow + day - 1) % 7, row = (startDow + day - 1) / 7 + 1;
                long due = openBorrows.stream().filter(b -> ds.equals(b.expectedReturn)).count();
                long ovd = openBorrows.stream().filter(b -> ds.equals(b.expectedReturn) && "OVERDUE".equals(b.status)).count();
                VBox cell = new VBox(3); cell.setMinSize(82, 62); cell.setPadding(new Insets(5)); cell.setAlignment(Pos.TOP_CENTER);
                String cs = date.equals(LocalDate.now())
                        ? "-fx-background-color:#d6eaf8;-fx-border-color:#2980b9;-fx-border-width:2;-fx-border-radius:6;-fx-background-radius:6;"
                        : (ovd > 0 ? "-fx-background-color:#fadbd8;-fx-border-color:#e74c3c;-fx-border-width:1;-fx-border-radius:6;-fx-background-radius:6;"
                        : (due > 0 ? "-fx-background-color:#fef9e7;-fx-border-color:#f39c12;-fx-border-width:1;-fx-border-radius:6;-fx-background-radius:6;"
                        : "-fx-background-color:#f8f9fa;-fx-border-color:#dee2e6;-fx-border-width:1;-fx-border-radius:6;-fx-background-radius:6;"));
                cell.setStyle(cs);
                Label ld = new Label(String.valueOf(day)); ld.setStyle("-fx-font-weight:bold;-fx-font-size:13px;");
                cell.getChildren().add(ld);
                if (due > 0) { Label b2 = new Label(due + " due"); b2.setStyle("-fx-font-size:10px;-fx-text-fill:#c0392b;-fx-font-weight:bold;"); cell.getChildren().add(b2); }
                cell.setOnMouseClicked(ev -> {
                    List<DataClass.BorrowRecord> dayBorrows = openBorrows.stream().filter(b -> ds.equals(b.expectedReturn)).collect(Collectors.toList());
                    detTbl.setItems(FXCollections.observableArrayList(dayBorrows));
                    lblDet.setText("Transactions due on " + ds + " (" + dayBorrows.size() + " found):");
                });
                cal.add(cell, col, row);
            }
        };
        rebuildCal.run();
        btnPrev.setOnAction(e -> { cur[0] = cur[0].minusMonths(1); rebuildCal.run(); });
        btnNext.setOnAction(e -> { cur[0] = cur[0].plusMonths(1);  rebuildCal.run(); });

        ScrollPane sp = new ScrollPane(cal); sp.setFitToWidth(true); sp.setPrefHeight(380); sp.setStyle("-fx-background-color:transparent;");
        root.getChildren().addAll(nav, legend, sp, lblDet, detTbl);
        mainLayout.setCenter(root);
    }

    private Label legendBadge(String text, String bg, String border) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color:" + bg + ";-fx-border-color:" + border +
                ";-fx-border-width:1;-fx-border-radius:4;-fx-background-radius:4;-fx-padding:3 8 3 8;-fx-font-size:11px;");
        return l;
    }

    // ================================================================
    // STUDENTS / CLASSES / ACTIVITY REQUEST
    // ================================================================
    private void viewStudents() {
        VBox root = createContentRoot("🎓 Student Directory");
        TableView<DataClass.StudentRecord> t = new TableView<>();
        setupCols(t, "personId","lastName","firstName","email","status");
        t.setItems(FXCollections.observableArrayList(personDAO.getAllStudents()));
        root.getChildren().add(t); mainLayout.setCenter(root);
    }

    private void viewLabClasses() {
        VBox root = createContentRoot("🏫 Laboratory Class Schedules");
        TableView<DataClass.LabClassRecord> t = new TableView<>();
        setupCols(t, "classId","courseCode","section","semester","schoolYear","room","schedule","facultyName","enrolledCount");
        t.setItems(FXCollections.observableArrayList(labClassDAO.getAllClasses()));
        root.getChildren().add(t); mainLayout.setCenter(root);
    }

    private void viewRequestForm() {
        VBox root = createContentRoot("📝 Submit New Activity Request");
        GridPane form = new GridPane(); form.setHgap(20); form.setVgap(15);
        TextField txtName = new TextField(); txtName.setPromptText("Activity name (max 100 chars)");
        ComboBox<String> cbType = new ComboBox<>(FXCollections.observableArrayList("RECRUITMENT","CERTIFICATION","MEETING","TRAINING","OTHER"));
        cbType.setValue("TRAINING"); cbType.setPrefWidth(200);
        DatePicker dp = new DatePicker();
        dp.setDayCellFactory(p -> new DateCell() {
            @Override public void updateItem(LocalDate d, boolean empty) { super.updateItem(d, empty); setDisable(empty || d.isBefore(LocalDate.now())); }
        });
        TextField txtLoc = new TextField(); txtLoc.setPromptText("Room/Facility (max 100 chars)");
        TextArea txtNotes = new TextArea(); txtNotes.setPromptText("Optional notes (max 255 chars)"); txtNotes.setPrefHeight(80);
        form.add(new Label("Activity Name:"), 0, 0); form.add(txtName,  1, 0);
        form.add(new Label("Activity Type:"), 0, 1); form.add(cbType,   1, 1);
        form.add(new Label("Activity Date:"), 0, 2); form.add(dp,       1, 2);
        form.add(new Label("Location:"),      0, 3); form.add(txtLoc,   1, 3);
        form.add(new Label("Notes:"),         0, 4); form.add(txtNotes, 1, 4);
        Button btnSub = styledBtn("📤 SUBMIT REQUEST", "#27ae60");
        btnSub.setOnAction(e -> {
            if (txtName.getText().trim().isEmpty())  { showError("Activity name required."); return; }
            if (txtName.getText().length() > 100)    { showError("Name ≤ 100 chars."); return; }
            if (dp.getValue() == null)               { showError("Select a valid date."); return; }
            if (txtLoc.getText().length() > 100)     { showError("Location ≤ 100 chars."); return; }
            if (txtNotes.getText().length() > 255)   { showError("Notes ≤ 255 chars."); return; }
            int res = activityDAO.addActivity(txtName.getText().trim(), cbType.getValue(),
                    dp.getValue().toString(), txtLoc.getText().trim(), loggedInUserId, txtNotes.getText().trim());
            if (res != -1) {
                AuditLog.log(loggedInUserId, authenticatedUsername, "SUBMIT_ACTIVITY_REQUEST",
                        "activity_request", String.valueOf(res), "Activity: " + txtName.getText().trim());
                new Alert(Alert.AlertType.INFORMATION, "✅ Request submitted!\nRequest ID: " + res).showAndWait();
                viewBorrowerHistoryLogic();
            } else { showError("Submission failed."); }
        });
        root.getChildren().addAll(form, btnSub); mainLayout.setCenter(root);
    }

    // ================================================================
    // USER ACCOUNTS (Admin)
    // ================================================================
    private void viewUserAccounts() {
        VBox root = createContentRoot("👤 User Account Management");
        Label lblEx = new Label("Existing Accounts"); lblEx.setStyle("-fx-font-weight:bold;-fx-font-size:14px;");
        TableView<String[]> tbl = new TableView<>();
        String[] hdrs = {"ID","Person ID","Username","Role","Account Status","Person Status","Full Name","Created"};
        for (int i = 0; i < hdrs.length; i++) {
            final int idx = i;
            TableColumn<String[], String> col = new TableColumn<>(hdrs[i]);
            col.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue()[idx]));
            tbl.getColumns().add(col);
        }
        tbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY); tbl.setPrefHeight(220);

        // Action Column: Toggle Account & Archive Person
        TableColumn<String[], Void> actCol = new TableColumn<>("Actions");
        actCol.setMinWidth(220);
        actCol.setCellFactory(p -> new TableCell<String[], Void>() {
            private final Button btnTog = new Button();
            private final Button btnArc = new Button("Archive");
            private final HBox box = new HBox(6, btnTog, btnArc);
            {
                btnArc.setStyle("-fx-background-color:#8e44ad;-fx-text-fill:white;-fx-font-size:10px;");
                btnArc.setOnAction(e -> {
                    String[] row = getTableView().getItems().get(getIndex());
                    if (((Person) personDAO).archivePerson(row[1])) {
                        AuditLog.log(loggedInUserId, authenticatedUsername, "ARCHIVE_PERSON", "person", row[1], "Archived: " + row[6]);
                        refreshAcctTable(tbl);
                        new Alert(Alert.AlertType.INFORMATION, "Person archived successfully.").showAndWait();
                    } else { showError("Could not archive. They may have active borrows."); }
                });
                btnTog.setOnAction(e -> {
                    String[] row = getTableView().getItems().get(getIndex());
                    boolean makeActive = "Inactive".equals(row[4]);
                    if (((Person) personDAO).setAccountActive(Integer.parseInt(row[0]), makeActive)) {
                        AuditLog.log(loggedInUserId, authenticatedUsername,
                                makeActive ? "ACTIVATE_ACCOUNT" : "DEACTIVATE_ACCOUNT",
                                "user_account", row[0], "Account: " + row[2]);
                        refreshAcctTable(tbl);
                    } else { showError("Could not toggle account."); }
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                String[] row = getTableView().getItems().get(getIndex());
                boolean inactive = "Inactive".equals(row[4]);
                btnTog.setText(inactive ? "Activate" : "Deactivate");
                btnTog.setStyle(inactive ? "-fx-background-color:#27ae60;-fx-text-fill:white;-fx-font-size:10px;"
                        : "-fx-background-color:#e74c3c;-fx-text-fill:white;-fx-font-size:10px;");
                setGraphic(box);
            }
        });
        tbl.getColumns().add(actCol);
        Button btnRef = new Button("↺ Refresh"); btnRef.setOnAction(e -> refreshAcctTable(tbl)); refreshAcctTable(tbl);

        Separator sep = new Separator(); sep.setPadding(new Insets(14, 0, 14, 0));
        Label lblCr = new Label("Register New Person & Account"); lblCr.setStyle("-fx-font-weight:bold;-fx-font-size:14px;");

        TextField txtPId = new TextField(); txtPId.setPromptText("ID (e.g. 2021-00013 or FAC-004)");
        TextField txtFname = new TextField(); txtFname.setPromptText("First Name");
        TextField txtLname = new TextField(); txtLname.setPromptText("Last Name");
        TextField txtMname = new TextField(); txtMname.setPromptText("Middle Name (Optional)");
        ComboBox<String> cbPType = new ComboBox<>(FXCollections.observableArrayList("STUDENT","FACULTY","STAFF"));
        cbPType.setValue("STUDENT"); cbPType.setPrefWidth(160);
        TextField txtEmail = new TextField(); txtEmail.setPromptText("Email");
        TextField txtContact = new TextField(); txtContact.setPromptText("Contact #");

        Separator sep2 = new Separator(); sep2.setPadding(new Insets(6, 0, 6, 0));

        TextField txtUname = new TextField(); txtUname.setPromptText("Username (unique, ≤50 chars)");
        PasswordField pfP = new PasswordField(); pfP.setPromptText("Password");
        PasswordField pfC = new PasswordField(); pfC.setPromptText("Confirm Password");
        ComboBox<String> cbRole = new ComboBox<>(FXCollections.observableArrayList("BORROWER","CUSTODIAN","ADMIN"));
        cbRole.setValue("BORROWER"); cbRole.setPrefWidth(160);

        GridPane cForm = new GridPane(); cForm.setHgap(14); cForm.setVgap(10);
        int r = 0;
        cForm.add(new Label("Person ID:"),       0, r); cForm.add(txtPId,    1, r);
        cForm.add(new Label("Person Type:"),     2, r); cForm.add(cbPType,   3, r++);
        cForm.add(new Label("First Name:"),      0, r); cForm.add(txtFname,  1, r);
        cForm.add(new Label("Last Name:"),       2, r); cForm.add(txtLname,  3, r++);
        cForm.add(new Label("Middle Name:"),     0, r); cForm.add(txtMname,  1, r);
        cForm.add(new Label("Email:"),           2, r); cForm.add(txtEmail,  3, r++);
        cForm.add(new Label("Contact #:"),       0, r); cForm.add(txtContact,1, r);
        cForm.add(new Label(""),                 2, r); cForm.add(new Label(""), 3, r++); // spacer

        cForm.add(new Label("Username:"),        0, r); cForm.add(txtUname,  1, r);
        cForm.add(new Label("Role:"),            2, r); cForm.add(cbRole,    3, r++);
        cForm.add(new Label("Password:"),        0, r); cForm.add(pfP,       1, r);
        cForm.add(new Label("Confirm PW:"),      2, r); cForm.add(pfC,       3, r++);

        Button btnCr = styledBtn("➕ Register User", "#2980b9");
        Label lblSt = new Label(); lblSt.setStyle("-fx-font-size:12px;");

        btnCr.setOnAction(e -> {
            String pid  = txtPId.getText().trim();
            String fn   = txtFname.getText().trim();
            String ln   = txtLname.getText().trim();
            String mn   = txtMname.getText().trim();
            String ptype= cbPType.getValue();
            String em   = txtEmail.getText().trim();
            String cont = txtContact.getText().trim();
            String un   = txtUname.getText().trim();
            String pw   = pfP.getText();
            String pwc  = pfC.getText();
            String role = cbRole.getValue();

            if (pid.isEmpty() || fn.isEmpty() || ln.isEmpty()) { lblSt.setText("❌ Person ID, First & Last Name required."); lblSt.setStyle("-fx-text-fill:#e74c3c;"); return; }
            if (un.isEmpty())    { lblSt.setText("❌ Username required.");      lblSt.setStyle("-fx-text-fill:#e74c3c;"); return; }
            if (pw.isEmpty())    { lblSt.setText("❌ Password required.");      lblSt.setStyle("-fx-text-fill:#e74c3c;"); return; }
            if (!pw.equals(pwc)) { lblSt.setText("❌ Passwords do not match."); lblSt.setStyle("-fx-text-fill:#e74c3c;"); return; }

            boolean success = ((Person) personDAO).registerNewUser(pid, ln, fn, mn, ptype, em, cont, un, pw, role);
            if (success) {
                AuditLog.log(loggedInUserId, authenticatedUsername, "REGISTER_NEW_USER",
                        "person", pid, "Registered: " + ln + ", " + fn + " | Account: " + un);
                lblSt.setStyle("-fx-text-fill:#27ae60;"); lblSt.setText("✅ User registered successfully!");
                txtPId.clear(); txtFname.clear(); txtLname.clear(); txtMname.clear(); txtEmail.clear(); txtContact.clear();
                txtUname.clear(); pfP.clear(); pfC.clear(); cbRole.setValue("BORROWER"); cbPType.setValue("STUDENT");
                refreshAcctTable(tbl);
            } else {
                lblSt.setStyle("-fx-text-fill:#e74c3c;"); lblSt.setText("❌ Failed. Person ID or Username may already exist.");
            }
        });

        root.getChildren().addAll(lblEx, new HBox(10, btnRef), tbl, sep, lblCr, cForm, btnCr, lblSt);
        mainLayout.setCenter(root);
    }

    private void refreshAcctTable(TableView<String[]> t) {
        t.setItems(FXCollections.observableArrayList(((Person) personDAO).getAllAccounts()));
    }

    // ================================================================
    // AUDIT LOG (Admin)
    // ================================================================
    private void viewAuditLog() {
        VBox root = createContentRoot("📜 Audit Log");
        Label info = new Label("All significant user actions, newest first.");
        info.setStyle("-fx-font-size:12px;-fx-text-fill:#555;");
        TextField txtSrch = new TextField(); txtSrch.setPromptText("Search action, username, or detail..."); txtSrch.setPrefWidth(280);
        ComboBox<String> cbLim = new ComboBox<>(FXCollections.observableArrayList("50 entries","100 entries","200 entries","500 entries"));
        cbLim.setValue("100 entries");
        Button btnSrch = new Button("🔍 Search"); Button btnRef = new Button("↺ Show Recent");
        HBox bar = new HBox(10, new Label("Filter:"), txtSrch, cbLim, btnSrch, btnRef);
        bar.setAlignment(Pos.CENTER_LEFT); bar.setPadding(new Insets(0, 0, 10, 0));
        TableView<DataClass.AuditEntry> table = new TableView<>();
        setupCols(table, "logId","loggedAt","username","personId","action","entity","entityId","details");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(FXCollections.observableArrayList(auditLogDAO.getRecentLogs(100)));
        btnRef.setOnAction(e -> { txtSrch.clear(); int l = Integer.parseInt(cbLim.getValue().split(" ")[0]); table.setItems(FXCollections.observableArrayList(auditLogDAO.getRecentLogs(l))); });
        btnSrch.setOnAction(e -> { String k = txtSrch.getText().trim(); if (k.isEmpty()) { int l = Integer.parseInt(cbLim.getValue().split(" ")[0]); table.setItems(FXCollections.observableArrayList(auditLogDAO.getRecentLogs(l))); } else { table.setItems(FXCollections.observableArrayList(auditLogDAO.searchLogs(k))); } });
        root.getChildren().addAll(info, bar, table); mainLayout.setCenter(root);
    }

    // ================================================================
    // POPUPS
    // ================================================================
    private void showItemsPopup(int txnId, String title) {
        Stage popup = new Stage(); popup.setTitle("Items — " + title);
        VBox box = new VBox(10); box.setPadding(new Insets(16));
        TableView<DataClass.BorrowItem> t = new TableView<>();
        setupCols(t, "barcode","itemName","conditionOut","conditionIn","damageNotes");
        t.setItems(FXCollections.observableArrayList(borrowDAO.getItemsByTransactionId(txnId)));
        t.setPrefHeight(220); t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        box.getChildren().addAll(new Label("Transaction #" + txnId + " — borrowed items:"), t);
        popup.setScene(new Scene(box, 720, 290)); popup.show();
    }

    private void showActivityBorrowsPopup(int reqId, String actName) {
        Stage popup = new Stage(); popup.setTitle("Borrows for: " + actName);
        VBox box = new VBox(10); box.setPadding(new Insets(16));
        List<DataClass.BorrowRecord> records = borrowDAO.getRecordsByRequestId(reqId);
        if (records.isEmpty()) {
            box.getChildren().add(new Label("No borrow transactions linked to this activity yet."));
        } else {
            TableView<DataClass.BorrowRecord> t = new TableView<>();
            setupCols(t, "transactionId","borrowerName","borrowerId","borrowDate","expectedReturn","returnDate","status");
            t.setItems(FXCollections.observableArrayList(records)); t.setPrefHeight(240);
            t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            box.getChildren().addAll(new Label("Activity: " + actName + " (Request #" + reqId + ")"), t);
        }
        popup.setScene(new Scene(box, 920, 330)); popup.show();
    }

    // ================================================================
    // UTILITIES
    // ================================================================
    private VBox createContentRoot(String title) {
        VBox root = new VBox(14); root.setPadding(new Insets(26)); root.setStyle("-fx-background-color:white;");
        Label lbl = new Label(title); lbl.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:#2c3e50;");
        root.getChildren().add(lbl);
        return root;
    }

    private <T> void setupCols(TableView<T> table, String... fields) {
        table.getColumns().clear();
        for (String f : fields) {
            String h = f.replaceAll("([a-z])([A-Z])","$1 $2").replaceAll("([A-Z]+)([A-Z][a-z])","$1 $2").toUpperCase();
            TableColumn<T, String> col = new TableColumn<>(h);
            col.setCellValueFactory(new PropertyValueFactory<>(f));
            table.getColumns().add(col);
        }
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    /** PropertyValueFactory-based column (works for POJO tables with getters). */
    private <T> void addCol(TableView<T> table, String header, String field) {
        TableColumn<T, String> col = new TableColumn<>(header);
        col.setCellValueFactory(new PropertyValueFactory<>(field));
        table.getColumns().add(col);
    }

    private void colorBorrowRows(TableView<DataClass.BorrowRecord> table) {
        table.setRowFactory(tv -> new TableRow<DataClass.BorrowRecord>() {
            @Override protected void updateItem(DataClass.BorrowRecord item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) { setStyle(""); return; }
                switch (item.status) {
                    case "OVERDUE":             setStyle("-fx-background-color:#fadbd8;"); break;
                    case "BORROWED":            setStyle("-fx-background-color:#fef9e7;"); break;
                    case "RETURNED_WITH_ISSUE": setStyle("-fx-background-color:#fdf2e9;"); break;
                    default:                    setStyle(""); break;
                }
            }
        });
    }

    private Button styledBtn(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:13px;");
        btn.setPadding(new Insets(10, 20, 10, 20));
        return btn;
    }

    private void showError(String msg) { new Alert(Alert.AlertType.ERROR, msg).showAndWait(); }

    public static void main(String[] args) { launch(args); }
}