package billing.management.system;

import java.util.ArrayList;
import java.util.List;

public class Company extends User {
    private List<Bill> billslist;
    private String industry;

    public Company(String name, String password, int id, String email, String industry) {
        super(name, password, id, email);
        this.industry = industry;
        this.billslist = new ArrayList<>();
        updateBills();
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }
    
    private void updateBills() {
        this.billslist = new ArrayList<>(FileManager.loadBills().stream()
                .filter(bill -> bill.getCompanyId() == this.getId())
                .toList());
        billslist.size();
        getTotalrevenu();
    }


    public int getTotalBills() {
        return billslist.size();
    }

    public double getTotalrevenu() {
        double sum = 0;
        for (Bill bill : billslist) {
            sum += bill.getValue();
        }
        return sum;
    }

    @Override
    public String toDataString() {
        return "Company," + getName() + "," + getPassword() + "," + getId() + "," + getEmail() + "," + getIndustry();
    }
}