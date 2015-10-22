(ns net.thegeez.gatherlist.core
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.log :as log]
            [io.pedestal.http :as http]
            [net.thegeez.w3a.server :as server]
            [net.thegeez.w3a.system.sql-database :as database]
            [ring.middleware.session.memory]
            [net.thegeez.gatherlist.fixtures :as fixtures]
            [net.thegeez.gatherlist.migrations :as migrations]
            [net.thegeez.gatherlist.service :as service]))

(defn prod-system [config-options]
  (log/info :msg "Hello world, this is the production system!")
  (let [{:keys [db-connect-string port migrations]} config-options]
    (component/system-map
     :session-options {:store (ring.middleware.session.memory/memory-store)}
     :server (component/using
              (server/pedestal-component
               (-> (assoc service/service
                     ::http/port port)
                   http/default-interceptors))
              {:database :db
               :session-options :session-options})
     :jetty (component/using
             (server/jetty-component)
             [:server])
     :db (database/database db-connect-string)
     :db-migrator (component/using
                   (database/dev-migrator migrations)
                   {:database :db})
     :fixtures (component/using
                (fixtures/fixtures)
                {:database :db
                 :db-migrator :db-migrator}))))
