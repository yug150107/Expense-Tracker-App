import java.io.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;



class InvalidAmountException extends Exception {
    public InvalidAmountException(String message) {
        super(message);
    }
}

class BudgetExceededException extends Exception {
    public BudgetExceededException(String message) {
        super(message);
    }
}


enum ExpenseCategory {
    FOOD("Food & Dining"),
    TRANSPORT("Transportation"),
    SHOPPING("Shopping"),
    BILLS("Bills & Utilities"),
    ENTERTAINMENT("Entertainment"),
    HEALTHCARE("Healthcare"),
    EDUCATION("Education"),
    OTHER("Other");
    
    private final String displayName;
    
    ExpenseCategory(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

enum PaymentMethod {
    CASH, CARD, UPI, NET_BANKING
}


class Expense {
    private static int expenseCounter = 0;
    private final int expenseId;
    private String description;
    private double amount;
    private ExpenseCategory category;
    private PaymentMethod paymentMethod;
    private LocalDate date;
    
    public Expense(String description, double amount, ExpenseCategory category, 
                   PaymentMethod paymentMethod) throws InvalidAmountException {
        if (amount <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero");
        }
        this.expenseId = ++expenseCounter;
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.paymentMethod = paymentMethod;
        this.date = LocalDate.now();
    }
    
    
    public int getExpenseId() { return expenseId; }
    public String getDescription() { return description; }
    public double getAmount() { return amount; }
    public ExpenseCategory getCategory() { return category; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public LocalDate getDate() { return date; }
    
    
    public void setDescription(String description) { this.description = description; }
    public void setAmount(double amount) throws InvalidAmountException {
        if (amount <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero");
        }
        this.amount = amount;
    }
    
    @Override
    public String toString() {
        return String.format("ID: %d | %s | ₹%.2f | %s | %s | %s", 
            expenseId, date, amount, category.getDisplayName(), 
            paymentMethod, description);
    }
}


class BudgetManager {
    private Map<ExpenseCategory, Double> categoryBudgets;
    private double monthlyBudget;
    
    public BudgetManager(double monthlyBudget) {
        this.monthlyBudget = monthlyBudget;
        this.categoryBudgets = new HashMap<>();
    }
    
    public void setCategoryBudget(ExpenseCategory category, double budget) {
        categoryBudgets.put(category, budget);
    }
    
    public double getCategoryBudget(ExpenseCategory category) {
        return categoryBudgets.getOrDefault(category, 0.0);
    }
    
    public double getMonthlyBudget() {
        return monthlyBudget;
    }
    
    public void checkBudget(ExpenseCategory category, double spent) throws BudgetExceededException {
        double categoryBudget = categoryBudgets.getOrDefault(category, Double.MAX_VALUE);
        if (spent > categoryBudget) {
            throw new BudgetExceededException(
                String.format("Budget exceeded for %s! Budget: ₹%.2f, Spent: ₹%.2f", 
                category.getDisplayName(), categoryBudget, spent));
        }
    }
}


class ExpenseRepository {
    private List<Expense> expenses;
    private Stack<String> operationHistory;
    
    public ExpenseRepository() {
        this.expenses = new ArrayList<>();
        this.operationHistory = new Stack<>();
    }
    
    public synchronized void addExpense(Expense expense) {
        expenses.add(expense);
        operationHistory.push("Added: " + expense.getDescription() + " - ₹" + expense.getAmount());
    }
    
    public synchronized boolean deleteExpense(int expenseId) {
        for (Expense exp : expenses) {
            if (exp.getExpenseId() == expenseId) {
                expenses.remove(exp);
                operationHistory.push("Deleted: " + exp.getDescription());
                return true;
            }
        }
        return false;
    }
    
    public List<Expense> getAllExpenses() {
        return new ArrayList<>(expenses);
    }
    
    public List<Expense> getExpensesByCategory(ExpenseCategory category) {
        List<Expense> result = new ArrayList<>();
        for (Expense exp : expenses) {
            if (exp.getCategory() == category) {
                result.add(exp);
            }
        }
        return result;
    }
    
    public double getTotalExpenses() {
        double total = 0;
        for (Expense exp : expenses) {
            total += exp.getAmount();
        }
        return total;
    }
    
    public double getCategoryTotal(ExpenseCategory category) {
        double total = 0;
        for (Expense exp : expenses) {
            if (exp.getCategory() == category) {
                total += exp.getAmount();
            }
        }
        return total;
    }
    
    public Map<ExpenseCategory, Double> getCategorySummary() {
        Map<ExpenseCategory, Double> summary = new HashMap<>();
        for (Expense exp : expenses) {
            summary.put(exp.getCategory(), 
                summary.getOrDefault(exp.getCategory(), 0.0) + exp.getAmount());
        }
        return summary;
    }
    
    public void displayHistory() {
        System.out.println("\n=== Operation History ===");
        Stack<String> temp = (Stack<String>) operationHistory.clone();
        int count = 0;
        while (!temp.isEmpty() && count < 10) {
            System.out.println(temp.pop());
            count++;
        }
    }
}


class ExpenseFileHandler {
    
    
    public static void saveToFile(List<Expense> expenses, String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("ExpenseID,Date,Description,Amount,Category,PaymentMethod");
            for (Expense exp : expenses) {
                writer.printf("%d,%s,%s,%.2f,%s,%s%n",
                    exp.getExpenseId(),
                    exp.getDate(),
                    exp.getDescription(),
                    exp.getAmount(),
                    exp.getCategory(),
                    exp.getPaymentMethod());
            }
            System.out.println("✓ Data saved to " + filename);
        } catch (IOException e) {
            System.out.println("✗ Error saving file: " + e.getMessage());
        }
    }
    
    
    public static void generateReport(ExpenseRepository repo, BudgetManager budget, String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("═════════════MONTHLY EXPENSE REPORT══════════════");
            writer.println("\nGenerated: " + LocalDate.now());
            writer.println("\nMonthly Budget: ₹" + budget.getMonthlyBudget());
            writer.println("Total Spent: ₹" + String.format("%.2f", repo.getTotalExpenses()));
            writer.println("Remaining: ₹" + String.format("%.2f", 
                budget.getMonthlyBudget() - repo.getTotalExpenses()));
            
            writer.println("\n--- Category-wise Summary ---");
            Map<ExpenseCategory, Double> summary = repo.getCategorySummary();
            for (Map.Entry<ExpenseCategory, Double> entry : summary.entrySet()) {
                writer.printf("%s: ₹%.2f%n", entry.getKey().getDisplayName(), entry.getValue());
            }
            
            writer.println("\n--- All Expenses ---");
            for (Expense exp : repo.getAllExpenses()) {
                writer.println(exp);
            }
            
            System.out.println("✓ Report generated: " + filename);
        } catch (IOException e) {
            System.out.println("✗ Error generating report: " + e.getMessage());
        }
    }
}


class ExpenseDatabase {
    private static final String DB_URL = "jdbc:sqlite:expenses.db";
    private Connection connection;
    
    public void connect() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            createTable();
            System.out.println("✓ Database connected");
        } catch (SQLException e) {
            System.out.println("✗ Database connection failed: " + e.getMessage());
        }
    }
    
    private void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS expenses (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "description TEXT NOT NULL, " +
                    "amount REAL NOT NULL, " +
                    "category TEXT NOT NULL, " +
                    "payment_method TEXT, " +
                    "date TEXT NOT NULL)";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }
    
    public void saveExpense(Expense expense) {
        String sql = "INSERT INTO expenses (description, amount, category, payment_method, date) " +
                    "VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, expense.getDescription());
            pstmt.setDouble(2, expense.getAmount());
            pstmt.setString(3, expense.getCategory().toString());
            pstmt.setString(4, expense.getPaymentMethod().toString());
            pstmt.setString(5, expense.getDate().toString());
            pstmt.executeUpdate();
            System.out.println("✓ Expense saved to database");
        } catch (SQLException e) {
            System.out.println("✗ Error saving to database: " + e.getMessage());
        }
    }
    
    public void loadExpenses(ExpenseRepository repo) {
        String sql = "SELECT * FROM expenses ORDER BY date DESC";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            int count = 0;
            while (rs.next()) {
                count++;
            }
            System.out.println("✓ Loaded " + count + " expenses from database");
        } catch (SQLException e) {
            System.out.println("✗ Error loading from database: " + e.getMessage());
        }
    }
    
    public void close() {
        try {
            if (connection != null) {
                connection.close();
                System.out.println("✓ Database disconnected");
            }
        } catch (SQLException e) {
            System.out.println("✗ Error closing database: " + e.getMessage());
        }
    }
}

class ReportGeneratorThread extends Thread {
    private ExpenseRepository repository;
    private BudgetManager budgetManager;
    
    public ReportGeneratorThread(ExpenseRepository repository, BudgetManager budgetManager) {
        this.repository = repository;
        this.budgetManager = budgetManager;
    }
    
    @Override
    public void run() {
        System.out.println("\n[Thread " + Thread.currentThread().getName() + 
                         "] Generating report...");
        try {
            Thread.sleep(1000); 
            
            System.out.println("\n═════════════EXPENSE SUMMARY REPORT═══════════");
            
            double total = repository.getTotalExpenses();
            double budget = budgetManager.getMonthlyBudget();
            
            System.out.printf("\nMonthly Budget: ₹%.2f%n", budget);
            System.out.printf("Total Spent:    ₹%.2f%n", total);
            System.out.printf("Remaining:      ₹%.2f%n", budget - total);
            System.out.printf("Spent: %.1f%%%n", (total / budget) * 100);
            
            System.out.println("\n--- Category Breakdown ---");
            Map<ExpenseCategory, Double> summary = repository.getCategorySummary();
            for (Map.Entry<ExpenseCategory, Double> entry : summary.entrySet()) {
                double percentage = (entry.getValue() / total) * 100;
                System.out.printf("%-20s: ₹%8.2f (%5.1f%%)%n", 
                    entry.getKey().getDisplayName(), entry.getValue(), percentage);
            }
            
        } catch (InterruptedException e) {
            System.out.println("Report generation interrupted");
        }
    }
}


public class ExpenseTrackerApp {
    
    private static ExpenseRepository repository = new ExpenseRepository();
    private static BudgetManager budgetManager = new BudgetManager(50000);
    private static ExpenseDatabase database = new ExpenseDatabase();
    private static Scanner scanner = new Scanner(System.in);
    
    public static void main(String[] args) {
        System.out.println("═════════PERSONAL EXPENSE TRACKER══════════");
        database.connect();
        setupBudgets();
        
        addSampleExpenses();
        
        boolean running = true;
        while (running) {
            displayMenu();
            System.out.print("\nEnter choice: ");
            
            try {
                int choice = scanner.nextInt();
                scanner.nextLine(); 
                
                switch (choice) {
                    case 1: addNewExpense(); break;
                    case 2: viewAllExpenses(); break;
                    case 3: viewByCategory(); break;
                    case 4: viewSummary(); break;
                    case 5: generateReportAsync(); break;
                    case 6: saveToFile(); break;
                    case 7: deleteExpense(); break;
                    case 8: repository.displayHistory(); break;
                    case 9: 
                        running = false;
                        System.out.println("\n✓ Thank you for using Expense Tracker!");
                        break;
                    default:
                        System.out.println("✗ Invalid choice!");
                }
            } catch (InputMismatchException e) {
                System.out.println("✗ Invalid input! Please enter a number.");
                scanner.nextLine(); 
            } catch (Exception e) {
                System.out.println("✗ Error: " + e.getMessage());
            }
        }
        
        database.close();
        scanner.close();
    }
    
    private static void displayMenu() {
        System.out.println("\n________________ MENU __________________");
        System.out.println(" |1. Add New Expense                      |");
        System.out.println(" |2. View All Expenses                    |");
        System.out.println(" |3. View by Category                     |");
        System.out.println(" |4. View Summary                         |");
        System.out.println(" |5. Generate Report                      |");
        System.out.println(" |6. Save to File                         |");
        System.out.println(" |7. Delete Expense                       |");
        System.out.println(" |8. View History                         |");
        System.out.println(" |9. Exit                                 |");
        System.out.println(" |________________________________________|");
    }
    
    private static void setupBudgets() {
        budgetManager.setCategoryBudget(ExpenseCategory.FOOD, 10000);
        budgetManager.setCategoryBudget(ExpenseCategory.TRANSPORT, 5000);
        budgetManager.setCategoryBudget(ExpenseCategory.SHOPPING, 8000);
        budgetManager.setCategoryBudget(ExpenseCategory.BILLS, 15000);
        budgetManager.setCategoryBudget(ExpenseCategory.ENTERTAINMENT, 5000);
    }
    
    private static void addSampleExpenses() {
        try {
            repository.addExpense(new Expense("Grocery shopping", 2500, 
                ExpenseCategory.FOOD, PaymentMethod.CARD));
            repository.addExpense(new Expense("Uber ride", 350, 
                ExpenseCategory.TRANSPORT, PaymentMethod.UPI));
            repository.addExpense(new Expense("Movie tickets", 800, 
                ExpenseCategory.ENTERTAINMENT, PaymentMethod.CARD));
            repository.addExpense(new Expense("Electricity bill", 1200, 
                ExpenseCategory.BILLS, PaymentMethod.NET_BANKING));
            repository.addExpense(new Expense("Restaurant dinner", 1500, 
                ExpenseCategory.FOOD, PaymentMethod.CARD));
            System.out.println("✓ Sample expenses loaded");
        } catch (InvalidAmountException e) {
            System.out.println("✗ Error adding sample data: " + e.getMessage());
        }
    }
    
    private static void addNewExpense() {
        try {
            System.out.print("\nDescription: ");
            String desc = scanner.nextLine();
            
            System.out.print("Amount: ₹");
            double amount = scanner.nextDouble();
            scanner.nextLine();
            
            System.out.println("\nCategories:");
            ExpenseCategory[] categories = ExpenseCategory.values();
            for (int i = 0; i < categories.length; i++) {
                System.out.printf("%d. %s%n", i + 1, categories[i].getDisplayName());
            }
            System.out.print("Choose category: ");
            int catChoice = scanner.nextInt() - 1;
            scanner.nextLine();
            
            System.out.println("\nPayment Methods:");
            PaymentMethod[] methods = PaymentMethod.values();
            for (int i = 0; i < methods.length; i++) {
                System.out.printf("%d. %s%n", i + 1, methods[i]);
            }
            System.out.print("Choose payment method: ");
            int methodChoice = scanner.nextInt() - 1;
            scanner.nextLine();
            
            Expense expense = new Expense(desc, amount, categories[catChoice], methods[methodChoice]);
            
            double categorySpent = repository.getCategoryTotal(categories[catChoice]);
            try {
                budgetManager.checkBudget(categories[catChoice], categorySpent + amount);
            } catch (BudgetExceededException e) {
                System.out.println("\n⚠ WARNING: " + e.getMessage());
                System.out.print("Continue anyway? (y/n): ");
                if (!scanner.nextLine().equalsIgnoreCase("y")) {
                    return;
                }
            }
            
            repository.addExpense(expense);
            database.saveExpense(expense);
            System.out.println("\n✓ Expense added successfully!");
            System.out.println(expense);
            
        } catch (InvalidAmountException e) {
            System.out.println("\n✗ " + e.getMessage());
        } catch (Exception e) {
            System.out.println("\n✗ Error: " + e.getMessage());
            scanner.nextLine(); 
        }
    }
    
    private static void viewAllExpenses() {
        List<Expense> expenses = repository.getAllExpenses();
        if (expenses.isEmpty()) {
            System.out.println("\n✗ No expenses found!");
            return;
        }
        
        System.out.println("\n═══════════════ ALL EXPENSES ═══════════════");
        for (Expense exp : expenses) {
            System.out.println(exp);
        }
        System.out.printf("\nTotal: ₹%.2f%n", repository.getTotalExpenses());
    }
    
    private static void viewByCategory() {
        System.out.println("\nSelect Category:");
        ExpenseCategory[] categories = ExpenseCategory.values();
        for (int i = 0; i < categories.length; i++) {
            System.out.printf("%d. %s%n", i + 1, categories[i].getDisplayName());
        }
        System.out.print("Choice: ");
        int choice = scanner.nextInt() - 1;
        scanner.nextLine();
        
        List<Expense> expenses = repository.getExpensesByCategory(categories[choice]);
        System.out.println("\n═══ " + categories[choice].getDisplayName() + " Expenses ═══");
        for (Expense exp : expenses) {
            System.out.println(exp);
        }
        System.out.printf("\nCategory Total: ₹%.2f%n", 
            repository.getCategoryTotal(categories[choice]));
    }
    
    private static void viewSummary() {
        System.out.println("\n═════════════EXPENSE SUMMARY════════════════");
 
        double total = repository.getTotalExpenses();
        System.out.printf("\nTotal Expenses: ₹%.2f%n", total);
        System.out.printf("Monthly Budget: ₹%.2f%n", budgetManager.getMonthlyBudget());
        System.out.printf("Remaining: ₹%.2f%n", budgetManager.getMonthlyBudget() - total);
        
        Map<ExpenseCategory, Double> summary = repository.getCategorySummary();
        System.out.println("\n--- Category-wise Breakdown ---");
        for (Map.Entry<ExpenseCategory, Double> entry : summary.entrySet()) {
            double percentage = (entry.getValue() / total) * 100;
            System.out.printf("%-20s: ₹%8.2f (%5.1f%%)%n", 
                entry.getKey().getDisplayName(), entry.getValue(), percentage);
        }
    }
    
    private static void generateReportAsync() {
        ReportGeneratorThread reportThread = new ReportGeneratorThread(repository, budgetManager);
        reportThread.start();
        
        try {
            reportThread.join(); 
        } catch (InterruptedException e) {
            System.out.println("✗ Report generation interrupted");
        }
    }
    
    private static void saveToFile() {
        String filename = "expense_report_" + LocalDate.now() + ".txt";
        ExpenseFileHandler.saveToFile(repository.getAllExpenses(), "expenses.csv");
        ExpenseFileHandler.generateReport(repository, budgetManager, filename);
    }
    
    private static void deleteExpense() {
        System.out.print("\nEnter Expense ID to delete: ");
        int id = scanner.nextInt();
        scanner.nextLine();
        
        if (repository.deleteExpense(id)) {
            System.out.println("✓ Expense deleted successfully!");
        } else {
            System.out.println("✗ Expense not found!");
        }
    }

}
