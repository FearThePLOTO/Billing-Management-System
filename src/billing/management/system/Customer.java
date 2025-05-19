package billing.management.system;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Customer extends User {
    private List<Bill> customerBills;
    private Date startDate;

    public Customer(String name, String password, int id, String email, Date startDate) {
        super(name, password, id, email);
        this.startDate = startDate;
        this.customerBills = new ArrayList<>();
        updateBills();
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    private void updateBills() {
        this.customerBills = new ArrayList<>(FileManager.loadBills().stream()
                .filter(bill -> bill.getCustomerId() == this.getId())
                .toList());
        customerBills.size();
        getTotalrevenu();
    }

    public int getTotalBills() {
        return customerBills.size();
    }

    public double getTotalrevenu() {
        double sum = 0;
        for (Bill bill : customerBills) {
            if (bill.isIspaid()) {
                sum += bill.getValue();
            }
        }
        return sum;
    }

    @Override
    public String toDataString() {
        return "Customer," + getName() + "," + getPassword() + "," + getId() + "," + getEmail() + "," + startDate.getTime();
    }
}