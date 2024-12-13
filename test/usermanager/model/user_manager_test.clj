;; copyright (c) 2019-2023 Sean Corfield, all rights reserved

(ns usermanager.model.user-manager-test
  "These tests use H2 in-memory."
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [com.stuartsierra.component :as component]
            [next.jdbc :as jdbc]
            [usermanager.model.user-manager :as model]
            [usermanager.db :as db]))

(def ^:private test-db (atom nil))

(def ^:private db-spec {:dbtype "h2:mem"
                        :dbname "usermanager_test"
                        :database_to_upper false})

(defn- with-test-db
  "A test fixture that sets up an in-memory H2 database for running tests."
  [t]
  ;; clear out any existing in-memory data
  (let [ds (jdbc/get-datasource db-spec)]
    (try
      (jdbc/execute-one! ds ["drop table department"])
      (jdbc/execute-one! ds ["drop table addressbook"])
      (catch Exception _)))
  (let [db (component/start
            (model/map->Database {:db-spec db-spec}))]
    (reset! test-db db)
    (t)
    (component/stop db)))

(use-fixtures :once with-test-db)

(deftest department-test
  (is (= #:department{:id 1 :name "Accounting"}
         (db/query-one! (@test-db) (model/department-by-id 1))))
  (is (= 4 (count (db/query! (@test-db) model/all-departments)))))

(deftest user-test
  (is (= 1 (:addressbook/id (db/query-one! (@test-db) (model/user-by-id 1)))))
  (is (= "Sean" (:addressbook/first_name
                 (db/query-one! (@test-db) (model/user-by-id 1)))))
  (is (= 4 (:addressbook/department_id
            (db/query-one! (@test-db) (model/user-by-id 1)))))
  (is (= 1 (count (db/query!  (@test-db) model/all-users))))
  (is (= "Development" (:department/name
                        (db/query-one! (@test-db)
                                       model/all-users)))))

(deftest save-test
  (is (= "sean@corfield.org"
         (:addressbook/email
          (do
            (db/save! (@test-db)
                      :addressbook
                      {:addressbook/id 1
                       :addressbook/email "sean@corfield.org"})
            (db/query-one! (@test-db) (model/user-by-id 1))))))
  (is (= "seancorfield@hotmail.com"
         (:addressbook/email
          (do
            (db/save! (@test-db)
                     :addressbook
                     {:addressbook/first_name "Sean"
                      :addressbook/last_name "Corfield"
                      :addressbook/department_id 4
                      :addressbook/email "seancorfield@hotmail.com"})
            (db/query-one! (@test-db) (model/user-by-id 1)))))))
