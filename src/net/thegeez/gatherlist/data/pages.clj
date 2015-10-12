(ns net.thegeez.gatherlist.data.pages
  (:require [clojure.edn :as edn]
            [clojure.java.jdbc :as jdbc]
            [io.pedestal.log :as log]))

(defn parse-item [item]
  (log/info :parse-item item)
  (update-in item [:data]
             edn/read-string))

(defmulti to-stream (fn [stream item]
                      (log/info :to-stream item)
                      (get-in item [:data :type])))

(defmethod to-stream :title [stream item]
  (let [new-title (get-in item [:data :title])
        old-title (:title stream)
        item (cond-> {:type :title
                      :to new-title
                      :by (:name item)
                      :time (:created_at item)}
                     old-title
                     (assoc :from old-title))]
    (-> stream
        (assoc :title new-title)
        (update-in [:stream] (fnil conj []) item))))

(defmethod to-stream :item [stream item]
  (let [item {:type :item
              :text (:text (:data item))
              :by (:name item)
              :time (:created_at item)}]
    (-> stream
        (update-in [:stream] (fnil conj []) item))))

(defmethod to-stream :default [stream item]
  (log/info :to-stream-default item)
  stream)

(defn get-page [context page-slug]
  (let [db (:database context)]
    (when-let [page (first (jdbc/query db ["SELECT pages.*, users.name FROM pages JOIN users ON created_by = users.id WHERE page_slug = ? " page-slug]))]
      (let [page-id (:id page)
            items (jdbc/query db ["SELECT items.*, users.name FROM items JOIN users ON created_by = users.id WHERE page_id = ? " page-id])
            stream (reduce to-stream {} (map parse-item items))]
        (-> (select-keys page [:id :created_at :updated_at :name])
            (assoc
                :page-slug (:page_slug page)
                :items items
                :stream stream
                :title (:title stream)))))))

(defn set-title [context]
  (let [db (:database context)
        page-id (get-in context [:page :id])
        values (get-in context [:request :values])
        auth-id (get-in context [:auth :id])
        now (.getTime (java.util.Date.))
        res (try
              (jdbc/insert! db
                            :items
                            {:page_id page-id
                             :data (pr-str {:type :title
                                            :title (:title values)})
                             :created_by auth-id
                             :created_at now
                             :updated_at now})
              nil
              (catch Exception e
                (log/error :sql-exception e)
                {:errors {:title ["Can't set title"]}}
                ))]
    res))

(defn add-item [context]
  (let [db (:database context)
        page-id (get-in context [:page :id])
        values (get-in context [:request :values])
        auth-id (get-in context [:auth :id])
        now (.getTime (java.util.Date.))
        res (try
              (jdbc/insert! db
                            :items
                            {:page_id page-id
                             :data (pr-str {:type :item
                                            :text (:text values)})
                             :created_by auth-id
                             :created_at now
                             :updated_at now})
              nil
              (catch Exception e
                (log/error :sql-exception e)
                {:errors {:title ["Can't set title"]}}
                ))]
    res))

(defn create-page [context]
  (let [db (:database context)
        values (get-in context [:request :values])
        page-slug (:slug values)
        auth-id (get-in context [:auth :id])
        now (.getTime (java.util.Date.))]
    (try
      (let [res (jdbc/insert! db
                              :pages
                              {:page_slug page-slug
                               :created_by auth-id
                               :created_at now
                               :updated_at now})]
        (when-not (= 1 (count res))
          (throw (Exception.)))
        page-slug)
      (catch Exception e
        ;; assume title unique violation
        {:errors {:slug ["Page already exists"]}}
        ))))
