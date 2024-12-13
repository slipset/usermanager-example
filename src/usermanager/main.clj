;; copyright (c) 2019-2023 Sean Corfield, all rights reserved

(ns usermanager.main
  "This is an example web application, using just a few basic Clojure
  libraries: Ring, Compojure, Component, Selmer, and next.jdbc.

  I recommend this as a good way to get started building web applications
  in Clojure so that you understand the basic moving parts in any web app.

  Ring is pretty much the fundamental building block of all web apps
  in Clojure. It provides an abstraction that maps HTTP requests to
  simple Clojure hash maps. Your handler processes those hash maps
  and produces another hash map containing :status and :body that
  Ring turns into an HTTP response.

  Compojure is the most widely used routing library. It lets you
  define mappings from URL patterns -- routes -- to handler functions.

  Selmer is a templating library that lets you write your web pages
  as HTML templates that follow the Django style of simple variable
  substitution, conditionals, and loops. Another popular approach
  for building web pages is Hiccup, which takes Clojure data structures
  and transforms them to HTML. If you need designers to deal with your
  HTML templates, Selmer is going to be a lot easier for them to work with.

  next.jdbc is the next generation JDBC library for Clojure, replacing
  clojure.java.jdbc. It provides a fast, idiomatic wrapper around the
  complexity that is Java's JDBC class hierarchy.

  This example uses a local SQLite database to store data."
  (:require [compojure.coercions :refer [as-int]]
            [compojure.core :refer [GET POST let-routes]]
            [compojure.route :as route]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.defaults :as ring-defaults]
            [ring.util.response :as resp]
            [usermanager.controllers.user :as user-ctl]
            [usermanager.migration :as migration]
            [next.jdbc :as jdbc])
  (:gen-class))

(defn my-middleware
  "This middleware runs for every request and can execute before/after logic.

  If the handler returns an HTTP response (like a redirect), we're done.
  Else we use the result of the handler to render an HTML page."
  [handler]
  (fn [req]
    (let [resp (handler req)]
      (if (resp/response? resp)
        resp
        (user-ctl/render-page resp)))))

;; Helper for building the middleware:
(defn- add-app-component
  "Middleware to add your application component into the request. Use
  the same qualified keyword in your controller to retrieve it."
  [handler db]
  (fn [req]
    (handler (assoc req :application/db db))))

;; This is Ring-specific, the specific stack of middleware you need for your
;; application. This example uses a fairly standard stack of Ring middleware
;; with some tweaks for convenience
(defn middleware-stack
  "Given the application component and middleware, return a standard stack of
  Ring middleware for a web application."
  [db app-middleware]
  (fn [handler]
    (-> handler
        (app-middleware)
        (add-app-component db)
        (ring-defaults/wrap-defaults (-> ring-defaults/site-defaults
                                         ;; disable XSRF for now
                                         (assoc-in [:security :anti-forgery] false)
                                         ;; support load balancers
                                         (assoc-in [:proxy] true))))))

;; This is the main web handler, that builds routing middleware
;; from the application component (defined above). The handler is passed
;; into the web server component (below).
;; Note that Vars are used -- the #' notation -- instead of bare symbols
;; to make REPL-driven development easier. See the following for details:
;; https://clojure.org/guides/repl/enhancing_your_repl_workflow#writing-repl-friendly-programs
(defn my-handler
  "Given the application component, return middleware for routing.

  We use let-routes here rather than the more usual defroutes because
  Compojure assumes that if there's a match on the route, the entire
  request will be handled by the function specified for that route.

  Since we need to deal with page rendering after the handler runs,
  and we need to pass in the application component at start up, we
  need to define our route handlers so that they can be parameterized."
  [db]
  (let-routes [wrap (middleware-stack db #'my-middleware)]
    (GET  "/"                        []              (wrap #'user-ctl/default))
    ;; horrible: application should POST to this URL!
    (GET  "/user/delete/:id{[0-9]+}" [id :<< as-int] (wrap #'user-ctl/delete-by-id))
    ;; add a new user:
    (GET  "/user/form"               []              (wrap #'user-ctl/edit))
    ;; edit an existing user:
    (GET  "/user/form/:id{[0-9]+}"   [id :<< as-int] (wrap #'user-ctl/edit))
    (GET  "/user/list"               []              (wrap #'user-ctl/get-users))
    (POST "/user/save"               []              (wrap #'user-ctl/save))
    ;; this just resets the change tracker but really should be a POST :)
    (GET  "/reset"                   []              (wrap #'user-ctl/reset-changes))
    (route/resources "/")
    (route/not-found "Not Found")))

;; Standard web server component -- knows how to stop and start the
;; web server (with the application component as a dependency, and
;; the handler function as a parameter):

(defn web-server
  "Return a WebServer component that depends on the application.

  The handler-fn is a function that accepts the application (Component) and
  returns a fully configured Ring handler (with middeware)."
  [handler-fn db port]
  (run-jetty (handler-fn db)
             {:port port :join? false}))

;; This is the piece that combines the generic web server component above with
;; your application-specific component defined at the top of the file, and
;; any dependencies your application has (in this case, the database):
;; Note that a Var is used -- the #' notation -- instead of a bare symbol
;; to make REPL-driven development easier. See the following for details:
;; https://clojure.org/guides/repl/enhancing_your_repl_workflow#writing-repl-friendly-programs


(def ^:private my-db
  "SQLite database connection spec."
  {:dbtype "sqlite" :dbname "usermanager_db"})

(defn setup-database [db-spec] (jdbc/get-datasource db-spec))

(defn new-system
  "Build a default system to run. In the REPL:

  (def system (new-system 8888))

  (alter-var-root #'system component/start)

  (alter-var-root #'system component/stop)

  See the Rich Comment Form below."
  [port]
  (let [db (setup-database my-db)]
    {:db db
     :web-server (web-server #'my-handler db port)}))

(defn stop [system]
  (.stop (:web-server system)))

(comment
  (def system (new-system 8888))
  (stop system)
  )

(defonce ^:private
  ^{:doc "This exists so that if you run a socket REPL when
  you start the application, you can get at the running
  system easily.

  Assuming a socket REPL running on 50505:

  nc localhost 50505
  user=> (require 'usermanager.main)
  nil
  user=> (in-ns 'usermanager.main)
  ...
  usermanager.main=> (require '[next.jdbc :as jdbc])
  nil
  usermanager.main=> (def db (-> repl-system deref :application :database))
  #'usermanager.main/db
  usermanager.main=> (jdbc/execute! (db) [\"select * from addressbook\"])
  [#:addressbook{:id 1, :first_name \"Sean\", :last_name \"Corfield\", :email \"sean@worldsingles.com\", :department_id 4}]
  usermanager.main=>"}
  repl-system
  (atom nil))

(defn -main
  [& [port]]
  (let [port (or port (get (System/getenv) "PORT" 8080))
        port (cond-> port (string? port) Integer/parseInt)]
    (println "Starting up on port" port)
    ;; start the web server and application:
    (-> (new-system port )
        ;; then put it into the atom so we can get at it from a REPL
        ;; connected to this application:
        (->> (reset! repl-system)))
    (migration/populate (:db @repl-system) "sql-lite")))
