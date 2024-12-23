(ns usermanager.hiccup
  (:require [hiccup2.core :as h]))

(defn default [{:keys [message]}]
  [:div {:id "primary"}
   [:p message]])

(defn options [departments selected]
  (map (fn [{:keys [department/id department/name]}]
         [:option {:value id :selected (= id selected)} name])
       departments))

(defn form [{:keys [user departments]}]
  [:div {:id "primary"}
   [:h3 "User Info"]
   [:form
    {:class "familiar medium", :method "post", :action "/user/save"}
    [:input
     {:type "hidden",
      :name "id",
      :id "id",
      :value (:addressbook/id user)}]
    [:div
     {:class "field"}
     [:label {:for "first_name", :class "label"} "First Name:"]
     [:input
      {:type "text",
       :name "first_name",
       :id "first_name",
       :value (:addressbook/first_name user)}]]
    [:div
     {:class "field"}
     [:label {:for "last_name", :class "label"} "Last Name:"]
     [:input
      {:type "text",
       :name "last_name",
       :id "last_name",
       :value (:addressbook/last_name user)}]]
    [:div
     {:class "field"}
     [:label {:for "email", :class "label"} "Email:"]
     [:input
      {:type "text",
       :name "email",
       :id "email",
       :value (:addressbook/email user)}]]
    [:div
     {:class "field"}
     [:label {:for "department_id", :class "label"} "Department:"]
     [:select {:name "department_id", :id "department_id"}
      (options departments (:addressbook/department_id user))]]
    [:div
     {:class "control"}
     [:input {:type "submit", :value "Save User"}]]]])

(defn render-user [user]
  [:tr
   [:td
    [:a
     {:href (str "/user/form/" (:addressbook/id user))}
     (:addressbook/id user)]]
   [:td
    {:class "name"}
    [:a
     {:href (str "/user/form/" (:addressbook/id user))}
     (:addressbook/last_name user) "," [:br] (:addressbook/first_name user)]]
   [:td {:class "email"} (:addressbook/email user)]
   [:td {:class "department"} (:department/name user)]
   [:td
    {:class "delete"}
    [:a {:href (str "/user/delete/" (:addressbook/id user))} "DELETE"]]])

(defn list-users [{:keys [users]}]
  [:div {:id "primary"}
   [:table
    {:border "0", :cellspacing "0"}
    [:colgroup [:col {:width "40"}]]
    [:thead
     [:tr
      [:th "Id"]
      [:th "Name"]
      [:th "Email"]
      [:th "Department"]
      [:th "Delete"]]]
    [:tbody
     (if-not (seq users)
       [:tr
        [:td
         {:colspan "5"}
         "No users exist but"
         [:a {:href "/user/form"} "new ones can be added"]
         "."]]
       (map render-user users))]]])

(defn layout [{:keys [changes] :as data} body]
  [:html
   [:head [:title "User Manager"]]
   [:link
    {:rel "stylesheet", :type "text/css", :href "/assets/css/styles.css"}]
   [:body
    [:div
     {:id "container"}
     [:h1 "User Manager"]
     [:ul
      {:class "nav horizontal clear"}
      [:li [:a {:href "/"} "Home"]]
      [:li
       [:a {:href "/user/list", :title "View the list of users"} "Users"]]
      [:li
       [:a
        {:href "/user/form", :title "Fill out form to add new user"}
        "Add User"]]
      [:li [:a {:href "/reset", :title "Resets change tracking"} "Reset"]]]
     [:br] body]
    [:div
     {:class "font: smaller;"}
     (str "You have made " changes " change(s) since the last reset!")]]])

(defn render-view [view data]
  (case view
    "default" (default data)
    "form" (form data)
    "list" (list-users data)))

(defn html
  [data view]
  (->> data
       (render-view view)
       (layout data)
       h/html
       str))

