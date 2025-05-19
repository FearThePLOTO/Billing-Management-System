package billing.management.system;

import java.util.Date;

public class Bill {
    private int billId;
    private double value;
    private int customerId;
    private int companyId;
    private String customerName;
    private String companyName;
    private Date date;
    private Date dueDate;
    private boolean isPaid;

    public Bill(int billId, double value, int customerId, int companyId, String customerName, String companyName, Date date, Date dueDate, boolean isPaid) {
        this.billId = billId;
        this.value = value;
        this.customerId = customerId;
        this.companyId = companyId;
        this.customerName = customerName;
        this.companyName = companyName;
        this.date = date;
        this.dueDate = dueDate;
        this.isPaid = isPaid;
    }

    public int getBillId() {
        return billId;
    }

    public void setBillId(int billId) {
        this.billId = billId;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public int getCompanyId() {
        return companyId;
    }

    public void setCompanyId(int companyId) {
        this.companyId = companyId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public boolean isIspaid() {
        return isPaid;
    }

    public void setIspaid(boolean isPaid) {
        this.isPaid = isPaid;
    }

    public String toDataString() {
        return billId + "|" + value + "|" + customerId + "|" + companyId + "|" + customerName + "|" + companyName + "|" + date.getTime() + "|" + (dueDate != null ? dueDate.getTime() : "") + "|" + isPaid;
    }
}