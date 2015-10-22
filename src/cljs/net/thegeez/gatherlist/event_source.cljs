(ns net.thegeez.gatherlist.event-source
  (:require [cljs.reader :as reader]
            [goog.Timer :as gtimer]))

(defn event-source [url & {:keys [on-open on-message on-error]
                           :or {on-open (fn [])
                                on-message (fn [event])
                                on-error (fn [e])}}]
  (let [source (js/EventSource. url)
        open (atom false)]
    (set! (.-onopen source)
          (fn []
            (reset! open true)
            (on-open)
            nil))
    (set! (.-onerror source)
          (fn [e]
            ;; can't connect is a problem, disconnecting is not
            (when-not @open
              (on-error e))
            (reset! open false)
            (goog.Timer/callOnce (fn []
                                   ;; this sometimes happens in
                                   ;; firefox
                                   (when (= (.-readyState source) (.-CLOSED js/EventSource))
                                     (event-source url :on-open on-open :on-message on-message :on-error on-error)))
                                 (* 9 1000))
            nil))
    (set! (.-onmessage source)
          (fn [e]
            (let [data (.-data e)
                  event (reader/read-string data)]
              (on-message event))
            nil))
    source))
