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
  (:require [compojure.route :as route]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.defaults :as ring-defaults]
            [ring.util.response :as resp]
            [usermanager.controllers.user :as user-ctl]
            [usermanager.migration :as migration]
            [next.jdbc :as jdbc]
            [selmer.parser :as tmpl]
            [clojure.string :as str])
  (:gen-class))

(defn render-page
  [data view]
  (let [data (assoc data :changes @user-ctl/changes)
        html (tmpl/render-file (str "views/user/" view ".html") data)]
    (-> (resp/response (tmpl/render-file "layouts/default.html"
                                         (assoc data :body [:safe html])))
        (resp/content-type "text/html"))))

(defn ->db [req]
  (-> req :application/db))


(defn- ->id [uri]
  (-> uri (str/split #"/") last Long/parseLong))

(defn handle-default []
  (-> user-ctl/default
      (render-page "default")))

(defn handle-reset-changes []
  (-> (user-ctl/reset-changes)
      (render-page "default")))

(defn handle-delete-user [db id]
  (user-ctl/delete-by-id db id)
  (resp/redirect "/user/list"))

(defn handle-edit [db]
  (-> db
      (user-ctl/edit nil)
      (render-page "form")))

(defn handle-edit-user [db id]
  (-> db
      (user-ctl/edit id)
      (render-page "form")))

(defn handle-get-users [db]
  (-> db
      user-ctl/get-users
      (render-page "list")))

(defn handle-save [db user]
  (-> db
      (user-ctl/save user))
  (resp/redirect "/user/list"))

(defn my-handler
  "Given the application component, return middleware for routing.

  We use let-routes here rather than the more usual defroutes because
  Compojure assumes that if there's a match on the route, the entire
  request will be handled by the function specified for that route.

  Since we need to deal with page rendering after the handler runs,
  and we need to pass in the application component at start up, we
  need to define our route handlers so that they can be parameterized."
  [db]
  (fn [{:keys [uri] :as req}]
    (cond
      (= uri "/") (#'handle-default)
      (str/starts-with? uri "/user/delete") (#'handle-delete-user db (->id uri))
      (= uri "/user/form") (#'handle-edit db)
      (str/starts-with? uri "/user/form/") (#'handle-edit-user db (->id uri))
      (= uri "/user/list") (#'handle-get-users db)
      (= uri "/user/save") (#'handle-save db (:params req))
      (= uri "/reset") (#'handle-reset-changes)
      :else ((route/not-found "Not Found") req))))

(defn middleware-stack
  "Given the application component and middleware, return a standard stack of
  Ring middleware for a web application."
  [db]
  (-> (my-handler db)
      (ring-defaults/wrap-defaults (-> ring-defaults/site-defaults
                                         ;; disable XSRF for now
                                       (assoc-in [:security :anti-forgery] false)
                                         ;; support load balancers
                                       (assoc-in [:proxy] true)))))

(defn web-server
  "Return a WebServer component that depends on the application.

  The handler-fn is a function that accepts the application (Component) and
  returns a fully configured Ring handler (with middeware)."
  [handler-fn port]
  (run-jetty handler-fn
             {:port port :join? false}))

(def ^:private my-db
  "SQLite database connection spec."
  {:dbtype "sqlite" :dbname "usermanager_db"})

(defn setup-database [db-spec] (jdbc/get-datasource db-spec))

(defn new-system
  "Build a default system to run. In the REPL:

  (def system (new-system 8888))

  (stop system)

  See the Rich Comment Form below."
  [port]
  (let [db (setup-database my-db)]
    {:db db
     :web-server (web-server (middleware-stack db) port)}))

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
