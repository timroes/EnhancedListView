EnhancedListView [Deprecated]
=============================

An Android ListView with enhanced functionality (e.g. Swipe To Dismiss and Undo)

The documentation can be found in the wiki: https://github.com/timroes/EnhancedListView/wiki

Deprecation Notice
------------------

With the new Android L release a new View called
[RecyclerView](https://developer.android.com/preview/material/ui-widgets.html#recyclerview) will be introduced.
This view will be part of the support library, so it is backward compatible till API level 7.

This View has many advantages over the old ListView (and other adapter based views). I highly recommend 
using the new `RecyclerView` from its official release on. This means, this library would need a complete rewrite
to the new view. Since I don't have the time to do so, I deprecate this library with the coming Android L release,
**meaning there will be no further development from my side.**

If you want to port this view to the new `RecyclerView` feel free to take as much from this library that you need (it
is all licensed under Apache Software License). If you've written a replacement library on the new technology, feel
free to drop me a notice. I will gladly add a link here to your library.

If you start a new project and stumble upon this library, please think twice of including it in your project, since
it will very soon be old technology (and not developed any further).

Update Notice
-------------

### v0.3.0

* All resources (layouts, colors, etc.) got an `elv_` prefix. So if you have changed 
  some of these in your own app, you must make sure to also add the `elv_` prefix to your
  resources (e.g. to change or internationalize the "Undo" string, you will need to have a
  string resource `elv_undo` instead of `undo`).
