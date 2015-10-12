(ns net.thegeez.gatherlist.data.users
  (:require [clojure.java.jdbc :as jdbc]
            [io.pedestal.log :as log]))

(defn find-or-create-by-github-login [context github-login]
  (let [db (:database context)]
    (if-let [user (first (jdbc/query db ["SELECT * FROM users WHERE github_login = ? " github-login]))]
      (select-keys user [:id :name :created_at :updated_at])
      ;; create
      (let [res (jdbc/insert! db
                              :users
                              (let [now (.getTime (java.util.Date.))]
                                {:github_login github-login
                                 :created_at now
                                 :updated_at now}))]
        (when-not (= 1 (count res))
          (throw (Exception.)))
        (recur context github-login)))))

(defn find-or-create-by-google-id [context google-id]
  (let [db (:database context)]
    (if-let [user (first (jdbc/query db ["SELECT * FROM users WHERE google_id = ? " google-id]))]
      (select-keys user [:id :name :created_at :updated_at])
      ;; create
      (let [res (jdbc/insert! db
                              :users
                              (let [now (.getTime (java.util.Date.))]
                                {:google_id google-id
                                 :created_at now
                                 :updated_at now}))]
        (when-not (= 1 (count res))
          (throw (Exception.)))
        (recur context google-id)))))

(defn find-or-create-by-facebook-id [context facebook-id]
  (let [db (:database context)]
    (if-let [user (first (jdbc/query db ["SELECT * FROM users WHERE facebook_id = ? " facebook-id]))]
      (select-keys user [:id :name :created_at :updated_at])
      ;; create
      (let [res (jdbc/insert! db
                              :users
                              (let [now (.getTime (java.util.Date.))]
                                {:facebook_id facebook-id
                                 :created_at now
                                 :updated_at now}))]
        (when-not (= 1 (count res))
          (throw (Exception.)))
        (recur context facebook-id)))))

(defn find-or-create-by-twitter-id [context twitter-id]
  (let [db (:database context)]
    (if-let [user (first (jdbc/query db ["SELECT * FROM users WHERE twitter_id = ? " twitter-id]))]
      (select-keys user [:id :name :created_at :updated_at])
      ;; create
      (let [res (jdbc/insert! db
                              :users
                              (let [now (.getTime (java.util.Date.))]
                                {:twitter_id twitter-id
                                 :created_at now
                                 :updated_at now}))]
        (when-not (= 1 (count res))
          (throw (Exception.)))
        (recur context twitter-id)))))

(defn set-name [context id name]
  (try
    (let [res (jdbc/update! (:database context)
                            :users
                            {:name name
                             :updated_at (.getTime (java.util.Date.))}
                            ["id = ?" id])]
      (println "RES!" res)
      (log/info :res res)
      (when-not (= 1 (count res))
        (throw (Exception.)))
      id)
    (catch Exception _
      ;; assume name unique violation
      (log/info :create-name-e _)
      {:errors {:name ["Name already exists"]}})))
