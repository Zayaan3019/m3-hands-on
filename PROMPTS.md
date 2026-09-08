public record UserDTO(String id, String email, String name) {
    public static UserDTO fromUser(User user) {
        return new UserDTO(user.id(), user.email(), user.name());
    }
}


public record UserDTO(String id, String email, String name, boolean active) {
    public static UserDTO fromUser(User user) {
        return new UserDTO(
            String.valueOf(user.getId()),
            user.getEmail(),
            user.getName(),
            user.isActive()
        );
    }
}

## Compile error found in src/UserDTO.java

`UserDTO.java` had a duplicate `fromUser` method floating **outside** the
record body (after the closing `}` of the record, lines 18-25 of the
original file). That stray top-level method declaration made `javac`
(JDK 25) parse the file as a "compact source file" (implicit class),
which requires a `main` method — producing this exact error:

```
$ javac -d out src/User.java src/UserDTO.java
src\UserDTO.java:7: error: compact source file does not have main method in the form of void main() or void main(String[] args)
public record UserDTO(String id, String email, String name, boolean active) {
^
1 error
```

Fix: removed the duplicate/stray `fromUser` method left outside the
record (the correct one already existed inside the record body), and
added a `main(String[] args)` method to `UserDTO` that constructs a
`User`, calls `UserDTO.fromUser(user)`, and prints the resulting DTO.

## Compile error found in src/OrderController.java (2026-09-08)

`OrderController.java`'s two TODO stubs, `getOrderById(long)` and
`createOrder(String, int)`, each threw `UnsupportedOperationException`
and then had a **second, Spring-annotated version of the same method
declared inside the method body** (Copilot's suggestion was appended
after the stub instead of replacing it). A method declaration cannot
live inside another method's body, and the `@GetMapping`/`@PathVariable`/
`@PostMapping`/`@RequestParam` annotations aren't even available (this
is a plain-Java project with no Spring dependency). This produced:

```
$ javac -d out src/OrderController.java
src\OrderController.java:37: error: ';' expected
        public Order getOrderById(@PathVariable long id) {
                                 ^
src\OrderController.java:37: error: ';' expected
        public Order getOrderById(@PathVariable long id) {
                                                       ^
src\OrderController.java:48: error: ';' expected
        public Order createOrder(@RequestParam String item, @RequestParam int qty) {
                                ^
src\OrderController.java:48: error: <identifier> expected
        public Order createOrder(@RequestParam String item, @RequestParam int qty) {
                                                           ^
src\OrderController.java:48: error: illegal start of expression
        public Order createOrder(@RequestParam String item, @RequestParam int qty) {
                                                            ^
src\OrderController.java:48: error: ';' expected
        public Order createOrder(@RequestParam String item, @RequestParam int qty) {
                                                                         ^
src\OrderController.java:48: error: ';' expected
        public Order createOrder(@RequestParam String item, @RequestParam int qty) {
                                                                                 ^
7 errors
```

Fix: removed the `throw new UnsupportedOperationException(...)` lines
and the nested Spring-annotated duplicate methods, and kept plain-Java
logic in their place:
- `getOrderById(long id)` returns `store.get(id)` (or `null` if absent).
- `createOrder(String item, int qty)` validates `item`/`qty`, allocates
  `nextId`, stores the new `Order`, and returns it.
