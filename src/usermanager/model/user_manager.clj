;; copyright (c) 2019-2023 Sean Corfield, all rights reserved

(ns usermanager.model.user-manager
  "The model for the application. This is where the persistence happens,
  although in a larger application, this would probably contain just the
  business logic and the persistence would be in a separate namespace."
  (:require [com.stuartsierra.component :as component]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]))

;; our database connection and initial data

(def ^:private my-db
  "SQLite database connection spec."
  {:dbtype "sqlite" :dbname "usermanager_db"})

(def ^:private departments
  "List of departments."
  ["Accounting" "Sales" "Support" "Development"])

(def ^:private initial-user-data
  "Seed the database with this data."
  [{:first_name "Sean" :last_name "Corfield"
    :email "sean@worldsingles.com" :department_id 4}])

;; database initialization

(defn- populate
  "Called at application startup. Attempts to create the
  database table and populate it. Takes no action if the
  database table already exists."
  [db db-type]
  (let [auto-key (if (= "sqlite" db-type)
                   "primary key autoincrement"
                   (str "generated always as identity"
                        " (start with 1 increment by 1)"
                        " primary key"))]
    (try
      (jdbc/execute-one! (db)
                         [(str "
create table department (
  id            integer " auto-key ",
  name          varchar(32)
)")])
      (jdbc/execute-one! (db)
                         [(str "
create table addressbook (
  id            integer " auto-key ",
  first_name    varchar(32),
  last_name     varchar(32),
  email         varchar(64),
  department_id integer not null
)")])
      (println "Created database and addressbook table!")
      ;; if table creation was successful, it didn't exist before
      ;; so populate it...
      (try
        (doseq [d departments]
          (sql/insert! (db) :department {:name d}))
        (doseq [row initial-user-data]
          (sql/insert! (db) :addressbook row))
        (println "Populated database with initial data!")
        (catch Exception e
          (println "Exception:" (ex-message e))
          (println "Unable to populate the initial data -- proceed with caution!")))
      (catch Exception e
        (println "Exception:" (ex-message e))
        (println "Looks like the database is already setup?")))))

;; database component

(defrecord Database [db-spec     ; configuration
                     datasource] ; state

  component/Lifecycle
  (start [this]
    (if datasource
      this ; already initialized
      (let [this+ds (assoc this :datasource (jdbc/get-datasource db-spec))]
        ;; set up database if necessary
        (populate this+ds (:dbtype db-spec))
        this+ds)))
  (stop [this]
    (assoc this :datasource nil))

  ;; allow the Database component to be "called" with no arguments
  ;; to produce the underlying datasource object
  clojure.lang.IFn
  (invoke [_] datasource))

(defn setup-database [] (map->Database {:db-spec my-db}))

;; data model access functions

(defn department-by-id [id]
  ["select * from departement where id = ?" id])

(def all-departments ["select * from department order by name"])

(defn user-by-id [id]
  ["select * from addressbook where id = ?" id])

(def all-users ["
select a.*, d.name
 from addressbook a
 join department d on a.department_id = d.id
 order by a.last_name, a.first_name
"])
(defn delete-by-id [id]
  ["delete from addressbook where id = ?" id]) 
