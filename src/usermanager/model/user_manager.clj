;; copyright (c) 2019-2023 Sean Corfield, all rights reserved

(ns usermanager.model.user-manager
  "The model for the application. This is where the persistence happens,
  although in a larger application, this would probably contain just the
  business logic and the persistence would be in a separate namespace.")

;; our database connection and initial data

;; data model access functions

(defn department-by-id [id]
  ["select * from department where id = ?" id])

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
