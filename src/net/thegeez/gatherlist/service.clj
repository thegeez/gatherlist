(ns net.thegeez.gatherlist.service
  (:require [buddy.hashers :as hashers]
            [clojure.java.jdbc :as jdbc]
            [io.pedestal.log :as log]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.ring-middlewares :as middlewares]
            [io.pedestal.http.route.definition :refer [defroutes]]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.impl.interceptor :as impl-interceptor]
            [net.cgrand.enlive-html :as html]
            [net.thegeez.w3a.binding :as binding]
            [net.thegeez.w3a.edn-wrap :as edn-wrap]
            [net.thegeez.w3a.form :as form]
            [net.thegeez.w3a.interceptor :refer [combine] :as w3ainterceptor]
            [net.thegeez.w3a.link :as link]
            [net.thegeez.w3a.pagination :as pagination]
            [net.thegeez.gatherlist.data.users :as users]
            [net.thegeez.gatherlist.data.pages :as pages]
            [net.thegeez.gatherlist.oauth.github :as oauth.github]
            [net.thegeez.gatherlist.oauth.google :as oauth.google]
            [net.thegeez.gatherlist.oauth.facebook :as oauth.facebook]
            [net.thegeez.gatherlist.oauth.twitter :as oauth.twitter]
            [net.thegeez.gatherlist.views.pages :as views.pages]))

(def application-frame (html/html-resource "templates/application.html"))

(defn return-to-or-link [context link-name & opts]
  (let [return-to (get-in context [:request :query-params :return-to])]
    (if (and return-to
             (seq return-to))
      return-to
      (apply link/link context link-name opts))))

(defn login-box-html [context]
  (if-let [auth (get-in context [:response :data :auth])]
    (html/transform-content
     [:a#name] (html/content (:name auth))
     [:a#user] (html/set-attr :href (:url auth))
     [:form#logout] (html/do->
                     (html/prepend
                      (html/html [:input {:type "hidden"
                                          :name "__anti-forgery-token"
                                          :value (get-in context [:request :io.pedestal.http.csrf/anti-forgery-token])}]))
                     (html/set-attr :action (str (:logout auth)
                                                 "?return-to=" (get context :self)))))
    (html/transform-content
     [:li] (html/clone-for [content [[:a {:href (link/link context ::login :query-params {:return-to (link/self context)})}
                                      "Login"]
                                     [:a {:href (link/link context ::signup :query-params {:return-to (link/self context)})}
                                      "Signup"]]]
                           (html/content (html/html content))))))

(defn flash-html [context]
  (if-let [msg (get-in context [:response :data :flash :message])]
    (html/before
     (html/html [:div {:id "flash"
                       :class "alert alert-info"} msg]))
    identity))

(def with-html
  (interceptor/interceptor
   {:leave (fn [context]
             (cond-> context
                     (edn-wrap/for-html? context)
                     (->
                      (assoc-in [:response :headers "Content-Type"] "text/html")
                      (update-in [:response :body]
                                 (fn [body]
                                   (apply str (html/emit*
                                               (html/at application-frame
                                                        [:#login-box]
                                                        (login-box-html context)

                                                        [:#content]
                                                        (flash-html context)

                                                        [:#content] (html/append
                                                                     (map html/html (edn-wrap/forms-edn context)))))))))))}))

(def with-login-html
  (interceptor/interceptor
   {:leave (fn [context]
             (cond-> context
                     (edn-wrap/for-html? context)
                     (->
                      (assoc-in [:response :headers "Content-Type"] "text/html")
                      (update-in [:response :body]
                                 (fn [body]
                                   (apply str (html/emit*
                                               (html/at application-frame
                                                        [:.navbar] nil
                                                        [:#content]
                                                        (flash-html context)

                                                        [:#content] (html/before
                                                                     (html/html [:h1 "Gatherlist login"]))
                                                        [:#content] (html/append (html/html
                                                                                  [:div.panel.panel-default
                                                                                   [:div.panel-body "Login with "
                                                                                    [:a {:href (get-in context [:response :data :links :github])} "GitHub"]]]
                                                                                  [:div.panel.panel-default
                                                                                   [:div.panel-body "Login with "
                                                                                    [:a {:href (get-in context [:response :data :links :google])} "Google"]]]
                                                                                  [:div.panel.panel-default
                                                                                   [:div.panel-body "Login with "
                                                                                    [:a {:href (get-in context [:response :data :links :facebook])} "Facebook"]]]
                                                                                  [:div.panel.panel-default
                                                                                   [:div.panel-body "Login with "
                                                                                    [:a {:href (get-in context [:response :data :links :twitter])} "Twitter"]]]
                                                                                  [:div "Login with amy/amy or bob/bob"]))
                                                        [:#content] (html/append
                                                                     (map html/html (edn-wrap/forms-edn context)))))))))))}))

(def with-signup-html
  (interceptor/interceptor
   {:leave (fn [context]
             (cond-> context
                     (edn-wrap/for-html? context)
                     (->
                      (assoc-in [:response :headers "Content-Type"] "text/html")
                      (update-in [:response :body]
                                 (fn [body]
                                   (apply str (html/emit*
                                               (html/at application-frame
                                                        [:.navbar] nil
                                                        [:#content]
                                                        (flash-html context)

                                                        [:#content] (html/before
                                                                     (html/html [:h1 "Gatherlist signup"]))
                                                        [:#content] (html/append (html/html
                                                                                  [:div.panel.panel-default
                                                                                   [:div.panel-body "Signup with "
                                                                                    [:a {:href (get-in context [:response :data :links :github])} "GitHub"]]]
                                                                                  [:div.panel.panel-default
                                                                                   [:div.panel-body "Signup with "
                                                                                    [:a {:href (get-in context [:response :data :links :google])} "Google"]]]
                                                                                  [:div.panel.panel-default
                                                                                   [:div.panel-body "Signup with "
                                                                                    [:a {:href (get-in context [:response :data :links :facebook])} "Facebook"]]]
                                                                                  [:div.panel.panel-default
                                                                                   [:div.panel-body "Signup with "
                                                                                    [:a {:href (get-in context [:response :data :links :twitter])} "Twitter"]]]))
                                                        [:#content] (html/append
                                                                     (map html/html (edn-wrap/forms-edn context)))))))))))}))


(def name-binding
  {:user
   [{:id :name
     :label "Name"
     :type :string
     :validator (fn [{{{:keys [name]} :params} :request :as context}]
                  (when (not (seq name))
                    {:name ["Name can't be empty"]}
))}]})

(def with-name-html
  (interceptor/interceptor
   {:leave (fn [context]
             (cond-> context
                     (edn-wrap/for-html? context)
                     (->
                      (assoc-in [:response :headers "Content-Type"] "text/html")
                      (update-in [:response :body]
                                 (fn [body]
                                   (apply str (html/emit*
                                               (html/at application-frame
                                                        [:.navbar] nil
                                                        [:#content]
                                                        (flash-html context)

                                                        [:#content] (html/before
                                                                     (html/html [:h1 "Gatherlist account creation"]))
                                                        [:#content] (html/append (html/html [:div "Attach a name to your account"]))
                                                        [:#content] (html/append
                                                                     (map html/html (edn-wrap/forms-edn context)))))))))))}))

(def create-name
  (interceptor/interceptor
   {:leave (fn [context]
             (combine context
                      {:response
                       {:status 200
                        :data {:flash (get-in context [:request :flash])
                               :user {:net.thegeez.w3a.binding/binding name-binding
                                      :name (get-in context [:request :query-params :suggested-name])}}}}))}))

(def create-name-post
  (interceptor/interceptor
   {:leave (fn [context]
             (let [values (get-in context [:request :values])
                   id (get-in context [:request :session :auth-create-name :id])
                   name (:name values)
                   id-or-errors (users/set-name context id name)]
               (if-let [errors (:errors id-or-errors)]
                 (combine context
                          {:response
                           {:status 400
                            :data {:user {:values values
                                          :errors errors}}}})
                 (combine context
                          {:response
                           {:status 303
                            :session {:auth {:id id}
                                      :auth-create-name nil}
                            :headers {"Location" (or (get-in context [:request :query-params :return-to])
                                                     (link/link context ::home))}
                            :flash {:message "Name created"}}}))))}))

(def with-home-html
  (interceptor/interceptor
   {:leave (fn [context]
             (cond-> context
                     (edn-wrap/for-html? context)
                     (->
                      (assoc-in [:response :headers "Content-Type"] "text/html")
                      (update-in [:response :body]
                                 (fn [body]
                                   (apply str
                                          (html/emit*
                                           (html/at application-frame
                                                    [:#login-box] (login-box-html context)
                                                    [:#content] (flash-html context)

                                                    [:#content]
                                                    (let [{:keys [create demo-page]}
                                                          (get-in context [:response :data :links])]
                                                      (html/append
                                                       (html/html
                                                        [:div.panel.panel-default
                                                         [:div.panel-heading
                                                          [:h3.panel-title "Gatherlist"]]
                                                         [:div.panel-body
                                                          [:p "A Clojure app on Heroku with Pedestal, database and OAuth login."]
                                                          [:a {:href create}
                                                           "Create a page"]
                                                          " or "
                                                          [:a {:href demo-page}
                                                           "use the demo page"]]])))))))))))}))

(def home
  (interceptor/interceptor
   {:leave (fn [context]
             (combine context
                      {:response
                       {:status 200
                        :data {:links {:signup (link/link context ::signup)
                                       :login (link/link context ::login)
                                       :create (link/link context ::create)
                                       :demo-page (link/link context ::page :params {:page-slug "demo"})}
                               :flash (get-in context [:request :flash])}}}))}))

(defn user-resource [context data]
  (dissoc data :id))

(defn get-users [db]
  (jdbc/query db ["SELECT id, name, created_at, updated_at FROM users"]))

(defn get-auth [db values]
  (let [{:keys [name password]} values]
    (when-let [user (first (jdbc/query db ["SELECT * FROM users WHERE name = ? " name]))]
      (when (hashers/check password (:password_encrypted user))
        (dissoc user :password_encrypted)))))

(def return-if-logged-in
  (interceptor/interceptor
   {:enter (fn [context]
             (if-let [user (get-in context [:auth])]
               (impl-interceptor/terminate
                (combine context
                         {:response {:status 303
                                     :headers {"Location" (return-to-or-link context ::home)}
                                     :flash {:message "Already logged in"}}}))
               context))}))

(def signup-binding
  {:user
   [{:id :name
     :label "Name"
     :type :string
     :validator (fn [{{{:keys [name]} :params} :request :as context}]
                  (when (not (seq name))
                    {:name ["Name can't be empty"]}
))}
    {:id :password
     :label "Password"
     :type :password
     :validator (fn [{{{:keys [password]} :params} :request :as context}]
                  (when (not (seq password))
                    {:password ["Password can't be empty"]}
                    ))}
    {:id :password-confirm
     :label "Password (repeat)"
     :type :password
     :validator (fn [{{{:keys [password password-confirm]} :params} :request :as context}]
                  (when-not (= password password-confirm)
                    {:password ["Passwords don't match"]
                     :password-confirm ["Passwords don't match"]}))}]})

(def signup
  (interceptor/interceptor
   {:leave (fn [context]
             (combine context
                      {:response
                       {:status 200
                        :data {:flash (get-in context [:request :flash])
                               :links {:github (link/link context ::oauth-github
                                                          :params {:return-to (return-to-or-link context ::home)})
                                       :google (link/link context ::oauth-google
                                                          :params {:return-to (return-to-or-link context ::home)})
                                       :facebook (link/link context ::oauth-facebook
                                                            :params {:return-to (return-to-or-link context ::home)})
                                       :twitter (link/link context ::oauth-twitter
                                                           :params {:return-to (return-to-or-link context ::home)})}}}}))}))

(def signup-post
  (interceptor/interceptor
   {:leave (fn [context]
             (let [values (get-in context [:request :values])
                   user {:name (:name values)
                         :password_encrypted (hashers/encrypt (:password values))}
                   id-or-errors
                   (try
                     (let [res (jdbc/insert! (:database context)
                                             :users
                                             (merge user
                                                    (let [now (.getTime (java.util.Date.))]
                                                      {:created_at now
                                                       :updated_at now})))]
                       (when-not (= 1 (count res))
                         (throw (Exception.)))
                       (:id (first (jdbc/query (:database context) ["SELECT id FROM users WHERE name = ?" (:name user)]))))
                     (catch Exception _
                       ;; assume name unique violation
                       {:errors {:name ["Name already exists"]}}
                       ))]
               (if-let [errors (:errors id-or-errors)]
                 (combine context
                          {:response
                           {:status 400
                            :data {:user {:values values
                                          :errors errors}}}})
                 (let [auth (get-auth (:database context) values)]
                   (combine context
                            {:response
                             {:status 201
                              :headers {"Location" (return-to-or-link context ::home)}
                              :session {:auth {:id (:id auth)}}
                              :flash {:message "User created"}}})))))}))

(def login-binding
  {:user
   [{:id :name
     :label "Name"
     :type :string
     :validator (fn [{{{:keys [name]} :params} :request :as context}]
                  (when (not (seq name))
                    {:name ["Name can't be empty"]}))}
    {:id :password
     :label "Password"
     :type :password
     :validator (fn [{{{:keys [password]} :params} :request :as context}]
                  (when (not (seq password))
                    {:password ["Password can't be empty"]}
                    ))}]})

(def login
  (interceptor/interceptor
   {:leave (fn [context]
             (combine context
                      {:response
                       {:status 200
                        :data {:flash (get-in context [:request :flash])
                               :links {:github (link/link context ::oauth-github
                                                          :params {:return-to (return-to-or-link context ::home)})
                                       :google (link/link context ::oauth-google
                                                          :params {:return-to (return-to-or-link context ::home)})
                                       :facebook (link/link context ::oauth-facebook
                                                            :params {:return-to (return-to-or-link context ::home)})
                                       :twitter (link/link context ::oauth-twitter
                                                           :params {:return-to (return-to-or-link context ::home)})}}}}))}))

(def login-post
  (interceptor/interceptor
   {:leave (fn [context]
             (let [values (get-in context [:request :values])]
               (if-let [auth (get-auth (:database context) values)]
                 (combine context
                          {:response
                           {:status 303
                            :headers {"Location"
                                      (return-to-or-link context ::home)}
                            :session {:auth {:id (:id auth)}}
                            :flash {:message "Login successful"}}})
                 (combine context
                          {:response
                           {:status 303
                            :headers {"Location" (get-in context [:self])}
                            :flash {:message "Login failed"}}}))))}))

(def logout-post
  (interceptor/interceptor
   {:leave (fn [context]
             (combine context
                      {:response
                       {:status 303
                        :headers {"Location" (or (get-in context [:request :query-params :return-to])
                                                 (link/link context ::home))}
                        :session {}
                        :flash {:message "Logout successful"}}}))}))

(defn get-user [db id]
  (first (jdbc/query db ["SELECT id, name, created_at, updated_at FROM users WHERE id = ?" id])))

(def with-user-auth
  (interceptor/interceptor
   {:enter (fn [context]
             (if-let [user (when-let [id (get-in context [:request :session :auth :id])]
                             (get-user (:database context) id))]
               (combine context {:auth user})
               context))}))

(defn auth-resource [context]
  (let [{:keys [id] :as auth} (:auth context)]
    (assoc auth
      :logout (link/link context ::logout-post))))

(def with-login-data
  (interceptor/interceptor
   {:enter (fn [context]
             (cond-> context
                     (:auth context)
                     (combine
                      (let [user (auth-resource context)]
                        {:response {:data {:auth user}}}))))}))

(def with-user
  (interceptor/interceptor
   {:enter (fn [context]
             (let [id (get-in context [:request :path-params :id])]
               (if-let [user (get-user (:database context) id)]
                 (assoc context :user user)
                 (impl-interceptor/terminate
                  (combine context
                           {:response
                            {:status 404
                             :body "not found"}})))))}))

(def user
  (interceptor/interceptor
   {:leave (fn [context]
             (combine context
                      {:response
                       {:status 200
                        :data {:flash (get-in context [:request :flash])
                               :user (user-resource context (get-in context [:user]))}}}))}))

(def require-auth
  (interceptor/interceptor
   {:enter (fn [context]
             (if (:auth context)
               context
               (impl-interceptor/terminate
                (combine context
                         {:response {:status 303
                                     :headers {"Location" (link/link context ::login
                                                                     :params {:return-to (get-in context [:self])})}
                                     :flash {:message "Authentication required"}}}))))}))

(def with-page
  (interceptor/interceptor
   {:enter (fn [context]
             (let [page-slug (get-in context [:request :path-params :page-slug])]
               (if-let [page (pages/get-page context page-slug)]
                 (combine context {:page page})
                 (impl-interceptor/terminate
                  (combine context
                           {:response {:status 404
                                       :body "not found"}})))))}))

(def title-binding
  {:page
   [{:id :title
     :label "Title"
     :type :string
     :validator (fn [{{{:keys [title]} :params} :request :as context}]
                  (when (not (seq title))
                    {:title ["Title can't be empty"]}))}]})

(def page-title-post
  (interceptor/interceptor
   {:leave (fn [context]
             (let [page-slug (get-in context [:page :page-slug])
                   values (get-in context [:request :values])
                   res (pages/set-title context)]
               (if-let [errors (:errors res)]
                 (combine context
                          {:response
                           {:status 400
                            :data {:page {:values values
                                          :errors errors}}}})
                 (combine context
                          {:response
                           {:status 201
                            :headers {"Location" (link/link context ::page :params {:page-slug page-slug})}
                            :flash {:message "Title updated"}}}))))}))

(def add-item-binding
  {:page
   [{:id :text
     :label "Text"
     :type :string
     :validator (fn [{{{:keys [text]} :params} :request :as context}]
                  (when (not (seq text))
                    {:text ["Text can't be empty"]}))}]})

(def page-add-item-post
  (interceptor/interceptor
   {:leave (fn [context]
             (let [page-slug (get-in context [:page :page-slug])
                   values (get-in context [:request :values])
                   res (pages/add-item context)]
               (if-let [errors (:errors res)]
                 (combine context
                          {:response
                           {:status 400
                            :data {:page {:values values
                                          :errors errors}}}})
                 (combine context
                          {:response
                           {:status 201
                            :headers {"Location" (link/link context ::page :params {:page-slug page-slug})}
                            :flash {:message "Added item"}}}))))}))

(def page
  (interceptor/interceptor
   {:enter (fn [context]
             (combine context
                      {:response {:status 200
                                  :data {:flash (get-in context [:request :flash])
                                         :self (get-in context [:self])
                                         :page (get-in context [:page])
                                         :links (cond-> {:self (link/link context ::page :params {:page-slug (get-in context [:page :page-slug])})}
                                                        (get-in context [:auth])
                                                        (merge {:edit-title (link/link context ::edit-title :params {:page-slug (get-in context [:page :page-slug])})
                                                                :add-item (link/link context ::add-item :params {:page-slug (get-in context [:page :page-slug])})}))}}}))}))

(def with-page-html
  (interceptor/interceptor
   {:leave (fn [context]
             (cond-> context
                     (edn-wrap/for-html? context)
                     (->
                      (assoc-in [:response :headers "Content-Type"] "text/html")
                      (update-in [:response :body]
                                 (fn [body]
                                   (apply str
                                          (html/emit*
                                           (html/at application-frame
                                                    [:#login-box] (login-box-html context)
                                                    [:#content] (flash-html context)

                                                    [:#content] (html/append
                                                                 (map html/html (edn-wrap/forms-edn context)))
                                                    [:#content] (html/append
                                                                 (views.pages/page (get-in context [:response :data :page])))
                                                    [:#content]
                                                    (if (get-in context [:auth])
                                                      (let [{:keys [edit-title add-item]} (get-in context [:response :data :links])]
                                                        (html/append
                                                         (html/html
                                                          [:div.panel.panel-default
                                                           [:div.panel-body
                                                            [:a {:href edit-title}
                                                             "Edit the title"]
                                                            " or "
                                                            [:a {:href add-item}
                                                             " add text to this page"]]])))
                                                      (html/append
                                                       (html/html
                                                        [:div.panel.panel-default
                                                         [:div.panel-body
                                                          [:a {:href (link/link context ::login :query-params {:return-to (link/self context)})}
                                                           "Login"]
                                                          " or "
                                                          [:a {:href (link/link context ::signup :query-params {:return-to (link/self context)})}
                                                           "Signup"]
                                                          " to add to this page"]])))))))))))}))

(def create-binding
  {:page
   [{:id :slug
     :label "Slug"
     :type :string
     :validator (fn [{{{:keys [slug]} :params} :request :as context}]
                  (when (not (seq slug))
                    {:slug ["Slug can't be empty"]}))}]})

(def create
  (interceptor/interceptor
   {:enter (fn [context]
             (combine context
                      {:response {:status 200
                                  :data {:self (get-in context [:self])
                                         :page {:slug (apply str (repeatedly 6 #(rand-nth "abcdefghijklmnopqrstuvwxyz0123456789")))}}}}))}))

(def create-post
  (interceptor/interceptor
   {:leave (fn [context]
             (let [values (get-in context [:request :values])
                   slug-or-errors (pages/create-page context)]
               (if-let [errors (:errors slug-or-errors)]
                 (combine context
                          {:response
                           {:status 400
                            :data {:page {:values values
                                          :errors errors}}}})
                 (combine context
                          {:response
                           {:status 201
                            :headers {"Location" (link/link context ::page :params {:page-slug slug-or-errors})}
                            :flash {:message "Page created"}}}))))}))

;; (io.pedestal.http.route/print-routes routes)
(defroutes
  routes
  [[["/"
     ^:interceptors [edn-wrap/wrap-edn
                     with-html
                     with-user-auth
                     with-login-data]
     {:get [::home
            ^:interceptors [with-home-html]
            home]}
     ["/create"
      ^:interceptors [require-auth
                      (binding/with-binding create-binding)]
      {:get [::create create]
       :post [::create-post create-post]}]
     ["/gl/:page-slug"
      ^:interceptors [with-page]
      {:get
       [::page
        ^:interceptors [with-page-html]
        page]}
      ["/edit"
       ^:interceptors [require-auth]
       ["/title"
        ^:interceptors [(binding/with-binding title-binding)]
        {:get [::edit-title page]
         :post [::edit-title#post page-title-post]}]
       ["/add-item"
        ^:interceptors [(binding/with-binding add-item-binding)]
        {:get [::add-item page]
         :post [::add-item#post page-add-item-post]}]]]
     ["/oauth"
      ^:interceptors [return-if-logged-in]
      ["/github"
       ["/authenticate" {:get [::oauth-github oauth.github/authenticate]}]
       ["/callback" {:get oauth.github/callback}]]

      ["/google"
       ["/authenticate" {:get [::oauth-google oauth.google/authenticate]}]
       ["/callback" {:get oauth.google/callback}]]

      ["/facebook"
       ["/authenticate" {:get [::oauth-facebook oauth.facebook/authenticate]}]
       ["/callback" {:get oauth.facebook/callback}]]

      ["/twitter"
       ["/authenticate" {:get [::oauth-twitter oauth.twitter/authenticate]}]
       ["/callback" {:get oauth.twitter/callback}]]
      ["/create-name"
       ^:interceptors [with-name-html
                       (binding/with-binding name-binding)]
       {:get [:oauth-create-name create-name]
        :post create-name-post}]]


     ["/signup"
      ^:interceptors [return-if-logged-in
                      with-signup-html
                      (binding/with-binding signup-binding)]
      {:get signup
       :post signup-post}]
     ["/login"
      ^:interceptors [return-if-logged-in
                      with-login-html
                      (binding/with-binding login-binding)]
      {:get login
       :post login-post}]
     ["/logout" {:post logout-post}]]]])

(def bootstrap-webjars-resource-path "META-INF/resources/webjars/bootstrap/3.3.4")
(def jquery-webjars-resource-path "META-INF/resources/webjars/jquery/1.11.1")

(def service
  {:env :prod
   ::http/routes routes

   ::http/resource-path "/public"

   ::http/default-interceptors [(middlewares/resource bootstrap-webjars-resource-path)
                                (middlewares/resource jquery-webjars-resource-path)]

   ::http/type :jetty
   })
