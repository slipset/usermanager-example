;; copyright (c) 2019-2023 Sean Corfield, all rights reserved

(ns usermanager.model.user-manager
  "The model for the application. This is where the persistence happens,
  although in a larger application, this would probably contain just the
  business logic and the persistence would be in a separate namespace."
  (:require [honey.sql.helpers :as hh]))

;; our database connection and initial data

;; data model access functions

(defn by-id [id]
  [:= :id id])

(defn department-by-id [id]
  (-> (hh/select :*)
      (hh/from :department)
      (hh/where (by-id [id]))))

(def all-departments
  (-> (hh/select :*)
      (hh/from :department)
      (hh/order-by :name)))

(defn user-by-id [id]
  (-> (hh/select :*)
      (hh/from :addressbook)
      (hh/where (by-id id))))

(def all-users
  (-> (hh/select :a.*, :d.name)
      (hh/from [:addressbook :a])
      (hh/join [:department :d] [:= :a.department_id :d.id])
      (hh/order-by :a.last_name :a.first_name)))

(defn delete-by-id [id]
  (-> (hh/delete)
      (hh/from :addressbook)
      (hh/where (by-id id))))

(defn update-user [user]
  (-> (hh/update :addressbook)
      (hh/set (dissoc user :addressbook/id))
      (hh/where (by-id (:addressbook/id user)))))

(defn create-user [user]
  (-> (hh/insert-into :addressbook)
      (hh/values [(dissoc user :addressbook/id)])))
