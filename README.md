# Low-level-Design--Java
LLD practice
<img width="402" height="453" alt="image" src="https://github.com/user-attachments/assets/975209f9-3e58-45b3-8f92-9c3a90851a10" />
# Parking Lot Low-Level Design (LLD)

A Java-based Parking Lot Management System designed using Object-Oriented Design principles and common design patterns such as Strategy Pattern, Factory Pattern, and Singleton Pattern.

## Features

- Vehicle Parking
- Vehicle Exit / Unparking
- Multi-floor Parking Support
- Vehicle Type Based Parking Spots
- Ticket Generation
- Payment Processing
- Pluggable Pricing Strategies
- Multiple Payment Modes
- Thread-safe Spot Allocation using AtomicBoolean
- Factory-based Object Creation

---

## Supported Vehicle Types

- Bike
- Car
- Truck

---

## Supported Payment Modes

- Cash
- Card
- UPI

---

## Design Patterns Used

# Singleton Pattern
#Stratergy Pattern

`ParkingLot`

Ensures only one parking lot instance exists throughout the application.

```java
ParkingLot.getInstance()
