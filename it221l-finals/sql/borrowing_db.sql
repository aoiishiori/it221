-- phpMyAdmin SQL Dump
-- version 5.2.3
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1:3306
-- Generation Time: May 13, 2026 at 12:15 AM
-- Server version: 8.0.45
-- PHP Version: 8.3.28

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `borrowing_db`
--

DELIMITER $$
--
-- Procedures
--
DROP PROCEDURE IF EXISTS `sp_mark_overdue_transactions`$$
CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_mark_overdue_transactions` ()   BEGIN
    UPDATE borrow_transaction
    SET transaction_status = 'OVERDUE'
    WHERE transaction_status = 'BORROWED'
      AND expected_return < CURDATE();
END$$

--
-- Functions
--
DROP FUNCTION IF EXISTS `fn_count_active_borrows`$$
CREATE DEFINER=`root`@`localhost` FUNCTION `fn_count_active_borrows` (`p_person_id` VARCHAR(20)) RETURNS INT DETERMINISTIC BEGIN
    DECLARE active_count INT;
    SELECT COUNT(*) INTO active_count
    FROM borrow_transaction
    WHERE borrower_id = p_person_id
      AND transaction_status IN ('BORROWED', 'OVERDUE');
    RETURN active_count;
END$$

DELIMITER ;

-- --------------------------------------------------------

--
-- Table structure for table `activity_request`
--

DROP TABLE IF EXISTS `activity_request`;
CREATE TABLE IF NOT EXISTS `activity_request` (
  `request_id` int NOT NULL AUTO_INCREMENT,
  `activity_name` varchar(100) NOT NULL,
  `activity_type` enum('TRAINING','MEETING','RECRUITMENT','CERTIFICATION','OTHER') NOT NULL,
  `activity_date` date NOT NULL,
  `location` varchar(100) DEFAULT NULL,
  `requested_by` varchar(20) NOT NULL,
  `approved_by` varchar(20) DEFAULT NULL,
  `status` enum('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
  `notes` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`request_id`),
  KEY `fk_request_requester` (`requested_by`),
  KEY `fk_request_approver` (`approved_by`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `activity_request`
--

INSERT INTO `activity_request` (`request_id`, `activity_name`, `activity_type`, `activity_date`, `location`, `requested_by`, `approved_by`, `status`, `notes`) VALUES
(1, 'CIS Recruitment Drive', 'RECRUITMENT', '2026-03-15', 'Gym', 'FAC-001', 'FAC-003', 'APPROVED', 'Annual recruitment for incoming freshmen'),
(2, 'AWS Certification Seminar', 'CERTIFICATION', '2026-04-10', 'AVR 2', '2021-00001', 'FAC-001', 'APPROVED', 'Batch exam simulation'),
(3, 'Faculty Team Meeting', 'MEETING', '2026-04-08', 'Faculty Room', 'FAC-002', 'FAC-003', 'APPROVED', NULL),
(4, 'Python Training Workshop', 'TRAINING', '2026-04-20', 'CIS Lab 2', '2021-00005', 'STAFF-001', 'APPROVED', 'Needs 10 laptops'),
(5, 'Campus Job Fair', 'OTHER', '2026-05-05', 'Covered Court', 'FAC-003', NULL, 'PENDING', NULL),
(6, 'Requesting Speakers', 'TRAINING', '2026-05-14', 'Gym', '2021-00001', NULL, 'PENDING', 'For training purposes'),
(7, 'Java Design Patterns Workshop', 'TRAINING', '2026-05-28', 'D428', '2021-00001', 'STAFF-002', 'REJECTED', 'Requesting to borrow 5 laptops and a projector for a peer-to-peer coding session.\n\n// test run'),
(8, 'Project Meeting', 'MEETING', '2026-05-22', 'D412', '2021-00001', 'STAFF-002', 'APPROVED', ''),
(9, 'Advanced World Systems Certificate', 'CERTIFICATION', '2026-05-30', 'Alabang', '2021-00001', 'STAFF-002', 'APPROVED', 'Awarded for completing the Advanced World Systems Adminstration Course');

-- --------------------------------------------------------

--
-- Table structure for table `borrow_item`
--

DROP TABLE IF EXISTS `borrow_item`;
CREATE TABLE IF NOT EXISTS `borrow_item` (
  `transaction_id` int NOT NULL,
  `barcode` varchar(30) NOT NULL,
  `item_condition_out` varchar(100) NOT NULL DEFAULT 'Good',
  `item_condition_in` varchar(100) DEFAULT NULL,
  `damage_notes` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`transaction_id`,`barcode`),
  KEY `fk_bi_equipment` (`barcode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `borrow_item`
--

INSERT INTO `borrow_item` (`transaction_id`, `barcode`, `item_condition_out`, `item_condition_in`, `damage_notes`) VALUES
(1, 'EQ-0006', 'Good', 'Good', NULL),
(1, 'EQ-0009', 'Good', 'Good', NULL),
(2, 'EQ-0007', 'Good', 'Damaged', 'Left button unresponsive upon return'),
(3, 'EQ-0004', 'Good', 'Good', NULL),
(3, 'EQ-0013', 'Good', 'Good', NULL),
(4, 'EQ-0002', 'Good', 'Good', NULL),
(4, 'EQ-0018', 'Good', 'Good', NULL),
(5, 'EQ-0012', 'Good', NULL, NULL),
(6, 'EQ-0010', 'Good', NULL, NULL),
(6, 'EQ-0011', 'Good', NULL, NULL),
(7, 'EQ-0001', 'Good', 'Good', NULL),
(7, 'EQ-0019', 'Good', 'Damaged', 'Missing keycaps'),
(8, 'EQ-0001', 'Good', NULL, NULL),
(9, 'EQ-0019', 'Good', NULL, NULL);

-- --------------------------------------------------------

--
-- Table structure for table `borrow_transaction`
--

DROP TABLE IF EXISTS `borrow_transaction`;
CREATE TABLE IF NOT EXISTS `borrow_transaction` (
  `transaction_id` int NOT NULL AUTO_INCREMENT,
  `borrower_id` varchar(20) NOT NULL,
  `custodian_id` varchar(20) NOT NULL,
  `class_id` varchar(20) DEFAULT NULL,
  `request_id` int DEFAULT NULL,
  `borrow_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expected_return` date NOT NULL,
  `return_date` datetime DEFAULT NULL,
  `return_custodian` varchar(20) DEFAULT NULL,
  `transaction_status` enum('BORROWED','RETURNED','RETURNED_WITH_ISSUE','OVERDUE') NOT NULL DEFAULT 'BORROWED',
  PRIMARY KEY (`transaction_id`),
  KEY `fk_trans_borrower` (`borrower_id`),
  KEY `fk_trans_custodian` (`custodian_id`),
  KEY `fk_trans_class` (`class_id`),
  KEY `fk_trans_request` (`request_id`),
  KEY `fk_trans_ret_custodian` (`return_custodian`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `borrow_transaction`
--

INSERT INTO `borrow_transaction` (`transaction_id`, `borrower_id`, `custodian_id`, `class_id`, `request_id`, `borrow_date`, `expected_return`, `return_date`, `return_custodian`, `transaction_status`) VALUES
(1, '2021-00001', 'STAFF-001', 'IT221L-A', NULL, '2026-04-01 08:00:00', '2026-04-01', '2026-04-01 10:30:00', 'STAFF-001', 'RETURNED'),
(2, '2021-00002', 'STAFF-001', 'IT221L-A', NULL, '2026-04-01 08:00:00', '2026-04-01', '2026-04-01 10:30:00', 'STAFF-001', 'RETURNED_WITH_ISSUE'),
(3, 'FAC-001', 'STAFF-001', NULL, 1, '2026-03-15 07:00:00', '2026-03-15', '2026-03-15 17:00:00', 'STAFF-001', 'RETURNED'),
(4, '2021-00003', 'STAFF-001', 'IT221L-A', NULL, '2026-04-03 08:00:00', '2026-04-03', '2026-05-13 07:03:25', 'STAFF-001', 'RETURNED'),
(5, '2021-00006', 'STAFF-001', 'IT221L-B', NULL, '2026-04-05 09:00:00', '2026-04-05', NULL, NULL, 'OVERDUE'),
(6, 'FAC-002', 'STAFF-001', NULL, 3, '2026-04-08 08:30:00', '2026-04-08', NULL, NULL, 'OVERDUE'),
(7, '2021-00001', 'STAFF-001', NULL, 2, '2026-04-07 09:00:00', '2026-04-10', '2026-05-13 07:04:13', 'STAFF-001', 'RETURNED_WITH_ISSUE'),
(8, '2021-00001', 'STAFF-001', NULL, NULL, '2026-05-13 06:27:58', '2026-05-14', NULL, NULL, 'BORROWED'),
(9, '2021-00001', 'STAFF-001', NULL, NULL, '2026-05-13 07:02:36', '2026-05-13', NULL, NULL, 'BORROWED');

-- --------------------------------------------------------

--
-- Table structure for table `class_enrollment`
--

DROP TABLE IF EXISTS `class_enrollment`;
CREATE TABLE IF NOT EXISTS `class_enrollment` (
  `class_id` varchar(20) NOT NULL,
  `student_id` varchar(20) NOT NULL,
  PRIMARY KEY (`class_id`,`student_id`),
  KEY `fk_enroll_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `class_enrollment`
--

INSERT INTO `class_enrollment` (`class_id`, `student_id`) VALUES
('CS201L-A', '2021-00001'),
('IT221L-A', '2021-00001'),
('IT221L-A', '2021-00002'),
('CS201L-A', '2021-00003'),
('IT221L-A', '2021-00003'),
('IT221L-A', '2021-00004'),
('IT221L-A', '2021-00005'),
('IT221L-B', '2021-00006'),
('IT221L-B', '2021-00007'),
('IT221L-B', '2021-00008'),
('IT211L-A', '2021-00009'),
('IT211L-A', '2021-00010');

-- --------------------------------------------------------

--
-- Table structure for table `equipment`
--

DROP TABLE IF EXISTS `equipment`;
CREATE TABLE IF NOT EXISTS `equipment` (
  `barcode` varchar(30) NOT NULL,
  `item_name` varchar(100) NOT NULL,
  `category` enum('EQUIPMENT','PERIPHERAL','ACCESSORY') NOT NULL,
  `brand` varchar(50) DEFAULT NULL,
  `model` varchar(50) DEFAULT NULL,
  `status` enum('AVAILABLE','BORROWED','DAMAGED','DECOMMISSIONED') NOT NULL DEFAULT 'AVAILABLE',
  `acquisition_date` date DEFAULT NULL,
  `remarks` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`barcode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `equipment`
--

INSERT INTO `equipment` (`barcode`, `item_name`, `category`, `brand`, `model`, `status`, `acquisition_date`, `remarks`) VALUES
('EQ-0001', 'Laptop', 'EQUIPMENT', 'Dell', 'Inspiron 15', 'AVAILABLE', '2022-06-01', NULL),
('EQ-0002', 'Laptop', 'EQUIPMENT', 'Dell', 'Inspiron 15', 'AVAILABLE', '2022-06-01', NULL),
('EQ-0003', 'Laptop', 'EQUIPMENT', 'HP', 'ProBook 450', 'AVAILABLE', '2022-06-01', NULL),
('EQ-0004', 'Projector', 'EQUIPMENT', 'Epson', 'EB-X41', 'AVAILABLE', '2021-01-15', NULL),
('EQ-0005', 'Projector', 'EQUIPMENT', 'Epson', 'EB-X41', 'DAMAGED', '2021-01-15', 'Lamp defective'),
('EQ-0006', 'Wireless Mouse', 'PERIPHERAL', 'Logitech', 'M305', 'AVAILABLE', '2023-03-10', NULL),
('EQ-0007', 'Wireless Mouse', 'PERIPHERAL', 'Logitech', 'M305', 'BORROWED', '2023-03-10', NULL),
('EQ-0008', 'Wireless Mouse', 'PERIPHERAL', 'Logitech', 'M305', 'AVAILABLE', '2023-03-10', NULL),
('EQ-0009', 'HDMI Cable', 'ACCESSORY', 'Generic', '1.5m', 'AVAILABLE', '2022-09-01', NULL),
('EQ-0010', 'HDMI Cable', 'ACCESSORY', 'Generic', '1.5m', 'AVAILABLE', '2022-09-01', NULL),
('EQ-0011', 'USB Hub', 'PERIPHERAL', 'Anker', '7-Port', 'AVAILABLE', '2023-01-20', NULL),
('EQ-0012', 'USB Hub', 'PERIPHERAL', 'Anker', '7-Port', 'BORROWED', '2023-01-20', NULL),
('EQ-0013', 'Extension Cord', 'ACCESSORY', 'Meralco', '3-Gang 5m', 'AVAILABLE', '2021-11-05', NULL),
('EQ-0014', 'Webcam', 'PERIPHERAL', 'Logitech', 'C920', 'AVAILABLE', '2022-07-15', NULL),
('EQ-0015', 'Webcam', 'PERIPHERAL', 'Logitech', 'C920', 'AVAILABLE', '2022-07-15', NULL),
('EQ-0016', 'Network Switch', 'EQUIPMENT', 'TP-Link', 'TL-SG108', 'AVAILABLE', '2020-05-30', NULL),
('EQ-0017', 'Ethernet Cable', 'ACCESSORY', 'Generic', 'Cat6 5m', 'AVAILABLE', '2022-01-10', NULL),
('EQ-0018', 'Ethernet Cable', 'ACCESSORY', 'Generic', 'Cat6 5m', 'AVAILABLE', '2022-01-10', NULL),
('EQ-0019', 'Keyboard', 'PERIPHERAL', 'Dell', 'KB216', 'DAMAGED', '2023-02-14', NULL),
('EQ-0020', 'Keyboard', 'PERIPHERAL', 'Dell', 'KB216', 'DAMAGED', '2023-02-14', 'Several keys not working'),
('EQ-0021', 'Wireless Mouse', 'EQUIPMENT', 'Logitech', 'Superlight', 'DECOMMISSIONED', '2022-07-16', 'Spare parts');

-- --------------------------------------------------------

--
-- Table structure for table `lab_class`
--

DROP TABLE IF EXISTS `lab_class`;
CREATE TABLE IF NOT EXISTS `lab_class` (
  `class_id` varchar(20) NOT NULL,
  `course_code` varchar(20) NOT NULL,
  `section` varchar(10) NOT NULL,
  `semester` varchar(20) NOT NULL,
  `school_year` varchar(10) NOT NULL,
  `room` varchar(20) DEFAULT NULL,
  `schedule` varchar(50) DEFAULT NULL,
  `faculty_id` varchar(20) NOT NULL,
  PRIMARY KEY (`class_id`),
  KEY `fk_class_faculty` (`faculty_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `lab_class`
--

INSERT INTO `lab_class` (`class_id`, `course_code`, `section`, `semester`, `school_year`, `room`, `schedule`, `faculty_id`) VALUES
('CS201L-A', 'CS 201L', 'A', '2nd Semester', '2025-2026', 'CIS Lab 3', 'TTH 1:00-2:30', 'FAC-003'),
('IT211L-A', 'IT 211L', 'A', '2nd Semester', '2025-2026', 'CIS Lab 2', 'TTH 10:30-12:00', 'FAC-002'),
('IT221L-A', 'IT 221L', 'A', '2nd Semester', '2025-2026', 'CIS Lab 1', 'MWF 7:30-9:00', 'FAC-001'),
('IT221L-B', 'IT 221L', 'B', '2nd Semester', '2025-2026', 'CIS Lab 1', 'MWF 9:00-10:30', 'FAC-001');

-- --------------------------------------------------------

--
-- Table structure for table `person`
--

DROP TABLE IF EXISTS `person`;
CREATE TABLE IF NOT EXISTS `person` (
  `person_id` varchar(20) NOT NULL,
  `last_name` varchar(50) NOT NULL,
  `first_name` varchar(50) NOT NULL,
  `middle_name` varchar(50) DEFAULT NULL,
  `person_type` enum('STUDENT','FACULTY','STAFF') NOT NULL,
  `email` varchar(100) DEFAULT NULL,
  `contact_no` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`person_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `person`
--

INSERT INTO `person` (`person_id`, `last_name`, `first_name`, `middle_name`, `person_type`, `email`, `contact_no`) VALUES
('2021-00001', 'Santos', 'Maria', 'Cruz', 'STUDENT', 'santos.mc@slu.edu.ph', '09171234501'),
('2021-00002', 'Reyes', 'John', 'dela', 'STUDENT', 'reyes.jd@slu.edu.ph', '09171234502'),
('2021-00003', 'Garcia', 'Ana', 'Lopez', 'STUDENT', 'garcia.al@slu.edu.ph', '09171234503'),
('2021-00004', 'Mendoza', 'Carlos', 'Bautista', 'STUDENT', 'mendoza.cb@slu.edu.ph', '09171234504'),
('2021-00005', 'Flores', 'Liza', 'Tan', 'STUDENT', 'flores.lt@slu.edu.ph', '09171234505'),
('2021-00006', 'Torres', 'Rico', 'Aquino', 'STUDENT', 'torres.ra@slu.edu.ph', '09171234506'),
('2021-00007', 'Villanueva', 'Grace', 'Abad', 'STUDENT', 'villanueva.ga@slu.edu.ph', '09171234507'),
('2021-00008', 'Cruz', 'Mark', 'Rivera', 'STUDENT', 'cruz.mr@slu.edu.ph', '09171234508'),
('2021-00009', 'Ramos', 'Joy', 'Ocampo', 'STUDENT', 'ramos.jo@slu.edu.ph', '09171234509'),
('2021-00010', 'Navarro', 'Ben', 'Salazar', 'STUDENT', 'navarro.bs@slu.edu.ph', '09171234510'),
('FAC-001', 'Domingo', 'Rafael', 'Ignacio', 'FACULTY', 'domingo.ri@slu.edu.ph', '09181234501'),
('FAC-002', 'Castillo', 'Elena', 'Morales', 'FACULTY', 'castillo.em@slu.edu.ph', '09181234502'),
('FAC-003', 'Padilla', 'Jerome', 'Lim', 'FACULTY', 'padilla.jl@slu.edu.ph', '09181234503'),
('STAFF-001', 'Hernandez', 'Miguel', 'Santos', 'STAFF', 'hernandez.ms@slu.edu.ph', '09191234501'),
('STAFF-002', 'Aguilar', 'Rose', 'Valdez', 'STAFF', 'aguilar.rv@slu.edu.ph', '09191234502');

-- --------------------------------------------------------

--
-- Table structure for table `user_account`
--

DROP TABLE IF EXISTS `user_account`;
CREATE TABLE IF NOT EXISTS `user_account` (
  `account_id` int NOT NULL AUTO_INCREMENT,
  `person_id` varchar(20) NOT NULL,
  `username` varchar(50) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `role` enum('ADMIN','CUSTODIAN','BORROWER') NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`account_id`),
  UNIQUE KEY `username` (`username`),
  KEY `fk_account_person` (`person_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `user_account`
--

INSERT INTO `user_account` (`account_id`, `person_id`, `username`, `password_hash`, `role`, `is_active`, `created_at`) VALUES
(1, 'STAFF-001', 'custodian1', 'test', 'CUSTODIAN', 1, '2026-04-07 23:57:25'),
(2, 'STAFF-002', 'admin1', 'admin123', 'ADMIN', 1, '2026-04-07 23:57:25'),
(3, '2021-00001', 'santos_mc', '123', 'BORROWER', 1, '2026-04-07 23:57:25'),
(4, '2021-00002', 'reyes_jd', 'hashed_pass_4', 'BORROWER', 1, '2026-04-07 23:57:25'),
(5, '2021-00003', 'garcia_al', 'hashed_pass_5', 'BORROWER', 1, '2026-04-07 23:57:25'),
(6, 'FAC-001', 'domingo_ri', 'hashed_pass_6', 'BORROWER', 1, '2026-04-07 23:57:25'),
(7, 'FAC-002', 'castillo_em', 'hashed_pass_7', 'BORROWER', 1, '2026-04-07 23:57:25');

-- --------------------------------------------------------

--
-- Stand-in structure for view `vw_active_borrows`
-- (See below for the actual view)
--
DROP VIEW IF EXISTS `vw_active_borrows`;
CREATE TABLE IF NOT EXISTS `vw_active_borrows` (
`transaction_id` int
,`borrower_id` varchar(20)
,`borrower_name` varchar(101)
,`person_type` enum('STUDENT','FACULTY','STAFF')
,`barcode` varchar(30)
,`item_name` varchar(100)
,`brand` varchar(50)
,`category` enum('EQUIPMENT','PERIPHERAL','ACCESSORY')
,`borrow_date` datetime
,`expected_return` date
,`transaction_status` enum('BORROWED','RETURNED','RETURNED_WITH_ISSUE','OVERDUE')
);

-- --------------------------------------------------------

--
-- Stand-in structure for view `vw_equipment_inventory`
-- (See below for the actual view)
--
DROP VIEW IF EXISTS `vw_equipment_inventory`;
CREATE TABLE IF NOT EXISTS `vw_equipment_inventory` (
`category` enum('EQUIPMENT','PERIPHERAL','ACCESSORY')
,`item_name` varchar(100)
,`brand` varchar(50)
,`total_units` bigint
,`available` decimal(23,0)
,`borrowed` decimal(23,0)
,`damaged` decimal(23,0)
,`decommissioned` decimal(23,0)
);

-- --------------------------------------------------------

--
-- Stand-in structure for view `vw_overdue_report`
-- (See below for the actual view)
--
DROP VIEW IF EXISTS `vw_overdue_report`;
CREATE TABLE IF NOT EXISTS `vw_overdue_report` (
`transaction_id` int
,`first_name` varchar(50)
,`last_name` varchar(50)
,`borrower_name` varchar(102)
,`person_type` enum('STUDENT','FACULTY','STAFF')
,`email` varchar(100)
,`contact_no` varchar(20)
,`item_name` varchar(100)
,`barcode` varchar(30)
,`category` enum('EQUIPMENT','PERIPHERAL','ACCESSORY')
,`expected_return` date
,`borrow_date` datetime
,`transaction_status` enum('BORROWED','RETURNED','RETURNED_WITH_ISSUE','OVERDUE')
);

-- --------------------------------------------------------

--
-- Stand-in structure for view `vw_student_class_details`
-- (See below for the actual view)
--
DROP VIEW IF EXISTS `vw_student_class_details`;
CREATE TABLE IF NOT EXISTS `vw_student_class_details` (
`student_id` varchar(20)
,`student_name` varchar(101)
,`class_id` varchar(20)
,`course_code` varchar(20)
,`section` varchar(10)
,`semester` varchar(20)
,`school_year` varchar(10)
,`schedule` varchar(50)
,`room` varchar(20)
,`faculty_name` varchar(101)
);

-- --------------------------------------------------------

--
-- Structure for view `vw_active_borrows`
--
DROP TABLE IF EXISTS `vw_active_borrows`;

DROP VIEW IF EXISTS `vw_active_borrows`;
CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `vw_active_borrows`  AS SELECT `bt`.`transaction_id` AS `transaction_id`, `bt`.`borrower_id` AS `borrower_id`, concat(`p`.`first_name`,' ',`p`.`last_name`) AS `borrower_name`, `p`.`person_type` AS `person_type`, `e`.`barcode` AS `barcode`, `e`.`item_name` AS `item_name`, `e`.`brand` AS `brand`, `e`.`category` AS `category`, `bt`.`borrow_date` AS `borrow_date`, `bt`.`expected_return` AS `expected_return`, `bt`.`transaction_status` AS `transaction_status` FROM (((`borrow_transaction` `bt` join `person` `p` on((`bt`.`borrower_id` = `p`.`person_id`))) join `borrow_item` `bi` on((`bt`.`transaction_id` = `bi`.`transaction_id`))) join `equipment` `e` on((`bi`.`barcode` = `e`.`barcode`))) WHERE (`bt`.`transaction_status` in ('BORROWED','OVERDUE')) ;

-- --------------------------------------------------------

--
-- Structure for view `vw_equipment_inventory`
--
DROP TABLE IF EXISTS `vw_equipment_inventory`;

DROP VIEW IF EXISTS `vw_equipment_inventory`;
CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `vw_equipment_inventory`  AS SELECT `equipment`.`category` AS `category`, `equipment`.`item_name` AS `item_name`, `equipment`.`brand` AS `brand`, count(0) AS `total_units`, sum((case when (`equipment`.`status` = 'AVAILABLE') then 1 else 0 end)) AS `available`, sum((case when (`equipment`.`status` = 'BORROWED') then 1 else 0 end)) AS `borrowed`, sum((case when (`equipment`.`status` = 'DAMAGED') then 1 else 0 end)) AS `damaged`, sum((case when (`equipment`.`status` = 'DECOMMISSIONED ') then 1 else 0 end)) AS `decommissioned` FROM `equipment` GROUP BY `equipment`.`category`, `equipment`.`item_name`, `equipment`.`brand` ;

-- --------------------------------------------------------

--
-- Structure for view `vw_overdue_report`
--
DROP TABLE IF EXISTS `vw_overdue_report`;

DROP VIEW IF EXISTS `vw_overdue_report`;
CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `vw_overdue_report`  AS SELECT `bt`.`transaction_id` AS `transaction_id`, `p`.`first_name` AS `first_name`, `p`.`last_name` AS `last_name`, concat(`p`.`last_name`,', ',`p`.`first_name`) AS `borrower_name`, `p`.`person_type` AS `person_type`, `p`.`email` AS `email`, `p`.`contact_no` AS `contact_no`, `e`.`item_name` AS `item_name`, `e`.`barcode` AS `barcode`, `e`.`category` AS `category`, `bt`.`expected_return` AS `expected_return`, `bt`.`borrow_date` AS `borrow_date`, `bt`.`transaction_status` AS `transaction_status` FROM (((`borrow_transaction` `bt` join `person` `p` on((`bt`.`borrower_id` = `p`.`person_id`))) join `borrow_item` `bi` on((`bt`.`transaction_id` = `bi`.`transaction_id`))) join `equipment` `e` on((`bi`.`barcode` = `e`.`barcode`))) WHERE (`bt`.`transaction_status` in ('OVERDUE','RETURNED_WITH_ISSUE')) ORDER BY `bt`.`transaction_status` ASC, `bt`.`expected_return` ASC ;

-- --------------------------------------------------------

--
-- Structure for view `vw_student_class_details`
--
DROP TABLE IF EXISTS `vw_student_class_details`;

DROP VIEW IF EXISTS `vw_student_class_details`;
CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `vw_student_class_details`  AS SELECT `ce`.`student_id` AS `student_id`, concat(`s`.`first_name`,' ',`s`.`last_name`) AS `student_name`, `lc`.`class_id` AS `class_id`, `lc`.`course_code` AS `course_code`, `lc`.`section` AS `section`, `lc`.`semester` AS `semester`, `lc`.`school_year` AS `school_year`, `lc`.`schedule` AS `schedule`, `lc`.`room` AS `room`, concat(`f`.`first_name`,' ',`f`.`last_name`) AS `faculty_name` FROM (((`class_enrollment` `ce` join `person` `s` on((`ce`.`student_id` = `s`.`person_id`))) join `lab_class` `lc` on((`ce`.`class_id` = `lc`.`class_id`))) join `person` `f` on((`lc`.`faculty_id` = `f`.`person_id`))) ;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `activity_request`
--
ALTER TABLE `activity_request`
  ADD CONSTRAINT `fk_request_approver` FOREIGN KEY (`approved_by`) REFERENCES `person` (`person_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_request_requester` FOREIGN KEY (`requested_by`) REFERENCES `person` (`person_id`) ON DELETE RESTRICT ON UPDATE CASCADE;

--
-- Constraints for table `borrow_item`
--
ALTER TABLE `borrow_item`
  ADD CONSTRAINT `fk_bi_equipment` FOREIGN KEY (`barcode`) REFERENCES `equipment` (`barcode`) ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_bi_transaction` FOREIGN KEY (`transaction_id`) REFERENCES `borrow_transaction` (`transaction_id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `borrow_transaction`
--
ALTER TABLE `borrow_transaction`
  ADD CONSTRAINT `fk_trans_borrower` FOREIGN KEY (`borrower_id`) REFERENCES `person` (`person_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_trans_class` FOREIGN KEY (`class_id`) REFERENCES `lab_class` (`class_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_trans_custodian` FOREIGN KEY (`custodian_id`) REFERENCES `person` (`person_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_trans_request` FOREIGN KEY (`request_id`) REFERENCES `activity_request` (`request_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_trans_ret_custodian` FOREIGN KEY (`return_custodian`) REFERENCES `person` (`person_id`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Constraints for table `class_enrollment`
--
ALTER TABLE `class_enrollment`
  ADD CONSTRAINT `fk_enroll_class` FOREIGN KEY (`class_id`) REFERENCES `lab_class` (`class_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_enroll_student` FOREIGN KEY (`student_id`) REFERENCES `person` (`person_id`) ON DELETE RESTRICT ON UPDATE CASCADE;

--
-- Constraints for table `lab_class`
--
ALTER TABLE `lab_class`
  ADD CONSTRAINT `fk_class_faculty` FOREIGN KEY (`faculty_id`) REFERENCES `person` (`person_id`) ON DELETE RESTRICT ON UPDATE CASCADE;

--
-- Constraints for table `user_account`
--
ALTER TABLE `user_account`
  ADD CONSTRAINT `fk_account_person` FOREIGN KEY (`person_id`) REFERENCES `person` (`person_id`) ON DELETE RESTRICT ON UPDATE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
