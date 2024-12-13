;; copyright (c) 2019-2023 Sean Corfield, all rights reserved

(ns usermanager.controllers.user
  "The main controller for the user management portion of this app."
  (:require [ring.util.response :as resp]
            [selmer.parser :as tmpl]
            [usermanager.model.user-manager :as model]
            [usermanager.db :as db]))

(def ^:private changes
  "Count the number of changes (since the last reload)."
  (atom 0))

(defn ->db [req]
  (-> req :application/db))

(defn render-page
  "Each handler function here adds :application/view to the request
  data to indicate which view file they want displayed. This allows
  us to put the rendering logic in one place instead of repeating it
  for every handler."
  [req]
  (let [data (assoc (:params req) :changes @changes)
        view (:application/view req "default")
        html (tmpl/render-file (str "views/user/" view ".html") data)]
    (-> (resp/response (tmpl/render-file "layouts/default.html"
                                         (assoc data :body [:safe html])))
        (resp/content-type "text/html"))))

(defn reset-changes
  [req]
  (reset! changes 0)
  (assoc-in req [:params :message] "The change tracker has been reset."))

(defn default
  [req]
  (assoc-in req [:params :message]
                (str "Welcome to the User Manager application demo! "
                     "This uses just Compojure, Ring, and Selmer.")))

(defn delete-by-id
  "Compojure has already coerced the :id parameter to an int."
  [req]
  (swap! changes inc)
  (db/delete! (->db req) (model/delete-by-id
                          (get-in req [:params :id])))
  (resp/redirect "/user/list"))

(defn edit
  "Display the add/edit form.

  If the :id parameter is present, Compojure will have coerced it to an
  int and we can use it to populate the edit form by loading that user's
  data from the addressbook."
  [req]
  (let [db   (->db req)
        user (when-let [id (get-in req [:params :id])]
               (db/query-one! db (model/user-by-id id)))
        departments (db/query! db model/all-departments)]
    (-> req
        (update :params assoc
                :user user
                :departments departments)
        (assoc :application/view "form"))))

(defn get-users
  "Render the list view with all the users in the addressbook."
  [req]
  (let [users (db/query! (->db req) model/all-users)]
    (-> req
        (assoc-in [:params :users] users)
        (assoc :application/view "list"))))

(defn save
  "This works for saving new users as well as updating existing users, by
  delegatin to the model, and either passing nil for :addressbook/id or
  the numeric value that was passed to the edit form."
  [req]
  (swap! changes inc)
  (-> req
      :params
      ;; get just the form fields we care about:
      (select-keys [:id :first_name :last_name :email :department_id])
      ;; convert form fields to numeric:
      (update :id            #(some-> % not-empty Long/parseLong))
      (update :department_id #(some-> % not-empty Long/parseLong))
      ;; qualify their names for domain model:
      (->> (reduce-kv (fn [m k v] (assoc! m (db/->kw :addressbook k) v))
                      (transient {}))
           (persistent!)
           (db/save! (->db req) :addressbook)))
  (resp/redirect "/user/list"))
