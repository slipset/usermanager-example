# Example Web Application in Clojure

This is a paraphrasing of Sean Corfields [usermanager-example](https://github.com/seancorfield/usermanager-example) application.
This version of it explores a couple of themes
- I'm not a huge fan of [Component](https://github.com/stuartsierra/component)/[Integrant](https://github.com/weavejester/integrant) et al, so none of that
- Routers are really very simple things at the core. This is a very simple app, so no router, just a `cond`
- I find middlewares confusing, so I don't like middleware stacks, so the use of middlewares is reduced

## Making queries first class

Perhaps most importantly, I find the most important bits of code in a crud-ish app like this one
are the queries. Therefore, they've been lifted up to top level.

## Do we really need a web frame work

I think this app also serves as an example of all the choices you don't have to make up front.
- It's easy to add a routing lib later if you find you need it
- Want more middlewares - add them
- Need a [Component](https://github.com/stuartsierra/component) like thing, retrofit it.

Basically Clojure, powerful as it is, let's you write the essential code of a webapp without having to 
start out by choosing a web-framework, you just pick a set of libaries, and it's not really that important
if you choose lib x or lib y

## Requirements

This example assumes that you have a recent version of the [Clojure CLI](https://clojure.org/guides/deps_and_cli) installed (at least 1.10.3.933), and provides a `deps.edn` file, and a `build.clj` file.

Clojure 1.10 (or later) is required. The "model" of this example app uses namespace-qualified keys in hash maps. It uses [next.jdbc](https://cljdoc.org/d/seancorfield/next.jdbc) -- the "next generation" JDBC library for Clojure -- which produces namespace-qualified hash maps from result sets.

## Usage

Clone the repo, `cd` into it, then follow below to _Run the Application_ or _Run the application in REPL_
or _Run the tests_ or _Build an Uberjar_.

### Run the Application
```
clojure -M -m usermanager.main
```

It should create a SQLite database (`usermanager_db`) and populate two tables (`department` and `addressbook`) and start a Jetty instance on port 8080.

If that port is in use, start it on a different port. For example, port 8100:

```
clojure -M -m usermanager.main 8100
```

### Run the Application in REPL

Start REPL

```
$ clj
```

Once REPL starts, start the server as an example on port 8888:

```clj
user=> (require 'usermanager.main)                             ; load the code
user=> (in-ns 'usermanager.main)                               ; move to the namespace
usermanager.main=> (def system (new-system 8888))              ; specify port
```

### Run the tests with:

```
clojure -T:build test
```

You should see something like this:

```
Running task for: test

Running tests in #{"test"}
2023-01-24 22:31:01.269:INFO::main: Logging initialized @4050ms to org.eclipse.jetty.util.log.StdErrLog

Testing usermanager.model.user-manager-test
Created database and addressbook table!
Populated database with initial data!

Ran 3 tests containing 9 assertions.
0 failures, 0 errors.
```

This uses the `:build` alias to load the `build.clj` file, based on [`tools.build`](https://clojure.org/guides/tools_build), and run the `test` task.

## Build an Uberjar

For production deployment, you typically want to build an "uberjar" -- a `.jar` file that contains Clojure itself and all of the code from your application and its dependencies, so that you can run it with the `java -jar` command.

The `build.clj` file -- mentioned above -- contains a `ci` task that:

* runs all the tests
* cleans up the `target` folder
* compiles the application (sometimes called "AOT compilation")
* produces a standalone `.jar` file

```
clojure -T:build ci
```

That should produce the same output as `test` above, followed by something like:

```
Copying source...

Compiling usermanager.main...
2023-01-24 22:35:37.922:INFO::main: Logging initialized @2581ms to org.eclipse.jetty.util.log.StdErrLog

Building JAR...
```

The `target` folder will be created if it doesn't exist and it will include a `classes` folder containing all of the compiled Clojure source code from the `usermanager` application _and all of its dependencies_ including Clojure itself:

```
$ ls target/classes/
camel_snake_kebab  clout  compojure  instaparse  medley  public  selmer       views
clojure            com    crypto     layouts     next    ring    usermanager
```

It will also include the standalone `.jar` file which you can run like this:

```
java -jar target/usermanager/example-standalone.jar
```

This should behave the same as the _Run the Application_ example above.

This JAR file can be deployed to any server that have Java installed and run with no other external dependencies or files.

## Stuff I Need To Do

* I might add a `datafy`/`nav` example.

# License & Copyright

Copyright (c) 2015-2023 Sean Corfield.

Distributed under the Apache Source License 2.0.
