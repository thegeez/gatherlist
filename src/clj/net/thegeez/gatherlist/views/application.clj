(ns net.thegeez.gatherlist.views.application
  (:require [net.cgrand.enlive-html :as html]
            [net.thegeez.w3a.helpers :as helpers]
            [net.thegeez.w3a.html :as w3a-html]
            [net.thegeez.w3a.edn-wrap :as edn-wrap]
            [net.thegeez.w3a.link :as link]))

(def application-frame (html/html-resource "templates/application.html"))

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
                     (html/set-attr :action
                                    (link/link context :logout :query-params {:return-to (link/self context)}))))
    (html/transform-content
     [:li] (html/clone-for [content [[:a {:href (link/link context :login :query-params {:return-to (link/self context)})}
                                      "Login"]
                                     [:a {:href (link/link context :signup :query-params {:return-to (link/self context)})}
                                      "Signup"]]]
                           (html/content (html/html content))))))

(def with-html
  (helpers/for-html-interceptor
   (fn [context]
     (w3a-html/html-string
      application-frame
      [:#login-box]
      (login-box-html context)

      [:#content] (html/append
                   (map html/html (edn-wrap/forms-edn context)))))))

(def with-login-html
  (helpers/for-html-interceptor
   (fn [context]
     (w3a-html/html-string
      application-frame
      [:.navbar] nil

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
                   (map html/html (edn-wrap/forms-edn context)))))))

(def with-signup-html
  (helpers/for-html-interceptor
   (fn [context]
     (w3a-html/html-string application-frame
                           [:.navbar] nil

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
                                        (map html/html (edn-wrap/forms-edn context)))))))

(def with-name-html
  (helpers/for-html-interceptor
   (fn [context]
     (w3a-html/html-string
      application-frame
      [:.navbar] nil

      [:#content] (html/before
                   (html/html [:h1 "Gatherlist account creation"]))
      [:#content] (html/append (html/html [:div "Attach a name to your account"]))
      [:#content] (html/append
                   (map html/html (edn-wrap/forms-edn context)))))))

(def with-home-html
  (helpers/for-html-interceptor
   (fn [context]
     (w3a-html/html-string
      application-frame
      [:#login-box]
      (login-box-html context)

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
             "use the demo page"]]])))))))
