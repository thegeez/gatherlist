(ns net.thegeez.gatherlist.migrations
  (:require [io.pedestal.log :as log]
            [clojure.java.jdbc :as jdbc]))

(defn serial-id [db]
  (if (.contains (:connection-uri db) "derby")
    [:id "INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)"]
    [:id :serial "PRIMARY KEY"]))

(def migrations
  [
   (let [table :migration_version]
     [1 {:up (fn [db]
               (jdbc/db-do-commands
                db (jdbc/create-table-ddl
                    table
                    [:id :int]
                    [:version :int]))
               (jdbc/insert! db
                             table {:id 0
                                    :version 1}))
         :down (fn [db]
                 (jdbc/db-do-commands
                  db (jdbc/drop-table-ddl
                      table)))}])
   (let [table :users]
     [2 {:up (fn [db]
               (jdbc/db-do-commands
                db (jdbc/create-table-ddl
                    table
                    (serial-id db)
                    [:name "VARCHAR(256) UNIQUE"]
                    [:password_encrypted "VARCHAR(256)"]
                    [:github_login "VARCHAR(256)"]
                    [:google_id "VARCHAR(256)"]
                    [:facebook_id "VARCHAR(256)"]
                    [:twitter_id "VARCHAR(256)"]
                    [:created_at "BIGINT"]
                    [:updated_at "BIGINT"])))
         ;; todo also add indexes for lookup by oauth id
         :down (fn [db]
                 (jdbc/db-do-commands
                  db (jdbc/drop-table-ddl
                      table)))}])
   (let [table :pages]
     [3 {:up (fn [db]
               (jdbc/db-do-commands
                db (jdbc/create-table-ddl
                    table
                    (serial-id db)
                    [:page_slug "VARCHAR(256) UNIQUE NOT NULL"]
                    [:created_by "BIGINT"]
                    [:created_at "BIGINT"]
                    [:updated_at "BIGINT"])))
         ;; todo index by page_slug
         :down (fn [db]
                 (jdbc/db-do-commands
                  db (jdbc/drop-table-ddl
                      table)))}])
   (let [table :items]
     [4 {:up (fn [db]
               (jdbc/db-do-commands
                db (jdbc/create-table-ddl
                    table
                    (serial-id db)
                    [:page_id "BIGINT"]
                    [:data "VARCHAR(2560)"]
                    [:created_by "BIGINT"]
                    [:created_at "BIGINT"]
                    [:updated_at "BIGINT"])))
         :down (fn [db]
                 (jdbc/db-do-commands
                  db (jdbc/drop-table-ddl
                      table)))}])
   (let [table :snippets]
     [5 {:up (fn [db]
               (jdbc/db-do-commands
                db (jdbc/create-table-ddl
                    table
                    (serial-id db)
                    [:title "VARCHAR(256) UNIQUE NOT NULL"]
                    [:quality_good "BOOLEAN DEFAULT FALSE"]
                    [:quality_fast "BOOLEAN DEFAULT FALSE"]
                    [:quality_cheap "BOOLEAN DEFAULT FALSE"]
                    [:owner "BIGINT"]
                    [:created_at "BIGINT"]
                    [:updated_at "BIGINT"])))
         :down (fn [db]
                 (jdbc/db-do-commands
                  db (jdbc/drop-table-ddl
                      table)))}])])
