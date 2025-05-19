package billing.management.system;

import java.io.*;
import java.util.*;

public class FileManager {
    private static final String USERS_FILE = "users.txt";
    private static final String BILLS_FILE = "bills.txt";
    private static List<User> cachedUsers = null;
    private static List<Bill> cachedBills = null;
    private static int lastBillId = 0;

    public static void saveUsers(List<User> users) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE))) {
            for (User user : users) {
                writer.write(user.toDataString());
                writer.newLine();
            }
            cachedUsers = new ArrayList<>(users);
            System.out.println("Users saved to " + USERS_FILE);
        } catch (IOException e) {
            System.err.println("Error saving users: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static List<User> loadUsers() {
        if (cachedUsers != null) {
            System.out.println("Returning cached users, count: " + cachedUsers.size());
            return new ArrayList<>(cachedUsers);
        }
        List<User> users = new ArrayList<>();
        File file = new File(USERS_FILE);
        if (!file.exists()) {
            System.out.println("Users file not found, creating new: " + USERS_FILE);
            return users;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                String type = parts[0];
                if ((type.equals("Admin") && parts.length != 5) ||
                    (type.equals("Company") && parts.length != 6) ||
                    (type.equals("Customer") && parts.length != 6)) {
                    System.err.println("Skipping invalid user line (incorrect field count for " + type + "): " + line);
                    continue;
                }

                try {
                    String name = parts[1];
                    String password = parts[2];
                    int id = Integer.parseInt(parts[3]);
                    String email = parts[4];

                    switch (type) {
                        case "Admin":
                            users.add(new Admin(name, password, id, email));
                            break;
                        case "Company":
                            String industry = parts[5];
                            users.add(new Company(name, password, id, email, industry));
                            break;
                        case "Customer":
                            long startDateMillis = Long.parseLong(parts[5]);
                            Date startDate = new Date(startDateMillis);
                            users.add(new Customer(name, password, id, email, startDate));
                            break;
                        default:
                            System.err.println("Skipping line with unknown user type: " + line);
                    }
                } catch (Exception e) {
                    System.err.println("Skipping invalid user line: " + line + ", Error: " + e.getMessage());
                }
            }
            cachedUsers = new ArrayList<>(users);
            System.out.println("Loaded " + users.size() + " users from " + USERS_FILE);
        } catch (IOException e) {
            System.err.println("Error reading users: " + e.getMessage());
            e.printStackTrace();
        }
        return users;
    }

    public static void saveBills(List<Bill> bills) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(BILLS_FILE))) {
            for (Bill bill : bills) {
                try {
                    if (bill.getDueDate() == null) {
                        System.err.println("Null due date detected for bill ID " + bill.getBillId() + "; skipping write");
                        continue;
                    }
                    writer.write(bill.toDataString());
                    writer.newLine();
                } catch (Exception e) {
                    System.err.println("Failed to write bill ID " + bill.getBillId() + ": " + e.getMessage());
                }
            }
            cachedBills = new ArrayList<>(bills);
            System.out.println("Bills saved to " + BILLS_FILE + ", count: " + bills.size());
        } catch (IOException e) {
            System.err.println("Error saving bills: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to save bills: " + e.getMessage());
        }
    }

    public static void saveBill(Bill bill) {
        if (bill == null || bill.getDate() == null || bill.getDueDate() == null) {
            System.err.println("Invalid bill data for ID " + (bill != null ? bill.getBillId() : "null") + ": null fields detected");
            throw new IllegalArgumentException("Bill or its date fields cannot be null");
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(BILLS_FILE, true))) {
            String billData = bill.toDataString();
            writer.write(billData);
            writer.newLine();
            if (cachedBills != null) {
                cachedBills.add(bill);
            } else {
                cachedBills = new ArrayList<>();
                cachedBills.add(bill);
            }
            if (bill.getBillId() > lastBillId) {
                lastBillId = bill.getBillId();
            }
            System.out.println("Bill ID " + bill.getBillId() + " appended to " + BILLS_FILE + ": " + billData);
            cachedBills = null; // Force reload on next loadBills
        } catch (IOException e) {
            System.err.println("Error appending bill ID " + bill.getBillId() + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to save bill: " + e.getMessage());
        }
    }

    public static List<Bill> loadBills() {
        if (cachedBills != null) {
            System.out.println("Returning cached bills, count: " + cachedBills.size());
            return new ArrayList<>(cachedBills);
        }
        List<Bill> bills = new ArrayList<>();
        File file = new File(BILLS_FILE);
        if (!file.exists()) {
            System.out.println("Bills file not found, creating new: " + BILLS_FILE);
            return bills;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    System.err.println("Skipping empty line at line " + lineNumber);
                    continue;
                }
                String[] parts = line.split("\\|");
                if (parts.length != 9) {
                    System.err.println("Skipping invalid bill line at line " + lineNumber + " (incorrect field count): " + line);
                    continue;
                }
                try {
                    int billId = Integer.parseInt(parts[0].trim());
                    double value = Double.parseDouble(parts[1].trim());
                    int customerId = Integer.parseInt(parts[2].trim());
                    int companyId = Integer.parseInt(parts[3].trim());
                    String customerName = parts[4].trim();
                    String companyName = parts[5].trim();
                    long dateMillis = Long.parseLong(parts[6].trim());
                    String dueDateStr = parts[7].trim();
                    boolean isPaid = Boolean.parseBoolean(parts[8].trim());

                    Date date = new Date(dateMillis);
                    Date dueDate;
                    if (dueDateStr.isEmpty()) {
                        System.err.println("Empty due date for bill ID " + billId + "; skipping bill");
                        continue;
                    } else {
                        long dueDateMillis = Long.parseLong(dueDateStr);
                        dueDate = new Date(dueDateMillis);
                        if (dueDateMillis <= 0) {
                            System.err.println("Invalid due date timestamp " + dueDateMillis + " for bill ID " + billId + "; skipping bill");
                            continue;
                        }
                    }
                    bills.add(new Bill(billId, value, customerId, companyId, customerName, companyName, date, dueDate, isPaid));
                    if (billId > lastBillId) lastBillId = billId;
                    System.out.println("Loaded bill ID " + billId + ": date=" + date + " (" + dateMillis + "ms), dueDate=" + dueDate + " (" + dueDateStr + "ms), isPaid=" + isPaid);
                } catch (Exception e) {
                    System.err.println("Skipping invalid bill line at line " + lineNumber + ": " + line + ", Error: " + e.getMessage());
                }
            }
            cachedBills = new ArrayList<>(bills);
            System.out.println("Loaded " + bills.size() + " bills from " + BILLS_FILE);
        } catch (IOException e) {
            System.err.println("Error loading bills: " + e.getMessage());
            e.printStackTrace();
        }
        return bills;
    }

    public static List<Bill> loadBillsForUser(int userId) {
        List<Bill> userBills = new ArrayList<>();
        for (Bill bill : loadBills()) {
            if (bill.getCompanyId() == userId || bill.getCustomerId() == userId) {
                userBills.add(bill);
            }
        }
        System.out.println("Loaded " + userBills.size() + " bills for user ID " + userId);
        return userBills;
    }
    
    public static void deleteUser(int userId, List<User> users) {
        User userToDelete = users.stream()
                .filter(user -> user.getId() == userId)
                .findFirst()
                .orElse(null);

        if (userToDelete == null) {
            System.err.println("User with ID " + userId + " not found.");
            return;
        }

        users.removeIf(user -> user.getId() == userId);
        saveUsers(users);

        if (userToDelete instanceof Company || userToDelete instanceof Customer) {
            List<Bill> bills = loadBills();
            int initialSize = bills.size();
            bills.removeIf(bill -> bill.getCompanyId() == userId || bill.getCustomerId() == userId);
            int removedBills = initialSize - bills.size();
            if (removedBills > 0) {
                System.out.println("Deleted " + removedBills + " bills associated with user ID " + userId);
            }
            saveBills(bills);
        } else {
            System.out.println("No bills deleted for Admin user ID " + userId);
        }
    }

    public static void deleteBills(List<Integer> billIds) {
        try {
            List<Bill> bills = loadBills();
            int initialSize = bills.size();
            bills.removeIf(bill -> billIds.contains(bill.getBillId()));
            int removedBills = initialSize - bills.size();
            saveBills(bills);
            System.out.println("Deleted " + removedBills + " bills with IDs: " + billIds);
        } catch (Exception e) {
            System.err.println("Error deleting bills: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to delete bills: " + e.getMessage());
        }
    }

    public static User findUserById(int userId) {
        for (User user : loadUsers()) {
            if (user.getId() == userId) return user;
        }
        return null;
    }

    public static Bill findBillById(int billId) {
        for (Bill bill : loadBills()) {
            if (bill.getBillId() == billId) return bill;
        }
        return null;
    }

    public static int generateBillId() {
        int newId = lastBillId + 1;
        lastBillId = newId;
        return newId;
    }

    public static int generateUserId() {
        List<User> users = loadUsers();
        if (users.isEmpty()) return 1;
        return users.stream().mapToInt(User::getId).max().orElse(0) + 1;
    }

    public static void updateUserAndBills(User updatedUser) {
        List<User> users = loadUsers();
        users.removeIf(user -> user.getId() == updatedUser.getId());
        users.add(updatedUser);
        saveUsers(users);

        List<Bill> bills = loadBills();
        for (Bill bill : bills) {
            if (bill.getCompanyId() == updatedUser.getId()) {
                bill.setCompanyName(updatedUser.getName());
            }
            if (bill.getCustomerId() == updatedUser.getId()) {
                bill.setCustomerName(updatedUser.getName());
            }
        }
        saveBills(bills);
    }

    public static void updateBillStatus(int billId, boolean isPaid) {
        List<Bill> bills = loadBills();
        for (Bill bill : bills) {
            if (bill.getBillId() == billId) {
                bill.setIspaid(isPaid);
                break;
            }
        }
        saveBills(bills);
    }
}