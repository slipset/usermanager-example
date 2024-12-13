(ns usermanager.db
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [honey.sql :as honey]))

(defn ->sql [honey]
  (honey/format honey))

(defn query! [db q]
  (jdbc/execute! db (->sql q)))

(defn query-one! [db q]
    (jdbc/execute-one! db (->sql q)))

(defn delete! [db q]
  (jdbc/execute! db (->sql q)))

(defn ->kw [ns kw]
  (keyword (name ns) (name kw)))

(defn save! [db table-kw data]
  (let [id-kw (->kw table-kw "id")
        id (id-kw data)]
    (if (and id (not (zero? id)))
      ;; update
      (sql/update! db table-kw
                   (dissoc data id-kw)
                   {:id id})
      ;; insert
      (sql/insert! db table-kw
                   (dissoc data id-kw)))))
