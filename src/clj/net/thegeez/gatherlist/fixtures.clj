(ns net.thegeez.gatherlist.fixtures
  (:require [buddy.hashers :as hashers]
            [com.stuartsierra.component :as component]
            [io.pedestal.log :as log]
            [clojure.java.jdbc :as jdbc]))

(defrecord Fixtures [database]
  component/Lifecycle
  (start [component]
    (log/info :msg "Starting fixture loader")
    (when-not (:loaded-fixtures component)
      (try
        (let [db (:spec database)
              now (.getTime (java.util.Date.))]
          (jdbc/insert! db :users
                        {:name "amy"
                         :password_encrypted (hashers/encrypt "amy")
                         :created_at now
                         :updated_at now}
                        {:name "bob"
                         :password_encrypted (hashers/encrypt "bob")
                         :created_at now
                         :updated_at now})
          (let [[amy-id bob-id] (map #(:id (first (jdbc/query db ["select id from users where name = ?" %]))) ["amy" "bob"])
                page-slug "demo"]
            (jdbc/insert! db :pages
             {:page_slug page-slug
              :created_by amy-id
              :created_at now
              :updated_at now})
            (let [page-id (:id (first (jdbc/query db ["select id from pages where page_slug = ?" page-slug])))]
              (jdbc/insert! db :items
                            {:page_id page-id
                             :data "{:type :title :title \"My first title\"}"
                             :created_by amy-id
                             :created_at now
                             :updated_at now}
                            {:page_id page-id
                             :data "{:type :title :title \"My second title\"}"
                             :created_by amy-id
                             :created_at now
                             :updated_at now}
                            {:page_id page-id
                             :data "{:type :title :title \"My third title\"}"
                             :created_by amy-id
                             :created_at now
                             :updated_at now}))))
        (catch Exception e
          (log/info :loading-fixtures-failed (.getMessage e)))))
    (assoc component :loaded-fixtures true))

  (stop [component]
    (log/info :msg "Stopping fixture loader")
    component))

(defn fixtures []
  (map->Fixtures {}))
