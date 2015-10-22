(ns net.thegeez.gatherlist.client
  (:require [goog.dom :as gdom]
            [goog.dom.classes :as classes]
            [goog.events :as gevents]
            [goog.dom.forms :as forms]
            [net.thegeez.gatherlist.event-source :as event-source]))

(enable-console-print!)
(println "hello world!")

(def updates-elem (gdom/getElement "updates"))

(def updates (atom 0))

(defn on-event [event]
  (let [count (swap! updates inc)]
    (when (= count 1)
      (classes/remove updates-elem "hidden"))
    (gdom/setTextContent updates-elem
                         (str count " new items available"))))

(defn ^:export startstream [url]
  (event-source/event-source url
                             :on-message (fn [event]
                                           (on-event event))
                             :on-error (fn [e]
                                         (println "EventStream error: " e))))

