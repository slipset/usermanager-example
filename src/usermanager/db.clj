(ns usermanager.db
  (:require [next.jdbc :as jdbc]
            [honey.sql :as honey]))

(defn ->sql [honey]
  (honey/format honey))

(defn query! [db q]
  (jdbc/execute! db (->sql q)))

(defn query-one! [db q]
    (jdbc/execute-one! db (->sql q)))
(defn transact! [db q]
  (jdbc/execute! db (->sql q)))
