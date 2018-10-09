# Things ToDo

There are several things to do for the library right now.

Please make sure that you open branches and use proper git-etiquette
when offering contributions.

### High Priority
- Move from using `GMTDate` to `OffsetDateTime`
  - I wanted to use it, but `GMTDate` isn't something we can count on
    to be backwards compatible in future versions of ktor
- Extracting websocket package contents from `me.kgustave.dkt.internal.websocket`
  to `me.kgustave.dkt.websocket`.
  - This is because not everything is going to end up as internal.
- Setup after receiving `READY` event
- Handle events
- Cache entities
  - I want to make sure that caching **is optional**
- The entire basic requester needs to be stripped out of the main
  module, and reallocated to a submodule
  - I may consider extracting the entire library down to a module
    as well, mostly to avoid dependency circulation or repeated/copied
    utilities (which could therefore be placed in a common module).
  - RestPromise is excluded from this on account it wraps a DiscordBotImpl
    instance inside it for access to things like the promise dispatcher.

### Low Priority
- More detailed contribution documentation.
- Documentation
  - Dokka support in gradle will also be required.
- Testing utilities
- Command framework
- Examples
- Repackaging
  - This will likely occur when nearing the initial release candidate
    and be majorly breaking for anything that was unfortunate enough to
    use this framework before it's initial release.

### Other Things
- ~~Discord Support Guild~~ (Will be appended to the README.md when available)
- Design Documentation
- Proof of concept coroutine based bot

### General Notes On Contributing
1) Keep code clean
  - If it's not clean, I'll ask nicely to have things cleaned up, but please
    do try to keep it nice and tide
  - Also, __NEVER__ do `) : Something`, __ALWAYS__ do `): Something` (this
    goes for properties too).
3) Coroutines and coroutine logic are **always** preferable over blocking, thread-based,
   or generally "non-coroutine" based logic and operations.
4) Comment everything
