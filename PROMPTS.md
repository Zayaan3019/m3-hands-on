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

## Part D -- AI-assisted git

**Original AI-drafted commit message** (what a "Generate commit message"
pass produces from the diff alone -- accurate about *what* changed,
silent on *why*):

```
Fix UserDTO and OrderController compile errors

- Remove duplicate fromUser method in UserDTO
- Add main method to UserDTO
- Remove duplicate getOrderById and createOrder methods in OrderController
- Simplify method bodies
```

**Edited commit message** (used for the actual commit -- honest subject,
plus a "why" sentence the AI could not have known):

```
Fix duplicate-method compile errors from accepted ghost text

Ghost text for the UserDTO.fromUser mapper and the two OrderController
TODO stubs got appended after the stub instead of replacing it, so
each ended up with a second, unreachable copy of the same method
(UserDTO's stray top-level fromUser, and Spring-annotated duplicates
nested inside getOrderById/createOrder that referenced annotations
this plain-Java project doesn't even depend on). Removed the stray
duplicates, kept the one correct method body in each case, and added
a main() to UserDTO that builds a User and prints fromUser's output
so the mapper can be eyeballed without a debugger.

I'm keeping this note because the compiler errors didn't point at the
real defect (an "implicit class needs main()" error for UserDTO, raw
parser errors for OrderController) -- the actual bug, in both cases,
was ghost text landing next to the stub rather than inside it.
```

**Original AI-drafted PR summary** ("Summarize PR" style -- a flat,
diff-shaped file list with no narrative):

```
## Summary
This PR updates UserDTO.java, OrderController.java, and PROMPTS.md.

- Modified UserDTO.java
- Modified OrderController.java
- Modified PROMPTS.md

## Changes
- fromUser method updated
- getOrderById and createOrder methods updated
```

**Edited PR description** (used for the real PR at
https://github.com/Zayaan3019/m3-hands-on/pull/1 -- adds the *what
broke*, the *fix*, and a *testing* section the draft never mentioned):

```
## What
`UserDTO.java` and `OrderController.java` both shipped with a compile
error introduced while accepting AI ghost text: the suggested method
body landed next to the TODO stub instead of replacing it, leaving a
duplicate/nested copy of each method behind.

- UserDTO.fromUser -- a second fromUser(User) was left floating
  outside the record body, which made javac parse the file as an
  implicit/compact source file and fail with "compact source file
  does not have main method".
- OrderController.getOrderById / createOrder -- each stub threw
  UnsupportedOperationException and then nested a second,
  Spring-annotated copy of the same method inside its own body --
  invalid syntax, and Spring isn't even a dependency here.

## Fix
- Removed the stray/duplicate method bodies in both files, keeping
  the one correct implementation in each.
- Added a main(String[] args) to UserDTO that builds a User, calls
  fromUser, and prints the result.
- getOrderById now returns store.get(id); createOrder validates its
  arguments, allocates the next id, stores, and returns the order.

## Why this matters
Both errors are recorded verbatim in PROMPTS.md, together with the
ghost text that produced them -- the point of this session is to
practice catching this exact failure mode at the compile step rather
than trusting an accepted suggestion.

## Testing
make test -- clean build, all 4 tests in OrderControllerTest pass.
```

## Part E -- Branch-name suggestion (optional)

Prompt given to the chat panel:

> Suggest a branch name for this issue: "customer wants to be able to
> close their account permanently". Format: prefix/short-kebab-slug.
> Prefix is one of: feat, fix, chore, docs, refactor.

Suggestion:

```
feat/permanent-account-closure
```

Reasoning offered: this adds a new user-facing capability (an account
is not closeable today), so `feat` fits better than `fix`; "permanent"
is kept in the slug because it's the detail that distinguishes this
from a reversible "deactivate account" feature, which would need a
different implementation (hard delete / anonymize vs. a status flag).

Would I have named it the same way? Yes for the prefix -- `feat` is
right, this isn't fixing anything broken. I'd likely shorten the slug
to `feat/close-account` in practice, since "permanently" is closer to
an implementation detail than something the branch name needs to
carry, and shorter branch names are easier to type/tab-complete in
this repo's history. Both are defensible; I'd only push back if the
AI had suggested `fix/...` (wrong prefix) or a slug over ~4 words.

## Reflection: where the AI understood intent, and where it didn't

The AI was reliable at **local, syntactic completion**: given `public
record UserDTO(`, it inferred a plausible field set from context, and
given a mapper stub with the right signature, it produced correct
field-by-field assignments once the record actually had fields to map.
It was also fine at **narrating a diff** -- both the draft commit
message and the draft PR summary correctly listed every file and
method that changed.

Where it broke down was **structural intent**: in both `UserDTO.java`
and `OrderController.java`, the accepted suggestion was placed *after*
the existing stub rather than *replacing* it, producing a duplicate
declaration the AI itself would have flagged as wrong had it re-read
the whole file instead of just the insertion point. It also had no way
to know *why* a change was made -- the "why" (ghost text landing next
to the stub instead of inside it, and the specific compiler symptom
that gave it away) had to be supplied by hand in both the commit
message and the PR description; the AI-drafted versions of both were
accurate about *what* changed and silent on *why*, which matches the
common pitfall this session calls out explicitly.
