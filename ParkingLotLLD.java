import java.sql.Time;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
public class ParkingLotLLDPractice {
    
}
//-----------------------------------------
abstract class Vehicle{
    private final String number;
    private final VehicleType type;

    public Vehicle(String number, VehicleType type) {
        this.number = number;
        this.type = type;
    }

    public String getNumber() {
        return number;
    }

    public VehicleType getType() {
        return type;
    }
}

class  Car extends Vehicle{
    public Car(String number) {
        super(number, VehicleType.CAR);
    }
    
}
class Bike extends Vehicle {
    public Bike(String number) { super(number, VehicleType.BIKE); }
}
class Truck extends Vehicle {
    public Truck(String number) { super(number, VehicleType.TRUCK); }
}
//-----------------------------------------------------------------
 abstract class Gate {
    protected final String id;

    public abstract GateType getType();
    public Gate(String id) {
        this.id = id;
    }
}
class EntryGate extends Gate {
    public EntryGate(String id) { super(id); }
    @Override
    public GateType getType() { return GateType.ENTRY; }

    public Ticket parkVehicle(Vehicle vehicle, LocalDateTime entryTime) {
        return ParkingLot.getInstance().parkVehicle(vehicle, entryTime);
    }
}
 class ExitGate extends Gate {
    public ExitGate(String id) { super(id); }
    @Override
    public GateType getType() { return GateType.EXIT; }
}

//----------------------------------------------------------------
class Ticket{
    private final String ticketId;
    private final LocalDateTime entryTime;
    private final Vehicle vehicle;
    private final String floorId;
    private final String spotId;
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    public Ticket(String ticketId, LocalDateTime entryTime, Vehicle vehicle, String floorId, String spotId) {
        this.ticketId = ticketId;
        this.entryTime = entryTime;
        this.vehicle = vehicle;
        this.floorId = floorId;
        this.spotId = spotId;
    }

    public String getTicketId() {
        return ticketId;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public String getFloorId() {
        return floorId;
    }

    public String getSpotId() {
        return spotId;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
}
//----------------------------------------------------------------  
class  ParkingSpot{
    private String spotId;
    private VehicleType allowedType;
    private AtomicBoolean occupied = new AtomicBoolean(false);

    public ParkingSpot(String spotId, VehicleType allowedType) {
        this.spotId = spotId;
        this.allowedType = allowedType;
    }

    public boolean tryOccupy() {
        return occupied.compareAndSet(false, true);
    }

    public void vacate() {
        occupied.set(false);
    }

    public boolean isOccupied() {
        return occupied.get();
    }

    public String getId() {
        return spotId;
    }
  public VehicleType getAllowedType() {
        return allowedType;
    }
 
}

//----------------------------------------------------------
class ParkingFloor{
    private final String id;
    private final Map<String, ParkingSpot> spots = new HashMap<>();

    public ParkingFloor(String id) { 
        this.id = id; 
    }

    public String getId() {
        return id;
    }

    public Map<String, ParkingSpot> getSpots() {
        return spots;
    }

    public void addSpot(ParkingSpot spot) {
        spots.put(spot.getId(), spot);
    }

    public ParkingSpot findAvailableSpot(VehicleType vehicleType) {
        for (ParkingSpot spot : spots.values()) {
            if (spot.getAllowedType() == vehicleType && spot.tryOccupy()) {
                return spot;
            }
        }
        return null;
    }
}
//----------------------------------------------------------------------
// //--------------------------------------------------------------

//------------------------------------------------------------------
 enum VehicleType{
    BIKE,
    CAR,
    TRUCK
}
enum PaymentStatus{
    PENDING,
    SUCCESS,
    FAILED
}
 enum GateType {
    ENTRY,
    EXIT
}
enum PricingStrategyType {
    TIME_BASED, EVENT_BASED
}
enum PaymentMode {
    CASH, UPI, CARD
}
//---------------------------------------

class  ParkingLot{
     private static final ParkingLot INSTANCE = new ParkingLot();

    private final Map<String, ParkingFloor> floors = new HashMap<>();
    private final Map<String, Ticket> activeTickets = new HashMap<>();
  
    private PricingStrategy pricingStrategy;

    private ParkingLot() {
        this.pricingStrategy = PricingStrategyFactory.get(PricingStrategyType.TIME_BASED);
    }

    public static ParkingLot getInstance() {
        return INSTANCE;
    }

    public void addFloor(ParkingFloor floor) {
        floors.put(floor.getId(), floor);
    }

    public void setPricingStrategy(PricingStrategy pricingStrategy) {
        this.pricingStrategy = pricingStrategy;
    }

    // b1, b2 (one spot)
    // car, bike

    public Ticket parkVehicle(Vehicle vehicle, LocalDateTime entryTime) {
        for (ParkingFloor floor : floors.values()) {
            ParkingSpot spot = floor.findAvailableSpot(vehicle.getType());

            if (spot != null) {
                // Successfully reserved the spot via atomic operation
                String ticketId = UUID.randomUUID().toString();
                Ticket ticket = new Ticket(ticketId, entryTime, vehicle, floor.getId(), spot.getId());

                activeTickets.put(ticketId, ticket);
                System.out.println("Vehicle parked. Ticket: " + ticketId);
                return ticket;
            }
        }

        System.out.println("No spot available for vehicle type: " + vehicle.getType());
        return null;
    }

    public void unparkVehicle(String ticketId, LocalDateTime exitTime, PaymentMode paymentMode) {
        Ticket ticket = activeTickets.get(ticketId);
        if (ticket == null) {
            System.out.println("Invalid ticket ID.");
            return;
        }

        double fee = pricingStrategy.calculateFee(
                ticket.getVehicle().getType(),
                ticket.getEntryTime(),
                exitTime
        );

        PaymentStratergy strategy = PaymentStrategyFactory.get(paymentMode);
        PaymentProcessor processor = new PaymentProcessor(strategy);
        boolean paid = processor.pay(ticket, fee);

        if (!paid) {
            System.out.println("Vehicle cannot exit. Payment unsuccessful.");
            return;
        }

        ParkingSpot spot = floors.get(ticket.getFloorId()).getSpots().get(ticket.getSpotId());
        spot.vacate();
        activeTickets.remove(ticketId);

        System.out.println("Vehicle exited. Fee charged: ₹" + fee);
    }

    public void printStatus() {
        floors.forEach((floorId, floor) -> {
            System.out.println("Floor: " + floorId);
            floor.getSpots().values().forEach(spot -> {
                System.out.println(" Spot " + spot.getId() + " [" + spot.getAllowedType() + "] - " + (spot.isOccupied() ? "Occupied" : "Free"));
            });
        });
    }

}

class PaymentProcessor{
    private final PaymentStratergy strategy;

    public PaymentProcessor(PaymentStratergy strategy) {
        this.strategy = strategy;
    }

    public boolean pay(Ticket ticket, double amount) {
        boolean success = strategy.processPayment(ticket, amount);
        if (success) {
            ticket.setPaymentStatus(PaymentStatus.SUCCESS);
        } else {
            ticket.setPaymentStatus(PaymentStatus.FAILED);
            System.out.println("Payment failed for ticket: " + ticket.getTicketId());
        }
        return success;
    }
}
//-------------------------------------------------------------------

interface PaymentStratergy{
    boolean processPayment(Ticket ticket, double amount);
}

class CardPayment implements PaymentStratergy {
    @Override
    public boolean processPayment(Ticket ticket, double amount) {
        System.out.println("Paid ₹" + amount + " for ticket " + ticket.getTicketId() + " via Card.");
        return true;
    }
}

class CashPayment implements PaymentStratergy {
    @Override
    public boolean processPayment(Ticket ticket, double amount) {
        System.out.println("Paid ₹" + amount + " for ticket " + ticket.getTicketId() + " via Cash.");
        return true;
    }
}

class UpiPayment implements PaymentStratergy {
    @Override
    public boolean processPayment(Ticket ticket, double amount) {
        System.out.println("Paid ₹" + amount + " for ticket " + ticket.getTicketId() + " via UPI.");
        return true;
    }
}

//---------------------------------------------------------
interface PricingStrategy {
    double calculateFee(VehicleType type, LocalDateTime entryTime, LocalDateTime exitTime);
}
class EventBasedPricing implements PricingStrategy {
   public double calculateFee(VehicleType vehicleType, LocalDateTime entryTime, LocalDateTime exitTime){
    return  0.00;
   }
}
class TimeBasedPricing implements PricingStrategy  {
    public double calculateFee(VehicleType vehicleType, LocalDateTime entryTime, LocalDateTime exitTime){
    return  0.00;
   }
}
//-----------------------------------------------------------------------
 class VehicleFactory {
    public static Vehicle create(String number, VehicleType type) {
        return switch (type) {
            case CAR -> new Car(number);
            case BIKE -> new Bike(number);
            case TRUCK -> new Truck(number);
        };
    }
}
 class PricingStrategyFactory {
    public static PricingStrategy get(PricingStrategyType type) {
        return switch (type) {
            case EVENT_BASED -> new EventBasedPricing();
            case TIME_BASED -> new TimeBasedPricing();
            
        };
    }
}

 class PaymentStrategyFactory {
    public static PaymentStratergy get(PaymentMode mode) {
        return switch (mode) {
            case CASH -> new CashPayment();
            case CARD -> new CardPayment();
            case UPI -> new UpiPayment();
        };
    }
}
 class Main {
    public static void main(String[] args) {
        ParkingLot lot = ParkingLot.getInstance();
        EntryGate entryGate = new EntryGate("EG1");
        ExitGate exitGate = new ExitGate("XG1");

        lot.setPricingStrategy(PricingStrategyFactory.get(PricingStrategyType.valueOf("EVENT_BASED")));

        ParkingFloor floor1 = new ParkingFloor("Floor1");
        floor1.addSpot(new ParkingSpot("F1S1", VehicleType.BIKE));
        floor1.addSpot(new ParkingSpot("F1S2", VehicleType.CAR));
        floor1.addSpot(new ParkingSpot("F1S3", VehicleType.TRUCK));
        floor1.addSpot(new ParkingSpot("F1S4", VehicleType.CAR));
        lot.addFloor(floor1);

        System.out.println("--------------------------");

        Vehicle bike1 = VehicleFactory.create("KA01AB1234", VehicleType.BIKE);
        Vehicle bike2 = VehicleFactory.create("KA01AB5678", VehicleType.BIKE);
        LocalDateTime entryTime = LocalDateTime.of(2025, 5, 21, 7, 30);
        System.out.println(entryTime.truncatedTo(ChronoUnit.HOURS));

        Thread t1 = new Thread(() -> entryGate.parkVehicle(bike1, entryTime));
        Thread t2 = new Thread(() -> entryGate.parkVehicle(bike2, entryTime));

        t1.start();
        t2.start();
    }
}