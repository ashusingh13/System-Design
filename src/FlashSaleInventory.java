import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class FlashSaleInventory {
    private static final long RESERVATION_MINUTES = 5;

    // Remaining unsold quantity for each product.
    private final Map<String, Integer> inventory = new HashMap<>();

    // At most one reservation per user.
    private final Map<String, Reservation> reservations = new HashMap<>();

    private static class Reservation{
        private final String productId;
        private final long reservationTimeMinutes;

        private Reservation(String productId, long reservationTimeMinutes) {
            this.productId = productId;
            this.reservationTimeMinutes = reservationTimeMinutes;
        }
    }

    // Add stock to the warehouse.
    public void addInventory(String productId, int totalQuantity){
        if(productId == null || productId.trim().isEmpty()){
            throw new IllegalArgumentException("Product ID cannot be empty");
        }
        if(totalQuantity <= 0){
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        inventory.merge(productId, totalQuantity, Integer :: sum);
    }

    // Reserve one unit for a user
    public boolean addToCart(
            String userId, String productId, long currentTimeMinutes) {
        validateUserId(userId);
        validateProductId(productId);

        // Release expired reservations before checking stock.
        releaseExpiredReservations(currentTimeMinutes);

        // A user may hold only one active reservation.
        if(reservations.containsKey(userId)){
            return false;
        }
        Integer totalStock = inventory.get(productId);
        if(totalStock == null || totalStock <= 0){
            return false;
        }
        int reservedCount = countReservations(productId);

        // Stock remaining after accounting for reservations.
        int availableStock = totalStock - reservedCount;

        if(availableStock <= 0){
            return false;
        }
        reservations.put(
                userId,
                new Reservation(productId, currentTimeMinutes));
        return true;
    }

    // Confirm a purchase if the reservation is still valid.
    public boolean checkout(String userId, long currentTimeMinutes){
        validateUserId(userId);
        Reservation reservation = reservations.get(userId);
        if(reservation == null){
            return false;
        }
        if(isExpired(reservation, currentTimeMinutes)){
            reservations.remove(userId);
            return  false;
        }

        // Commit the purchase: permanently reduce unsold stock.
        inventory.put(
                reservation.productId,
                inventory.get(reservation.productId)-1);

        // Remove the active reservation.
        reservations.remove(userId);
        return true;
    }

    // Release every reservation older than five minutes.
    public void releaseExpiredReservations(long currentTimeMinutes){
        Iterator<Map.Entry<String, Reservation>> iterator = reservations.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<String, Reservation> entry = iterator.next();
            Reservation reservation = entry.getValue();

            if(isExpired(reservation, currentTimeMinutes)){
                // Stock was never permanently deducted, so removing
                // the reservation makes the unit available again.
                iterator.remove();
            }
        }
    }

    // Return currently available stock.
    public int getAvailableStock(String productId){
        validateProductId(productId);

        Integer totalStock = inventory.get(productId);
        if(totalStock == null){
            return 0;
        }
        return totalStock - countReservations(productId);
    }

    private int countReservations(String productId){
        int count = 0;
        for(Reservation reservation : reservations.values()){
            if(reservation.productId.equals(productId)){
                count++;
            }
        }
        return count;
    }

    private boolean isExpired(
            Reservation reservation, long currentTimeMinutes){
        return  currentTimeMinutes >= reservation.reservationTimeMinutes
                && currentTimeMinutes - reservation.reservationTimeMinutes
                > RESERVATION_MINUTES;
    }

    private void validateUserId(String userId){
        if(userId == null || userId.trim().isEmpty()){
            throw new IllegalArgumentException("User ID cannot be empty");
        }
    }

    private void validateProductId(String productId){
        if(productId == null || productId.trim().isEmpty()){
            throw new IllegalArgumentException("Product ID cannot be empty");
        }
    }

    public static void main(String[] args){
        FlashSaleInventory sale = new FlashSaleInventory();
        sale.addInventory("IPHONE-17", 2);

        System.out.println("Initial stock: " + sale.getAvailableStock("IPHONE-17"));

        // User A reserves at minute 10.
        System.out.println("A reserves: " + sale.addToCart("A", "IPHONE-17",10));

        // User B reserves at minute 12.
        System.out.println("B reserves: " + sale.addToCart("B", "IPHONE-17", 12));

        System.out.println("Stock after reservations: " + sale.getAvailableStock("IPHONE-17"));

        // No stock remains available.
        System.out.println("C reserves: " + sale.addToCart("C", "IPHONE-17", 13));

        // Exactly five minutes later: still valid.
        System.out.println("A checkout at minute 15: " + sale.checkout("A", 15));

        System.out.println("Stock after A checkout: " + sale.getAvailableStock("IPHONE-17"));

        // B reserved at minute 12. At minute 18, age is 6 minutes.
        sale.releaseExpiredReservations(18);

        System.out.println("Stock after B expires: " + sale.getAvailableStock("IPHONE-17"));

        // C can now reserve the released phone.
        System.out.println("C reserves at minute 18: "+ sale.addToCart("C", "IPHONE-17", 18));

        // Reservation age is 6 minutes, so checkout fails.
        System.out.println("C checkout at minute 24: " + sale.checkout("C", 24));

        System.out.println("Final available stock: " + sale.getAvailableStock("IPHONE-17"));
    }
}
