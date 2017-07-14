* just( ) — convert an object or several objects into an Observable that emits that object or those objects
* from( ) — convert an Iterable, a Future, or an Array into an Observable
* create( ) — advanced use only! create an Observable from scratch by means of a function, consider fromEmitter instead
* fromEmitter() — create safe, backpressure-enabled, unsubscription-supporting Observable via a function and push events.
* defer( ) — do not create the Observable until a Subscriber subscribes; create a fresh Observable on each subscription
* range( ) — create an Observable that emits a range of sequential integers
* interval( ) — create an Observable that emits a sequence of integers spaced by a given time interval
* timer( ) — create an Observable that emits a single item after a given delay
* empty( ) — create an Observable that emits nothing and then completes
* error( ) — create an Observable that emits nothing and then signals an error
* never( ) — create an Observable that emits nothing at all

# Just
 Just is similar to From, but note that From will dive into an array or an iterable or something of that sort to pull out items to emit, while Just will simply emit the array or iterable or what-have-you as it is, unchanged, as a single item.

Note that if you pass null to Just, it will return an Observable that emits null as an item. Do not make the mistake of assuming that this will return an empty Observable (one that emits no items at all). For that, you will need the Empty operator. 

# From
 When you work with Observables, it can be more convenient if all of the data you mean to work with can be represented as Observables, rather than as a mixture of Observables and other types. This allows you to use a single set of operators to govern the entire lifespan of the data stream.

Iterables, for example, can be thought of as a sort of synchronous Observable; Futures, as a sort of Observable that always emits only a single item. By explicitly converting such objects to Observables, you allow them to interact as peers with other Observables.

For this reason, most ReactiveX implementations have methods that allow you to convert certain language-specific objects and data structures into Observables. 

# Create
 You can create an Observable from scratch by using the Create operator. You pass this operator a function that accepts the observer as its parameter. Write this function so that it behaves as an Observable — by calling the observer’s onNext, onError, and onCompleted methods appropriately.

A well-formed finite Observable must attempt to call either the observer’s onCompleted method exactly once or its onError method exactly once, and must not thereafter attempt to call any of the observer’s other methods. 