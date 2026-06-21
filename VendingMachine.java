import java.util.HashMap;
import java.util.Map;

// ---------- Coin ----------
enum Coin {
PENNY(1), NICKEL(5), DIME(10), QUARTER(25);


private final int value; // in cents

Coin(int value) {
    this.value = value;
}

public int getValue() {
    return value;
}

}

// ---------- Product ----------
class Product {
private final String code;
private final String name;
private final int price; // in cents


public Product(String code, String name, int price) {
    this.code = code;
    this.name = name;
    this.price = price;
}

public String getCode() { return code; }
public String getName() { return name; }
public int getPrice() { return price; }

}

// ---------- Inventory ----------
class Inventory {
private final Map<String, Product> products = new HashMap<>();
private final Map<String, Integer> quantities = new HashMap<>();

public void addProduct(Product product, int quantity) {
    products.put(product.getCode(), product);
    quantities.put(product.getCode(), quantity);
}

public boolean hasProduct(String code) {
    return products.containsKey(code) && quantities.getOrDefault(code, 0) > 0;
}

public Product getProduct(String code) {
    return products.get(code);
}

public void reduceStock(String code) {
    quantities.put(code, quantities.get(code) - 1);
}

public boolean isEmpty() {
    return quantities.values().stream().allMatch(q -> q == 0);
}


}

// ---------- State interface ----------
interface VendingMachineState {
void insertCoin(Coin coin);
void selectProduct(String code);
void dispense();
void refund();
}

// ---------- Concrete states ----------
class IdleState implements VendingMachineState {
private final VendingMachine machine;

public IdleState(VendingMachine machine) {
    this.machine = machine;
}

public void insertCoin(Coin coin) {
    machine.addBalance(coin.getValue());
    System.out.println("Coin inserted. Balance: " + machine.getBalance() + " cents");
    machine.setState(machine.getHasMoneyState());
}

public void selectProduct(String code) {
    System.out.println("Insert coin first.");
}

public void dispense() {
    System.out.println("Insert coin and select a product first.");
}

public void refund() {
    System.out.println("No money inserted yet.");
}

}

class HasMoneyState implements VendingMachineState {
private final VendingMachine machine;

public HasMoneyState(VendingMachine machine) {
    this.machine = machine;
}

public void insertCoin(Coin coin) {
    machine.addBalance(coin.getValue());
    System.out.println("Coin inserted. Balance: " + machine.getBalance() + " cents");
}

public void selectProduct(String code) {
    if (!machine.getInventory().hasProduct(code)) {
        System.out.println("Invalid selection or out of stock.");
        return;
    }
    Product product = machine.getInventory().getProduct(code);
    if (machine.getBalance() < product.getPrice()) {
        System.out.println("Insufficient balance. Need "
                + (product.getPrice() - machine.getBalance()) + " more cents.");
        return;
    }
    machine.setSelectedProduct(product);
    machine.setState(machine.getDispensingState());
    machine.getCurrentState().dispense();
}

public void dispense() {
    System.out.println("Select a product first.");
}

public void refund() {
    System.out.println("Refunding " + machine.getBalance() + " cents.");
    machine.resetBalance();
    machine.setState(machine.getIdleState());
}

}

class DispensingState implements VendingMachineState {
private final VendingMachine machine;

public DispensingState(VendingMachine machine) {
    this.machine = machine;
}

public void insertCoin(Coin coin) {
    System.out.println("Please wait, dispensing in progress.");
}

public void selectProduct(String code) {
    System.out.println("Already dispensing a product.");
}

public void dispense() {
    Product product = machine.getSelectedProduct();
    machine.getInventory().reduceStock(product.getCode());
    int change = machine.getBalance() - product.getPrice();

    System.out.println("Dispensing: " + product.getName());
    if (change > 0) {
        System.out.println("Returning change: " + change + " cents");
    }

    machine.resetBalance();
    machine.setSelectedProduct(null);

    if (machine.getInventory().isEmpty()) {
        machine.setState(machine.getSoldOutState());
    } else {
        machine.setState(machine.getIdleState());
    }
}

public void refund() {
    System.out.println("Cannot refund, dispensing in progress.");
}


}

class SoldOutState implements VendingMachineState {
private final VendingMachine machine;

public SoldOutState(VendingMachine machine) {
    this.machine = machine;
}

public void insertCoin(Coin coin) {
    System.out.println("Machine sold out. Refunding coin.");
}

public void selectProduct(String code) {
    System.out.println("Machine sold out.");
}

public void dispense() {
    System.out.println("Machine sold out.");
}

public void refund() {
    if (machine.getBalance() > 0) {
        System.out.println("Refunding " + machine.getBalance() + " cents.");
        machine.resetBalance();
    } else {
        System.out.println("No money to refund.");
    }
}


}

// ---------- Context ----------
class VendingMachine {
private final VendingMachineState idleState;
private final VendingMachineState hasMoneyState;
private final VendingMachineState dispensingState;
private final VendingMachineState soldOutState;
private VendingMachineState currentState;


private final Inventory inventory;
private int balance = 0;
private Product selectedProduct;

public VendingMachine(Inventory inventory) {
    this.inventory = inventory;
    idleState = new IdleState(this);
    hasMoneyState = new HasMoneyState(this);
    dispensingState = new DispensingState(this);
    soldOutState = new SoldOutState(this);
    currentState = inventory.isEmpty() ? soldOutState : idleState;
}

public void insertCoin(Coin coin) { currentState.insertCoin(coin); }
public void selectProduct(String code) { currentState.selectProduct(code); }
public void refund() { currentState.refund(); }

public void addBalance(int amount) { balance += amount; }
public void resetBalance() { balance = 0; }
public int getBalance() { return balance; }

public void setSelectedProduct(Product product) { this.selectedProduct = product; }
public Product getSelectedProduct() { return selectedProduct; }

public Inventory getInventory() { return inventory; }

public void setState(VendingMachineState state) { this.currentState = state; }
public VendingMachineState getCurrentState() { return currentState; }

public VendingMachineState getIdleState() { return idleState; }
public VendingMachineState getHasMoneyState() { return hasMoneyState; }
public VendingMachineState getDispensingState() { return dispensingState; }
public VendingMachineState getSoldOutState() { return soldOutState; }

}

// ---------- Demo ----------
 class VendingMachineDemo {
public static void main(String[] args) {
Inventory inventory = new Inventory();
inventory.addProduct(new Product("A1", "Coke", 75), 2);
inventory.addProduct(new Product("A2", "Chips", 50), 1);

    VendingMachine machine = new VendingMachine(inventory);

    System.out.println("-- Try selecting before paying --");
    machine.selectProduct("A1");

    System.out.println("\n-- Insert coins for Coke (75) --");
    machine.insertCoin(Coin.QUARTER); // 25
    machine.insertCoin(Coin.QUARTER); // 50
    machine.selectProduct("A1");      // not enough yet
    machine.insertCoin(Coin.QUARTER); // 75 -> exact
    machine.selectProduct("A1");      // dispenses Coke

    System.out.println("\n-- Buy Chips (50) with overpay --");
    machine.insertCoin(Coin.QUARTER);
    machine.insertCoin(Coin.QUARTER);
    machine.insertCoin(Coin.QUARTER); // 75, price is 50 -> 25 change
    machine.selectProduct("A2");      // dispenses Chips, machine now sold out

    System.out.println("\n-- Try again after sold out --");
    machine.insertCoin(Coin.QUARTER);
}

}