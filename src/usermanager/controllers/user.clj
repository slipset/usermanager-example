;; copyright (c) 2019-2023 Sean Corfield, all rights reserved

(ns usermanager.controllers.user
  "The main controller for the user management portion of this app."
  (:require [usermanager.model.user-manager :as model]
            [usermanager.db :as db]))

(def changes
  "Count the number of changes (since the last reload)."
  (atom 0))

(defn reset-changes
  []
  (reset! changes 0)
  {:message "The change tracker has been reset."})

(def default
  {:message (str "Welcome to the User Manager application demo! "
                 "This uses just Compojure, Ring, and Selmer.")})

(defn delete-by-id
  "Compojure has already coerced the :id parameter to an int."
  [db id]
  (swap! changes inc)
  (db/transact! db (model/delete-by-id id)))

(defn edit
  "Display the add/edit form.

  If the :id parameter is present, Compojure will have coerced it to an
  int and we can use it to populate the edit form by loading that user's
  data from the addressbook."
  [db id]
  {:user (some->> id model/user-by-id (db/query-one! db))
   :departments (db/query! db model/all-departments)})

(defn get-users
  "Render the list view with all the users in the addressbook."
  [db]
  {:users (db/query! db model/all-users)})

(defn ->kw [ns kw]
  (keyword (name ns) (name kw)))

(defn- prepare [user]
  (-> user
      ;; get just the form fields we care about:
      (select-keys [:id :first_name :last_name :email :department_id])
      ;; convert form fields to numeric:
      (update :id            #(some-> % not-empty Long/parseLong))
      (update :department_id #(some-> % not-empty Long/parseLong))
      ;; qualify their names for domain model:
      (->> (reduce-kv (fn [m k v] (assoc! m (->kw :addressbook k) v))
                      (transient {}))
           (persistent!))))

(defn save
  "This works for saving new users as well as updating existing users, by
  delegatin to the model, and either passing nil for :addressbook/id or
  the numeric value that was passed to the edit form."
  [db user]
  (swap! changes inc)
  (db/transact! db (if (:id user)
                     (model/update-user (prepare user))
                     (model/create-user (prepare user)))))
