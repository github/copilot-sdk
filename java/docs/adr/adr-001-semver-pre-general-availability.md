# SemVer requirements pre general-availability of Reference Implementation

## Context and Problem Statement

Steve Sanderson agreed that `copilot-sdk-java` will track reference implementation version numbers directly, with one exception: when the Java SDK needs to ship a breaking change before 1.0, the reference implementation will bump its minor version to accommodate, giving our release a clean version number that signals the change to users.

The reference implementation makes no backward compatibility guarantees pre-1.0 — and neither will we. That said, we're choosing to hold ourselves to a higher standard as a matter of good practice: we'll use minor version bumps as a signal to users when we do ship something breaking.

The 2026-02 state of `copilot-sdk-java` is that it takes Java 17+ as its baseline. This decision precludes the use of Java 21 features such as virtual threads. Our pre-analysis showed the **possibility** of a significant performance benefit when using Virtual Threads with Java 21.

We took an architectural decision to enable us to pursue investigating this possibility immediately.

## Considered Options

* Track SemVer of reference implementation, with one exception.
* Completely avoid the need for this by doing no breaking changes pre-1.0.
* Abandon the policy of tracking the versions of the reference implementation directly, just do our own thing.

## Decision Outcome

Chosen option: "Track SemVer of reference implementation, with one exception.", because this enables us to pursue Virtual Threads without delaying the first public release of `copilot-sdk-java`. Also, we're supposed to be aggressively modernizing our customers.

To some extent, I would use qualifiers to mark a release as having some feature that is awaiting a reference implementation full release before it goes full ga, i.e you put out 0.1.46-virtualthreads.3 until reference implementation is ready to move to 0.2.0 then you release your virtual threads change and go 0.2.0. So I would make your agreement that your version numbers would match with the exception of qualifiers that you might add in exceptional circumstances.

## Related work items

- https://devdiv.visualstudio.com/DevDiv/_workitems/edit/2745172

