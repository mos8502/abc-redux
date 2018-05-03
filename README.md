# ABC Redux

## Introduction
ABC redux is a minimalistic implementation of a Redux store. Though nothing prevents you from consuming it from java in its current state it is not so nice to consume it from java but that will be addressed in the future.

ABC redux is built following these principles
1. Have the smallest footprint as possible:
Apart from the Kotlin standard library it does not use any  3rd party libraries which allows it to be small
2. Be threading agnostic: the core implementation doesn't employ any threadring whatsoever but the implementation is thread safe through a means of lightweight locking which will detect and prevent concurrent modification of state. The API has been built in a way however that it should be easy to use it with coroutines or RxJava. Currently there are no adapters for these libraries but they will be coming in the future
3. Allow incremental building of  state: in order not define your whole application state in one place or make your state too elastic to accommodate adding new properties to support new features ABC Redux allow you to build your state as a tree dynamically. This practically means that your state tree can be extended at any point without compromising immutability or needing to use "multiple stores".


## Example
You create your root state by creating a root store
```kotlin
data class Credentials(val username: String = "", password: String = "") {
   val isLoggedIn = !username.isEmpty() && !password.isEmpty()
}

val rootStore = StateStore(Credentials())

```
A ```StateStore``` is the simplest store that allows actions of type ```(S) -> S``` to be dispatched to the store to mutate state. The state can in turn be observed by subscibing to the store
```kotlin
val subscription = rootStore.subscribe { credentials ->
   ...
}
```
You can stop listening to state changes by calling ```unsubscribed()``` on the subscription returned by ```subscribe()```.
```kotlin
subscription.unsubscribe()
```
You can extend your state by creating a sub state.
```kotlin
data class User(val firstName: String, val lastName: String)
val subState = rootStore.subState("user") { User(firstName = "Alan", lastName = "Turing")}
```
A sub state will represent the path from the newly create state node to the root node: ```State<Credentials, User>```
As this doesn't look too nice and becomes quite unmanageable with deep states, ABC Redux allows you to map any state to a shape that better represents it at a given level.
```kotlin
data class LoggedInUser(val username: String, val displayName: String, val password: String)
val mappedStore = subStore.map(Lens(
       get = { state: State<Credentials, User> ->
           LoggedInUser(
                   username = state.parentState.username,
                   password = state.parentState.password,
                   displayName = "${state.state.firstName} ${state.state.lastName}"
           )
       },
       set = { mappedState: LoggedInUser ->
           { state ->
               state.copy(
                       parentState = Credentials(
                               username = mappedState.username,
                               password = mappedState.password
                       ),
                       state = User(
                               firstName = mappedState.displayName.split(" ")[0],
                               lastName = mappedState.displayName.split(" ")[1]
                       )
               )
           }
       }
))
```
Note that the state represented by the mapped store will note create new state properties. It's a simple mapping of the state at a given node. It's a view of the state if you will.

And finally you can create a "reducer store" to be able to mutate your state through well defined actions and a reducer.
```kotlin
sealed class Action {
   data class SetUsername(val username: String): Action()
   data class SetPassword(val password: String): Action()
}

fun reducer(state: LoggedInUser, action: Action): LoggedInUser {
    return when(action) {
        is SetUsername -> state.copy(username = action.username)
        is SetPassword -> state.copy(password = action.password)
    }
}
   
val reducerStore = mappedStore.withReducer(::reducer)

```

```StateStore.withReducer()``` also takes an optional list of Middlewares

